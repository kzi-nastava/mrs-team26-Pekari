package com.example.blackcar.presentation.views.profile;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.blackcar.BlackCarApp;
import com.example.blackcar.BuildConfig;
import com.example.blackcar.R;
import com.example.blackcar.databinding.DialogChangePasswordBinding;
import com.example.blackcar.databinding.FragmentProfileBinding;
import com.example.blackcar.domain.model.DriverInfo;
import com.example.blackcar.domain.model.ProfileData;
import com.example.blackcar.domain.model.UserRole;
import com.example.blackcar.presentation.viewmodel.AppViewModelFactory;
import com.example.blackcar.presentation.viewmodel.ProfileViewModel;
import com.example.blackcar.presentation.viewmodel.ProfileViewState;

public final class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private ActivityResultLauncher<String> imagePicker;

    @Nullable
    private String selectedPictureUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedPictureUri = uri.toString();
                bindProfilePicture(selectedPictureUri);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BlackCarApp app = (BlackCarApp) requireActivity().getApplication();
        AppViewModelFactory factory = new AppViewModelFactory(app.getAppContainer().getSessionManager(), app.getAppContainer().getProfileRepository());
        viewModel = new ViewModelProvider(this, factory).get(ProfileViewModel.class);

        binding.btnEdit.setOnClickListener(v -> viewModel.toggleEdit());
        binding.btnCancel.setOnClickListener(v -> {
            viewModel.toggleEdit();
            viewModel.refresh();
        });
        binding.btnSubmit.setOnClickListener(v -> submit());
        binding.btnChangePassword.setOnClickListener(v -> openChangePasswordDialog());
        binding.btnChangePicture.setOnClickListener(v -> imagePicker.launch("image/*"));

        if (BuildConfig.DEBUG) {
            binding.devRoleCard.setVisibility(View.VISIBLE);
            binding.btnRolePassenger.setOnClickListener(v -> {
                viewModel.devSetRole(UserRole.PASSENGER);
                requireActivity().invalidateOptionsMenu();
            });
            binding.btnRoleDriver.setOnClickListener(v -> {
                viewModel.devSetRole(UserRole.DRIVER);
                requireActivity().invalidateOptionsMenu();
            });
            binding.btnRoleAdmin.setOnClickListener(v -> {
                viewModel.devSetRole(UserRole.ADMIN);
                requireActivity().invalidateOptionsMenu();
            });
        }

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.refresh();
    }

    private void render(@NonNull ProfileViewState state) {
        ProfileData profile = state.profile;

        // banners
        if (state.successMessage != null) {
            binding.successBanner.setVisibility(View.VISIBLE);
            binding.successBanner.setText(state.successMessage);
        } else {
            binding.successBanner.setVisibility(View.GONE);
        }
        if (state.errorMessage != null) {
            binding.errorBanner.setVisibility(View.VISIBLE);
            binding.errorBanner.setText(state.errorMessage);
        } else {
            binding.errorBanner.setVisibility(View.GONE);
        }

        boolean editing = state.isEditing;
        binding.btnEdit.setVisibility(editing ? View.GONE : View.VISIBLE);
        binding.editButtonsRow.setVisibility(editing ? View.VISIBLE : View.GONE);
        binding.btnChangePicture.setVisibility(editing ? View.VISIBLE : View.GONE);

        setEditable(binding.firstName, editing);
        setEditable(binding.lastName, editing);
        setEditable(binding.phone, editing);
        setEditable(binding.address, editing);

        if (profile != null) {
            binding.firstName.setText(profile.getFirstName());
            binding.lastName.setText(profile.getLastName());
            binding.phone.setText(profile.getPhoneNumber());
            binding.address.setText(profile.getAddress());
            binding.email.setText(profile.getEmail());
            String picture = selectedPictureUri != null ? selectedPictureUri : profile.getProfilePicture();
            bindProfilePicture(picture);
        }

        boolean isDriver = state.role == UserRole.DRIVER;
        binding.driverCard.setVisibility(isDriver && state.driverInfo != null ? View.VISIBLE : View.GONE);
        if (isDriver && state.driverInfo != null) {
            DriverInfo info = state.driverInfo;
            binding.hoursActive.setText(formatHours(info.getHoursActiveLast24h()));
            binding.vehicleInfo.setText(
                    info.getVehicle().getMake() + " " + info.getVehicle().getModel() + " (" + info.getVehicle().getYear() + ")\n" +
                            "Plate: " + info.getVehicle().getLicensePlate() + "\n" +
                            "VIN: " + info.getVehicle().getVin()
            );
            binding.btnSubmit.setText(getString(R.string.request_changes));
        } else {
            binding.btnSubmit.setText(getString(R.string.update_profile));
        }
    }

    private void submit() {
        String first = safeText(binding.firstName);
        String last = safeText(binding.lastName);
        String phone = safeText(binding.phone);
        String address = safeText(binding.address);

        viewModel.submitProfileUpdate(first, last, phone, address, selectedPictureUri);
    }

    private void openChangePasswordDialog() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        DialogChangePasswordBinding dialogBinding = DialogChangePasswordBinding.inflate(inflater);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.password_dialog_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.submit, (d, which) -> viewModel.submitPasswordChange(
                        safeText(dialogBinding.currentPassword),
                        safeText(dialogBinding.newPassword),
                        safeText(dialogBinding.confirmPassword)
                ))
                .setNegativeButton(R.string.cancel, (d, which) -> d.dismiss())
                .show();
    }

    private void bindProfilePicture(@Nullable String picture) {
        if (picture == null || picture.trim().isEmpty()) {
            binding.profileImage.setImageResource(R.drawable.ic_profile_placeholder);
            return;
        }
        try {
            Uri uri = Uri.parse(picture);
            binding.profileImage.setImageURI(uri);
        } catch (Exception ignored) {
            binding.profileImage.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private static void setEditable(@NonNull View view, boolean editable) {
        view.setEnabled(editable);
    }

    @NonNull
    private static String safeText(@NonNull com.google.android.material.textfield.TextInputEditText editText) {
        if (editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }

    private static String formatHours(double hours) {
        if (hours <= 0) return "0h 0m";
        int h = (int) Math.floor(hours);
        int m = (int) Math.round((hours - h) * 60.0);
        return h + "h " + m + "m";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
