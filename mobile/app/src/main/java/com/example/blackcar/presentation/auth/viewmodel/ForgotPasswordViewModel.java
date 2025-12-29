package com.example.blackcar.presentation.auth.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.presentation.auth.viewstate.ForgotPasswordViewState;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ForgotPasswordViewModel extends ViewModel {

    private final MutableLiveData<ForgotPasswordViewState> resetState = 
        new MutableLiveData<>(new ForgotPasswordViewState.Idle());
    private final ScheduledExecutorService executorService;

    public ForgotPasswordViewModel() {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public LiveData<ForgotPasswordViewState> getResetState() {
        return resetState;
    }

    public void sendResetEmail(String email) {
        resetState.setValue(new ForgotPasswordViewState.Loading());

        executorService.execute(() -> {
            try {
                Thread.sleep(1500);

                boolean success = !email.isEmpty() && 
                    android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();

                if (success) {
                    resetState.postValue(new ForgotPasswordViewState.Success());
                } else {
                    resetState.postValue(new ForgotPasswordViewState.Error("Invalid email address"));
                }
            } catch (Exception e) {
                resetState.postValue(new ForgotPasswordViewState.Error("Network error: " + e.getMessage()));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
