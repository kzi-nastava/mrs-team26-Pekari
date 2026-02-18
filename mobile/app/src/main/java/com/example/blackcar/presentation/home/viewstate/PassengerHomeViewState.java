package com.example.blackcar.presentation.home.viewstate;

import com.example.blackcar.data.api.model.ActiveRideResponse;
import com.example.blackcar.data.api.model.OrderRideResponse;
import com.example.blackcar.data.api.model.RideEstimateResponse;

/**
 * ViewState for passenger home / ride ordering
 */
public class PassengerHomeViewState {

    public boolean loading;
    public boolean error;
    public String errorMessage;
    public RideEstimateResponse estimate;
    public OrderRideResponse orderResult;
    public ActiveRideResponse activeRide;
    public boolean formDisabled;
    public boolean stopRequested;
    public boolean panicActivated;

    public static PassengerHomeViewState idle() {
        PassengerHomeViewState s = new PassengerHomeViewState();
        s.loading = false;
        s.error = false;
        s.errorMessage = null;
        s.estimate = null;
        s.orderResult = null;
        s.activeRide = null;
        s.formDisabled = false;
        s.stopRequested = false;
        s.panicActivated = false;
        return s;
    }

    public static PassengerHomeViewState loading() {
        PassengerHomeViewState s = idle();
        s.loading = true;
        return s;
    }

    public static PassengerHomeViewState error(String message) {
        PassengerHomeViewState s = idle();
        s.error = true;
        s.errorMessage = message;
        return s;
    }

    public static PassengerHomeViewState withEstimate(RideEstimateResponse estimate) {
        PassengerHomeViewState s = idle();
        s.estimate = estimate;
        return s;
    }

    public static PassengerHomeViewState withOrderResult(OrderRideResponse orderResult, boolean formDisabled) {
        PassengerHomeViewState s = idle();
        s.orderResult = orderResult;
        s.formDisabled = formDisabled;
        return s;
    }

    public static PassengerHomeViewState withActiveRide(ActiveRideResponse activeRide) {
        PassengerHomeViewState s = idle();
        s.activeRide = activeRide;
        s.formDisabled = true;
        s.orderResult = mapActiveRideToOrderResult(activeRide);
        s.stopRequested = "STOP_REQUESTED".equals(activeRide != null ? activeRide.getStatus() : null);
        return s;
    }

    private static OrderRideResponse mapActiveRideToOrderResult(ActiveRideResponse r) {
        if (r == null) return null;
        OrderRideResponse o = new OrderRideResponse();
        o.setRideId(r.getRideId());
        o.setStatus(r.getStatus());
        o.setMessage("Active ride in progress");
        o.setEstimatedPrice(r.getEstimatedPrice());
        o.setScheduledAt(r.getScheduledAt());
        o.setAssignedDriverEmail(r.getDriver() != null ? r.getDriver().getEmail() : null);
        return o;
    }
}
