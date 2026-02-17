package com.example.blackcar.presentation.history.viewstate;


import java.util.List;

public class PassengerHistoryViewState {

    public boolean loading;
    public boolean error;
    public String errorMessage;
    public List<RideUIModel> rides;
    public String sortField;  // date, price, distance, vehicleType, status, pickup, dropoff
    public boolean sortAscending;  // true = ascending, false = descending

    public PassengerHistoryViewState(boolean loading, boolean error, String errorMessage, List<RideUIModel> rides) {
        this.loading = loading;
        this.error = error;
        this.errorMessage = errorMessage;
        this.rides = rides;
        this.sortField = "date";
        this.sortAscending = false;  // default: newest first
    }

    public PassengerHistoryViewState(boolean loading, boolean error, String errorMessage, List<RideUIModel> rides,
                                 String sortField, boolean sortAscending) {
        this.loading = loading;
        this.error = error;
        this.errorMessage = errorMessage;
        this.rides = rides;
        this.sortField = sortField;
        this.sortAscending = sortAscending;
    }
}
