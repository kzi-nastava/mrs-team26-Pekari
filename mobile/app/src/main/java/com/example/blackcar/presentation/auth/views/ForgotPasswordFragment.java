package com.example.blackcar.presentation.auth.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.blackcar.R;
import com.example.blackcar.databinding.FragmentForgotPasswordBinding;
import com.example.blackcar.presentation.auth.viewmodel.ForgotPasswordViewModel;
import com.example.blackcar.presentation.auth.viewstate.ForgotPasswordViewState;

public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private ForgotPasswordViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        binding.resetPasswordButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();

            if (validateInput(email)) {
                viewModel.sendResetEmail(email);
            }
        });

        binding.backToLoginLink.setOnClickListener(v -> {
            Navigation.findNavController(binding.getRoot()).navigateUp();
        });
    }

    private void observeViewModel() {
        viewModel.getResetState().observe(getViewLifecycleOwner(), state -> {
            if (state instanceof ForgotPasswordViewState.Idle) {
                hideLoading();
            } else if (state instanceof ForgotPasswordViewState.Loading) {
                showLoading();
            } else if (state instanceof ForgotPasswordViewState.Success) {
                hideLoading();
                showSuccessMessage();
            } else if (state instanceof ForgotPasswordViewState.Error) {
                hideLoading();
                ForgotPasswordViewState.Error errorState = (ForgotPasswordViewState.Error) state;
                showError(errorState.getMessage());
            }
        });
    }

    private boolean validateInput(String email) {
        if (email.isEmpty()) {
            binding.emailEditText.setError("Email is required");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.setError("Invalid email format");
            return false;
        }

        return true;
    }

    private void showLoading() {
        binding.resetPasswordButton.setEnabled(false);
        binding.resetPasswordButton.setText("Sending...");
        binding.emailEditText.setEnabled(false);
    }

    private void hideLoading() {
        binding.resetPasswordButton.setEnabled(true);
        binding.resetPasswordButton.setText("Send Reset Link");
        binding.emailEditText.setEnabled(true);
    }

    private void showSuccessMessage() {
        Toast.makeText(requireContext(), 
            "Instructions for password change sent on email", 
            Toast.LENGTH_LONG).show();

        Navigation.findNavController(binding.getRoot()).navigateUp();
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
