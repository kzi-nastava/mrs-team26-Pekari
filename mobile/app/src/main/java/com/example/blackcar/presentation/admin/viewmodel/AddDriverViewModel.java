package com.example.blackcar.presentation.admin.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.RegisterDriverResponse;
import com.example.blackcar.data.repository.AuthRepository;
import com.example.blackcar.presentation.admin.viewstate.AddDriverViewState;

public class AddDriverViewModel extends ViewModel {

    private final MutableLiveData<AddDriverViewState> addDriverState =
            new MutableLiveData<>(new AddDriverViewState.Idle());
    private final AuthRepository authRepository;

    public AddDriverViewModel(Context context) {
        this.authRepository = new AuthRepository(context);
    }

    public LiveData<AddDriverViewState> getAddDriverState() {
        return addDriverState;
    }

    public void registerDriver(String email, String firstName, String lastName, String address,
                               String phoneNumber, String vehicleModel, String vehicleType,
                               String licensePlate, int numberOfSeats, boolean babyFriendly,
                               boolean petFriendly) {
        addDriverState.setValue(new AddDriverViewState.Loading());

        authRepository.registerDriver(email, firstName, lastName, address, phoneNumber,
                vehicleModel, vehicleType, licensePlate, numberOfSeats, babyFriendly, petFriendly,
                new AuthRepository.RepoCallback<RegisterDriverResponse>() {
                    @Override
                    public void onSuccess(RegisterDriverResponse data) {
                        String msg = data != null && data.getMessage() != null
                                ? data.getMessage()
                                : "Driver registered. Activation link sent to email.";
                        addDriverState.postValue(new AddDriverViewState.Success(msg));
                    }

                    @Override
                    public void onError(String message) {
                        addDriverState.postValue(new AddDriverViewState.Error(
                                message != null ? message : "Failed to register driver"));
                    }
                });
    }
}
