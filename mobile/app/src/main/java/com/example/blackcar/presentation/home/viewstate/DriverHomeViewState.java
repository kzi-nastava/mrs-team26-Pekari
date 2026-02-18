package com.example.blackcar.presentation.home.viewstate;

import com.example.blackcar.data.api.model.ActiveRideResponse;

/**
 * ViewState for driver home / active ride display
 */
public class DriverHomeViewState {

    public boolean loading;
    public boolean error;
    public String errorMessage;
    public ActiveRideResponse activeRide;
    public boolean actionInProgress;

    public static DriverHomeViewState idle() {
        DriverHomeViewState s = new DriverHomeViewState();
        s.loading = false;
        s.error = false;
        s.errorMessage = null;
        s.activeRide = null;
        s.actionInProgress = false;
        return s;
    }

    public static DriverHomeViewState loading() {
        DriverHomeViewState s = idle();
        s.loading = true;
        return s;
    }

    public static DriverHomeViewState error(String message) {
        DriverHomeViewState s = idle();
        s.error = true;
        s.errorMessage = message;
        return s;
    }

    public static DriverHomeViewState withActiveRide(ActiveRideResponse ride) {
        DriverHomeViewState s = idle();
        s.activeRide = ride;
        return s;
    }

    public static DriverHomeViewState actionInProgress(ActiveRideResponse ride) {
        DriverHomeViewState s = withActiveRide(ride);
        s.actionInProgress = true;
        return s;
    }
}
