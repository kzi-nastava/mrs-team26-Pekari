package com.example.blackcar.presentation.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.presentation.auth.viewstate.RegisterViewState;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class RegisterViewModel extends ViewModel {

    private final MutableLiveData<RegisterViewState> registerState = 
        new MutableLiveData<>(new RegisterViewState.Idle());
    private final ScheduledExecutorService executorService;

    public RegisterViewModel() {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public LiveData<RegisterViewState> getRegisterState() {
        return registerState;
    }

    public void register(String firstName, String lastName, String email, 
                        String address, String phoneNumber, String password) {
        registerState.setValue(new RegisterViewState.Loading());

        executorService.execute(() -> {
            try {
                Thread.sleep(1500);

                boolean success = !firstName.isEmpty() && !lastName.isEmpty() && 
                                !email.isEmpty() && !address.isEmpty() && 
                                !phoneNumber.isEmpty() && password.length() >= 6;

                if (success) {
                    registerState.postValue(new RegisterViewState.Success());
                } else {
                    registerState.postValue(new RegisterViewState.Error("Registration failed"));
                }
            } catch (Exception e) {
                registerState.postValue(new RegisterViewState.Error("Network error: " + e.getMessage()));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
