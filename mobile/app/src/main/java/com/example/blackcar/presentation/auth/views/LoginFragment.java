package com.example.blackcar.presentation.auth.views;

import android.os.Bundle;
import android.util.Log;
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
import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.databinding.FragmentLoginBinding;
import com.example.blackcar.presentation.ViewModelFactory;
import com.example.blackcar.presentation.auth.viewmodel.LoginViewModel;
import com.example.blackcar.presentation.auth.viewstate.LoginViewState;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i("LoginFragment", "--------------------------------------------------");
        Log.i("LoginFragment", "[DEBUG_LOG] LoginFragment.onViewCreated() starting...");
        Log.i("LoginFragment", "--------------------------------------------------");

        // Initialize ApiClient with context
        ApiClient.init(requireContext());

        ViewModelFactory factory = new ViewModelFactory(requireContext());
        viewModel = new ViewModelProvider(this, factory).get(LoginViewModel.class);

        setupClickListeners();

        observeViewModel();
    }

    private void setupClickListeners() {
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();

            if (validateInput(email, password)) {
                viewModel.login(email, password);
            }
        });

        binding.forgotPasswordLink.setOnClickListener(v -> {
            Navigation.findNavController(binding.getRoot())
                    .navigate(R.id.action_login_to_forgot_password);
        });

        binding.registerLink.setOnClickListener(v -> {
            Navigation.findNavController(binding.getRoot())
                    .navigate(R.id.action_login_to_register);
        });
    }

    private void observeViewModel() {
        viewModel.getLoginState().observe(getViewLifecycleOwner(), state -> {
            if (state instanceof LoginViewState.Idle) {
                hideLoading();
            } else if (state instanceof LoginViewState.Loading) {
                showLoading();
            } else if (state instanceof LoginViewState.Success) {
                hideLoading();
                LoginViewState.Success successState = (LoginViewState.Success) state;
                onLoginSuccess(successState.getUserId());
            } else if (state instanceof LoginViewState.Error) {
                hideLoading();
                LoginViewState.Error errorState = (LoginViewState.Error) state;
                showError(errorState.getMessage());
            }
        });
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            binding.emailEditText.setError("Email is required");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.setError("Invalid email format");
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

        return true;
    }

    private void showLoading() {
        binding.loginButton.setEnabled(false);
        binding.loginButton.setText("Logging in...");
        binding.emailEditText.setEnabled(false);
        binding.passwordEditText.setEnabled(false);
    }

    private void hideLoading() {
        binding.loginButton.setEnabled(true);
        binding.loginButton.setText("Log in");
        binding.emailEditText.setEnabled(true);
        binding.passwordEditText.setEnabled(true);
    }

    private void onLoginSuccess(String userId) {
        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(binding.getRoot())
                .navigate(R.id.action_login_to_home);
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
