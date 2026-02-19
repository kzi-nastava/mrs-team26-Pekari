package com.example.blackcar.presentation.admin.views;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.blackcar.R;
import com.example.blackcar.data.api.model.UserListItemResponse;
import com.example.blackcar.databinding.FragmentUserManagementBinding;
import com.example.blackcar.presentation.admin.viewmodel.UserManagementViewModel;

public class UserManagementFragment extends Fragment implements UserManagementAdapter.OnUserActionListener {

    private FragmentUserManagementBinding binding;
    private UserManagementViewModel viewModel;
    private UserManagementAdapter driversAdapter;
    private UserManagementAdapter passengersAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(UserManagementViewModel.class);

        driversAdapter = new UserManagementAdapter(this);
        passengersAdapter = new UserManagementAdapter(this);
        binding.recyclerDrivers.setAdapter(driversAdapter);
        binding.recyclerPassengers.setAdapter(passengersAdapter);

        observeState();
        viewModel.loadAll();
    }

    private void observeState() {
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            binding.progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);

            if (state.error && state.errorMessage != null) {
                binding.txtError.setVisibility(View.VISIBLE);
                binding.txtError.setText(state.errorMessage);
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show();
            } else {
                binding.txtError.setVisibility(View.GONE);
            }

            if (state.successMessage != null && !state.successMessage.isEmpty()) {
                binding.txtSuccess.setVisibility(View.VISIBLE);
                binding.txtSuccess.setText(state.successMessage);
                Toast.makeText(requireContext(), state.successMessage, Toast.LENGTH_SHORT).show();
            } else {
                binding.txtSuccess.setVisibility(View.GONE);
            }

            if (!state.loading) {
                driversAdapter.submitList(state.drivers);
                passengersAdapter.submitList(state.passengers);

                binding.txtDriversEmpty.setVisibility(state.drivers.isEmpty() ? View.VISIBLE : View.GONE);
                binding.txtPassengersEmpty.setVisibility(state.passengers.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onBlockClick(UserListItemResponse user) {
        openBlockDialog(user);
    }

    @Override
    public void onUnblockClick(UserListItemResponse user) {
        viewModel.unblockUser(user);
    }

    private void openBlockDialog(UserListItemResponse user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.user_management_block_dialog_title));

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        android.widget.TextView userInfo = new android.widget.TextView(requireContext());
        userInfo.setText(UserManagementViewModel.fullName(user) + " (" + (user.getEmail() != null ? user.getEmail() : "") + ")");
        userInfo.setTextColor(requireContext().getColor(R.color.text_secondary));
        userInfo.setTextSize(14);
        userInfo.setPadding(0, 0, 0, padding);
        layout.addView(userInfo);

        EditText noteInput = new EditText(requireContext());
        noteInput.setHint(getString(R.string.user_management_block_dialog_note_hint));
        noteInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        noteInput.setMinLines(3);
        noteInput.setText(user.getBlockedNote() != null ? user.getBlockedNote() : "");
        layout.addView(noteInput);

        builder.setView(layout);

        builder.setPositiveButton(getString(R.string.user_management_block_dialog_confirm), (dialog, which) -> {
            String note = noteInput.getText() != null ? noteInput.getText().toString().trim() : "";
            viewModel.blockUser(user, note.isEmpty() ? null : note);
        });

        builder.setNegativeButton(getString(R.string.user_management_block_dialog_cancel), (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
