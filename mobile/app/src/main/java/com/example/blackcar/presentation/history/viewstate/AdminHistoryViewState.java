package com.example.blackcar.presentation.history.viewstate;

import java.util.List;

/**
 * ViewState for admin ride history screen.
 */
public class AdminHistoryViewState {

    public boolean loading;
    public boolean error;
    public String errorMessage;
    public List<AdminRideUIModel> rides;
    public String sortField;      // createdAt, startedAt, completedAt, price, distanceKm, status, pickup, dropoff
    public boolean sortAscending; // true = ascending, false = descending

    public AdminHistoryViewState(boolean loading, boolean error, String errorMessage, List<AdminRideUIModel> rides) {
        this.loading = loading;
        this.error = error;
        this.errorMessage = errorMessage;
        this.rides = rides;
        this.sortField = "createdAt";
        this.sortAscending = false;  // default: newest first
    }

    public AdminHistoryViewState(boolean loading, boolean error, String errorMessage, List<AdminRideUIModel> rides,
                                 String sortField, boolean sortAscending) {
        this.loading = loading;
        this.error = error;
        this.errorMessage = errorMessage;
        this.rides = rides;
        this.sortField = sortField;
        this.sortAscending = sortAscending;
    }
}
