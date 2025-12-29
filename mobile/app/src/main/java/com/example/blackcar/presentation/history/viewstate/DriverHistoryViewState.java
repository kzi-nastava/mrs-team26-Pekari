package com.example.blackcar.presentation.history.viewstate;


import java.util.List;

public class DriverHistoryViewState {

    public boolean loading;
    public boolean error;
    public String errorMessage;
    public List<RideUIModel> rides;

    public DriverHistoryViewState(boolean loading, boolean error, String errorMessage, List<RideUIModel> rides) {
        this.loading = loading;
        this.error = error;
        this.errorMessage = errorMessage;
        this.rides = rides;
    }
}
