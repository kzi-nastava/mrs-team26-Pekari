package com.example.blackcar.presentation.history.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.AdminRideDetailResponse;
import com.example.blackcar.data.repository.RideRepository;
import com.example.blackcar.presentation.history.viewstate.AdminRideDetailViewState;

public class AdminRideDetailViewModel extends ViewModel {

    private final MutableLiveData<AdminRideDetailViewState> state = new MutableLiveData<>();
    private final RideRepository rideRepository;

    public AdminRideDetailViewModel() {
        this.rideRepository = new RideRepository();
    }

    public LiveData<AdminRideDetailViewState> getState() {
        return state;
    }

    public void loadRideDetail(Long rideId) {
        state.setValue(new AdminRideDetailViewState(true, false, null, null));

        rideRepository.getAdminRideDetail(rideId, new RideRepository.RepoCallback<AdminRideDetailResponse>() {
            @Override
            public void onSuccess(AdminRideDetailResponse data) {
                state.setValue(new AdminRideDetailViewState(false, false, null, data));
            }

            @Override
            public void onError(String message) {
                state.setValue(new AdminRideDetailViewState(false, true, message, null));
            }
        });
    }
}
