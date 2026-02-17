package com.example.blackcar.presentation.history.viewstate;

import com.example.blackcar.data.api.model.PassengerRideDetailResponse;

public class RideDetailViewState {

    public boolean loading;
    public boolean error;
    public String errorMessage;
    public PassengerRideDetailResponse rideDetail;

    public RideDetailViewState(boolean loading, boolean error, String errorMessage, PassengerRideDetailResponse rideDetail) {
        this.loading = loading;
        this.error = error;
        this.errorMessage = errorMessage;
        this.rideDetail = rideDetail;
    }
}
