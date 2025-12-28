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
import com.example.blackcar.databinding.FragmentRegisterBinding;
import com.example.blackcar.presentation.auth.viewmodel.RegisterViewModel;
import com.example.blackcar.presentation.auth.viewstate.RegisterViewState;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private RegisterViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        binding.registerButton.setOnClickListener(v -> {
            String firstName = binding.firstNameEditText.getText().toString().trim();
            String lastName = binding.lastNameEditText.getText().toString().trim();
            String email = binding.emailEditText.getText().toString().trim();
            String address = binding.addressEditText.getText().toString().trim();
            String phoneNumber = binding.phoneNumberEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();

            if (validateInput(firstName, lastName, email, address, phoneNumber, password, confirmPassword)) {
                viewModel.register(firstName, lastName, email, address, phoneNumber, password);
            }
        });

        binding.loginLink.setOnClickListener(v -> {
            Navigation.findNavController(binding.getRoot()).navigateUp();
        });
    }

    private void observeViewModel() {
        viewModel.getRegisterState().observe(getViewLifecycleOwner(), state -> {
            if (state instanceof RegisterViewState.Idle) {
                hideLoading();
            } else if (state instanceof RegisterViewState.Loading) {
                showLoading();
            } else if (state instanceof RegisterViewState.Success) {
                hideLoading();
                showSuccessMessage();
            } else if (state instanceof RegisterViewState.Error) {
                hideLoading();
                RegisterViewState.Error errorState = (RegisterViewState.Error) state;
                showError(errorState.getMessage());
            }
        });
    }

    private boolean validateInput(String firstName, String lastName, String email, 
                                   String address, String phoneNumber, 
                                   String password, String confirmPassword) {
        if (firstName.isEmpty()) {
            binding.firstNameEditText.setError("First name is required");
            return false;
        }

        if (lastName.isEmpty()) {
            binding.lastNameEditText.setError("Last name is required");
            return false;
        }

        if (email.isEmpty()) {
            binding.emailEditText.setError("Email is required");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.setError("Invalid email format");
            return false;
        }

        if (address.isEmpty()) {
            binding.addressEditText.setError("Address is required");
            return false;
        }

        if (phoneNumber.isEmpty()) {
            binding.phoneNumberEditText.setError("Phone number is required");
            return false;
        }

        if (password.isEmpty()) {
            binding.passwordEditText.setError("Password is required");
            return false;
        }

        if (password.length() < 6) {
            binding.passwordEditText.setError("Password must be at least 6 characters");
            return false;
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordEditText.setError("Please confirm your password");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordEditText.setError("Passwords do not match");
            return false;
        }

        return true;
    }

    private void showLoading() {
        binding.registerButton.setEnabled(false);
        binding.registerButton.setText("Creating account...");
        setInputsEnabled(false);
    }

    private void hideLoading() {
        binding.registerButton.setEnabled(true);
        binding.registerButton.setText("Create account");
        setInputsEnabled(true);
    }

    private void setInputsEnabled(boolean enabled) {
        binding.firstNameEditText.setEnabled(enabled);
        binding.lastNameEditText.setEnabled(enabled);
        binding.emailEditText.setEnabled(enabled);
        binding.addressEditText.setEnabled(enabled);
        binding.phoneNumberEditText.setEnabled(enabled);
        binding.passwordEditText.setEnabled(enabled);
        binding.confirmPasswordEditText.setEnabled(enabled);
    }

    private void showSuccessMessage() {
        Toast.makeText(requireContext(), 
            "Activation link was sent on email", 
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
