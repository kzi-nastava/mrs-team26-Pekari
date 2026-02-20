package com.example.blackcar.presentation.admin.viewstate;

import com.example.blackcar.data.api.model.DriverRideHistoryResponse;

import java.util.Collections;
import java.util.List;

public abstract class PanicPanelViewState {

    public static class Idle extends PanicPanelViewState {
        private static final Idle INSTANCE = new Idle();
        public static Idle getInstance() { return INSTANCE; }
    }

    public static class Loading extends PanicPanelViewState {
        private static final Loading INSTANCE = new Loading();
        public static Loading getInstance() { return INSTANCE; }
    }

    public static class Success extends PanicPanelViewState {
        private final List<DriverRideHistoryResponse> panicRides;

        public Success(List<DriverRideHistoryResponse> panicRides) {
            this.panicRides = panicRides != null ? panicRides : Collections.emptyList();
        }

        public List<DriverRideHistoryResponse> getPanicRides() {
            return panicRides;
        }
    }

    public static class Error extends PanicPanelViewState {
        private final String message;

        public Error(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
