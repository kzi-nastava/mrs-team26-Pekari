package com.example.blackcar.presentation.profile.views;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.blackcar.R;
import com.example.blackcar.databinding.FragmentProfileBinding;
import com.example.blackcar.presentation.ViewModelFactory;
import com.example.blackcar.presentation.profile.model.ApprovalRequestUIModel;
import com.example.blackcar.presentation.profile.model.DriverInfoUIModel;
import com.example.blackcar.presentation.profile.model.ProfileUIModel;
import com.example.blackcar.presentation.profile.model.VehicleUIModel;
import com.example.blackcar.presentation.profile.viewmodel.ProfileViewModel;

import java.util.List;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewModelFactory factory = new ViewModelFactory(requireContext());
        viewModel = new ViewModelProvider(this, factory).get(ProfileViewModel.class);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        String localPath = copyImageToInternalStorage(uri);
                        if (localPath != null) {
                            viewModel.updateLocalProfilePicture(localPath);
                        } else {
                            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        setupClickListeners();
        observeState();
    }

    private void setupClickListeners() {
        binding.btnEdit.setOnClickListener(v -> viewModel.setEditing(true));

        binding.btnCancel.setOnClickListener(v -> {
            viewModel.setEditing(false);
            viewModel.load();
        });

        binding.btnSave.setOnClickListener(v -> {
            ProfileUIModel current = viewModel.getState().getValue() != null
                    ? viewModel.getState().getValue().profile
                    : null;

            if (current == null) {
                Toast.makeText(requireContext(), "Profile not loaded", Toast.LENGTH_SHORT).show();
                return;
            }

            ProfileUIModel proposed = current.copyWith(
                    binding.edtFirstName.getText().toString().trim(),
                    binding.edtLastName.getText().toString().trim(),
                    binding.edtPhone.getText().toString().trim(),
                    binding.edtAddress.getText().toString().trim(),
                    current.profilePictureUri
            );

            viewModel.submitChanges(proposed);
        });

        binding.btnChangePassword.setOnClickListener(v -> openChangePasswordDialog());

        binding.btnChangePicture.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.btnLogout.setOnClickListener(v -> viewModel.logout());
    }

    private void observeState() {
        viewModel.getLogoutSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Navigation.findNavController(binding.getRoot())
                        .navigate(R.id.action_profile_to_login);
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                viewModel.clearToastMessage();
            }
        });

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }

            if (state.error && state.errorMessage != null && !state.errorMessage.trim().isEmpty()) {
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show();
            }

            if (state.bannerMessage != null && !state.bannerMessage.trim().isEmpty()) {
                binding.txtBanner.setVisibility(View.VISIBLE);
                binding.txtBanner.setText(state.bannerMessage);
            } else {
                binding.txtBanner.setVisibility(View.GONE);
            }

            renderProfile(state.profile);
            renderDriverInfo(state.profile, state.driverInfo);
            renderApprovals(state.profile, state.approvalRequests);

            setEditingUi(state.isEditing, state.profile);
        });
    }

    private void renderProfile(ProfileUIModel profile) {
        if (profile == null) {
            return;
        }

        binding.edtFirstName.setText(profile.firstName);
        binding.edtLastName.setText(profile.lastName);
        binding.edtPhone.setText(profile.phoneNumber);
        binding.edtEmail.setText(profile.email);
        binding.edtAddress.setText(profile.address);

        setProfileImage(profile.profilePictureUri);
    }

    private void renderDriverInfo(ProfileUIModel profile, DriverInfoUIModel info) {
        if (profile == null) {
            binding.cardDriverInfo.setVisibility(View.GONE);
            return;
        }

        if (!"driver".equalsIgnoreCase(profile.role)) {
            binding.cardDriverInfo.setVisibility(View.GONE);
            return;
        }

        binding.cardDriverInfo.setVisibility(View.VISIBLE);

        if (info == null) {
            binding.txtHoursActive.setText("-");
            binding.txtVehicleMake.setText("-");
            binding.txtVehicleModel.setText("-");
            binding.txtVehicleYear.setText("-");
            binding.txtVehicleLicense.setText("-");
            binding.txtVehicleVin.setText("-");
            return;
        }

        binding.txtHoursActive.setText(formatHours(info.hoursActiveLast24h));

        VehicleUIModel v = info.vehicle;
        if (v != null) {
            binding.txtVehicleMake.setText(getString(R.string.profile_vehicle_make) + ": " + safeValue(v.make));
            binding.txtVehicleModel.setText(getString(R.string.profile_vehicle_model) + ": " + safeValue(v.model));
            binding.txtVehicleYear.setText(getString(R.string.profile_vehicle_year) + ": " + (v.year > 0 ? v.year : "-"));
            binding.txtVehicleLicense.setText(getString(R.string.profile_vehicle_license_plate) + ": " + safeValue(v.licensePlate));
            binding.txtVehicleVin.setText(getString(R.string.profile_vehicle_vin) + ": " + safeValue(v.vin));
        }
    }

    private void renderApprovals(ProfileUIModel profile, List<ApprovalRequestUIModel> requests) {
        if (profile == null || !"admin".equalsIgnoreCase(profile.role)) {
            binding.cardApprovals.setVisibility(View.GONE);
            return;
        }

        binding.cardApprovals.setVisibility(View.VISIBLE);

        List<ApprovalRequestUIModel> pending = viewModel.getPendingApprovalRequestsOnly();
        if (pending.isEmpty()) {
            binding.approvalsEmpty.setVisibility(View.VISIBLE);
            binding.approvalsList.setVisibility(View.GONE);
            binding.approvalsList.removeAllViews();
            return;
        }

        binding.approvalsEmpty.setVisibility(View.GONE);
        binding.approvalsList.setVisibility(View.VISIBLE);
        binding.approvalsList.removeAllViews();

        for (ApprovalRequestUIModel r : pending) {
            binding.approvalsList.addView(createApprovalRow(r));
        }
    }

    private View createApprovalRow(ApprovalRequestUIModel request) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundColor(getResources().getColor(R.color.bg_primary, null));
        row.setPadding(12, 12, 12, 12);

        // Title
        android.widget.TextView title = new android.widget.TextView(requireContext());
        title.setTextColor(getResources().getColor(R.color.text_primary, null));
        title.setTextSize(14);
        title.setText("Request from: " + request.userEmail);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);

        // Summary
        android.widget.TextView summary = new android.widget.TextView(requireContext());
        summary.setTextColor(getResources().getColor(R.color.text_secondary, null));
        summary.setTextSize(13);
        summary.setText("Name: " + request.changes.firstName + " " + request.changes.lastName);

        // Buttons
        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, 12, 0, 0);

        android.widget.Button approve = new android.widget.Button(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        approve.setText(getString(R.string.profile_admin_approve));
        approve.setOnClickListener(v -> {
            boolean ok = viewModel.approveRequest(request.id);
            Toast.makeText(requireContext(), ok ? "Approved" : "Failed", Toast.LENGTH_SHORT).show();
        });

        android.widget.Button reject = new android.widget.Button(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        reject.setText(getString(R.string.profile_admin_reject));
        reject.setOnClickListener(v -> openRejectDialog(request.id));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMarginEnd(12);
        approve.setLayoutParams(lp);
        reject.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        actions.addView(approve);
        actions.addView(reject);

        row.addView(title);
        row.addView(summary);
        row.addView(actions);

        // spacer
        View spacer = new View(requireContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 12));
        row.addView(spacer);

        return row;
    }

    private void setEditingUi(boolean editing, ProfileUIModel profile) {
        binding.layoutEditButtons.setVisibility(editing ? View.VISIBLE : View.GONE);
        binding.btnEdit.setVisibility(editing ? View.GONE : View.VISIBLE);
        binding.btnChangePicture.setVisibility(editing ? View.VISIBLE : View.GONE);

        binding.edtFirstName.setEnabled(editing);
        binding.edtLastName.setEnabled(editing);
        binding.edtPhone.setEnabled(editing);
        binding.edtAddress.setEnabled(editing);

        if (profile != null && "driver".equalsIgnoreCase(profile.role)) {
            binding.btnSave.setText(getString(R.string.profile_action_request_changes));
        } else {
            binding.btnSave.setText(getString(R.string.profile_action_update));
        }
    }

    private void openChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.profile_password_title));

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        EditText current = new EditText(requireContext());
        current.setHint(getString(R.string.profile_password_current));
        current.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText next = new EditText(requireContext());
        next.setHint(getString(R.string.profile_password_new));
        next.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText confirm = new EditText(requireContext());
        confirm.setHint(getString(R.string.profile_password_confirm));
        confirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(current);
        layout.addView(next);
        layout.addView(confirm);

        builder.setView(layout);

        builder.setPositiveButton(getString(R.string.profile_password_submit), (dialog, which) -> {
            String c = current.getText().toString();
            String n = next.getText().toString();
            String k = confirm.getText().toString();

            if (c.length() < 6) {
                Toast.makeText(requireContext(), "Current password is required (min 6)", Toast.LENGTH_LONG).show();
                return;
            }
            if (n.length() < 6) {
                Toast.makeText(requireContext(), "New password is required (min 6)", Toast.LENGTH_LONG).show();
                return;
            }
            if (!n.equals(k)) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_LONG).show();
                return;
            }

            viewModel.changePassword(c, n, k);
        });

        builder.setNegativeButton(getString(R.string.profile_password_cancel), (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void openRejectDialog(String requestId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.profile_admin_reject_reason_title));

        EditText input = new EditText(requireContext());
        input.setHint(getString(R.string.profile_admin_reject_reason_hint));
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.profile_admin_reject), (dialog, which) -> {
            String reason = input.getText().toString().trim();
            boolean ok = viewModel.rejectRequest(requestId, reason.isEmpty() ? null : reason);
            Toast.makeText(requireContext(), ok ? "Rejected" : "Failed", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private static String formatHours(double hours) {
        return String.format(java.util.Locale.getDefault(), "%.2f h", hours);
    }

    private static String safeValue(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private void setProfileImage(String picture) {
        if (TextUtils.isEmpty(picture)) {
            binding.imgProfile.setImageResource(R.drawable.baseline_person_24);
            return;
        }

        String trimmed = picture.trim();

        // Handle local file paths (from our internal storage)
        if (trimmed.startsWith("/")) {
            File file = new File(trimmed);
            if (file.exists()) {
                binding.imgProfile.setImageURI(Uri.fromFile(file));
                return;
            } else {
                binding.imgProfile.setImageResource(R.drawable.baseline_person_24);
                return;
            }
        }

        // Handle file:// URIs
        if (trimmed.startsWith("file://")) {
            binding.imgProfile.setImageURI(Uri.parse(trimmed));
            return;
        }

        // Handle content:// URIs with try-catch for security exceptions
        if (trimmed.startsWith("content://") || trimmed.startsWith("android.resource://")) {
            try {
                binding.imgProfile.setImageURI(Uri.parse(trimmed));
            } catch (SecurityException e) {
                // Permission expired for picker URI, show default
                binding.imgProfile.setImageResource(R.drawable.baseline_person_24);
            }
            return;
        }

        Bitmap bitmap = decodeBase64Image(trimmed);
        if (bitmap != null) {
            binding.imgProfile.setImageBitmap(bitmap);
        } else {
            binding.imgProfile.setImageResource(R.drawable.baseline_person_24);
        }
    }

    private Bitmap decodeBase64Image(String value) {
        try {
            String data = value;
            if (value.startsWith("data:image")) {
                int comma = value.indexOf(',');
                if (comma >= 0) {
                    data = value.substring(comma + 1);
                }
            }
            byte[] decoded = android.util.Base64.decode(data, android.util.Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String copyImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            File imagesDir = new File(requireContext().getFilesDir(), "profile_images");
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }

            String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
            File outputFile = new File(imagesDir, fileName);

            OutputStream outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
