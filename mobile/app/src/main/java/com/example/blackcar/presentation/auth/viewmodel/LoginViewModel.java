package com.example.blackcar.presentation.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.presentation.auth.viewstate.LoginViewState;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<LoginViewState> loginState = new MutableLiveData<>(new LoginViewState.Idle());
    private final ScheduledExecutorService executorService;

    public LoginViewModel() {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public LiveData<LoginViewState> getLoginState() {
        return loginState;
    }

    public void login(String email, String password) {
        loginState.setValue(new LoginViewState.Loading());

        executorService.execute(() -> {
            try {
                Thread.sleep(1500);

                boolean success = !email.isEmpty() && password.length() >= 6;

                if (success) {
                    loginState.postValue(new LoginViewState.Success("dummy_user_123"));
                } else {
                    loginState.postValue(new LoginViewState.Error("Invalid email or password"));
                }
            } catch (Exception e) {
                loginState.postValue(new LoginViewState.Error("Network error: " + e.getMessage()));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
