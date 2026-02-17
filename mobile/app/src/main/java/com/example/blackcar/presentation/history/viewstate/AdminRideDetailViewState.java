package com.example.blackcar.presentation.history.viewstate;

import com.example.blackcar.data.api.model.AdminRideDetailResponse;

/**
 * ViewState for admin ride detail screen.
 */
public class AdminRideDetailViewState {

    public boolean loading;
    public boolean error;
    public String errorMessage;
    public AdminRideDetailResponse rideDetail;

    public AdminRideDetailViewState(boolean loading, boolean error, String errorMessage, AdminRideDetailResponse rideDetail) {
        this.loading = loading;
        this.error = error;
        this.errorMessage = errorMessage;
        this.rideDetail = rideDetail;
    }
}
