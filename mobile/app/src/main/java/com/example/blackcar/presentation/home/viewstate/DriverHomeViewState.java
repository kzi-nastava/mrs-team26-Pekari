package com.example.blackcar.presentation.home.viewstate;

import com.example.blackcar.data.api.model.ActiveRideResponse;
import com.example.blackcar.data.api.model.LocationPoint;

/**
 * ViewState for driver home / active ride display
 */
public class DriverHomeViewState {

    public boolean loading;
    public boolean error;
    public String errorMessage;
    public ActiveRideResponse activeRide;
    public boolean actionInProgress;
    public boolean stopRequested;
    public boolean panicActivated;
    public LocationPoint currentLocation;

    public static DriverHomeViewState idle() {
        DriverHomeViewState s = new DriverHomeViewState();
        s.loading = false;
        s.error = false;
        s.errorMessage = null;
        s.activeRide = null;
        s.actionInProgress = false;
        s.stopRequested = false;
        s.panicActivated = false;
        s.currentLocation = null;
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
        s.stopRequested = "STOP_REQUESTED".equals(ride != null ? ride.getStatus() : null);
        return s;
    }

    public static DriverHomeViewState actionInProgress(ActiveRideResponse ride) {
        DriverHomeViewState s = withActiveRide(ride);
        s.actionInProgress = true;
        return s;
    }

    public static DriverHomeViewState withPanicActivated(ActiveRideResponse ride) {
        DriverHomeViewState s = withActiveRide(ride);
        s.panicActivated = true;
        return s;
    }

    public DriverHomeViewState withCurrentLocation(LocationPoint location) {
        this.currentLocation = location;
        return this;
    }

    public DriverHomeViewState copyWithPanic(boolean panicActivated) {
        DriverHomeViewState copy = new DriverHomeViewState();
        copy.loading = this.loading;
        copy.error = this.error;
        copy.errorMessage = this.errorMessage;
        copy.activeRide = this.activeRide;
        copy.actionInProgress = this.actionInProgress;
        copy.stopRequested = this.stopRequested;
        copy.panicActivated = panicActivated;
        copy.currentLocation = this.currentLocation;
        return copy;
    }
}
