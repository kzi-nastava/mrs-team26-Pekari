package com.example.blackcar.presentation.history.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.PassengerRideDetailResponse;
import com.example.blackcar.data.repository.RideRepository;
import com.example.blackcar.presentation.history.viewstate.RideDetailViewState;

public class RideDetailViewModel extends ViewModel {

    private final MutableLiveData<RideDetailViewState> state = new MutableLiveData<>();
    private final RideRepository rideRepository;

    public RideDetailViewModel() {
        this.rideRepository = new RideRepository();
    }

    public LiveData<RideDetailViewState> getState() {
        return state;
    }

    public void loadRideDetail(Long rideId) {
        state.setValue(new RideDetailViewState(true, false, null, null));

        rideRepository.getPassengerRideDetail(rideId, new RideRepository.RepoCallback<PassengerRideDetailResponse>() {
            @Override
            public void onSuccess(PassengerRideDetailResponse data) {
                state.setValue(new RideDetailViewState(false, false, null, data));
            }

            @Override
            public void onError(String message) {
                state.setValue(new RideDetailViewState(false, true, message, null));
            }
        });
    }
}
