package com.example.blackcar.presentation.auth.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.repository.AuthRepository;
import com.example.blackcar.presentation.auth.viewstate.RegisterViewState;

public class RegisterViewModel extends ViewModel {

    private final MutableLiveData<RegisterViewState> registerState =
        new MutableLiveData<>(new RegisterViewState.Idle());
    private final AuthRepository authRepository;

    public RegisterViewModel(Context context) {
        this.authRepository = new AuthRepository(context);
    }

    public LiveData<RegisterViewState> getRegisterState() {
        return registerState;
    }

    public void register(String firstName, String lastName, String email,
                        String address, String phoneNumber, String password) {
        registerState.setValue(new RegisterViewState.Loading());

        authRepository.register(firstName, lastName, email, address, phoneNumber, password,
                new AuthRepository.RepoCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        registerState.postValue(new RegisterViewState.Success());
                    }

                    @Override
                    public void onError(String message) {
                        registerState.postValue(new RegisterViewState.Error(message != null ? message : "Registration failed"));
                    }
                });
    }
}
