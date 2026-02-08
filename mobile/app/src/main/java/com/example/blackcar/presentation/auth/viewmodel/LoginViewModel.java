package com.example.blackcar.presentation.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.repository.AuthRepository;
import com.example.blackcar.presentation.auth.viewstate.LoginViewState;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<LoginViewState> loginState = new MutableLiveData<>(new LoginViewState.Idle());
    private final AuthRepository authRepository = new AuthRepository();

    public LiveData<LoginViewState> getLoginState() {
        return loginState;
    }

    public void login(String email, String password) {
        loginState.setValue(new LoginViewState.Loading());
        authRepository.login(email, password, new AuthRepository.RepoCallback<String>() {
            @Override
            public void onSuccess(String userId) {
                loginState.postValue(new LoginViewState.Success(userId));
            }

            @Override
            public void onError(String message) {
                loginState.postValue(new LoginViewState.Error(message != null ? message : "Login failed"));
            }
        });
    }
}
