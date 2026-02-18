package com.example.blackcar.presentation.home.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.ActiveRideResponse;
import com.example.blackcar.data.api.model.MessageResponse;
import com.example.blackcar.data.repository.RideRepository;
import com.example.blackcar.presentation.home.viewstate.DriverHomeViewState;

import java.text.DecimalFormat;

public class DriverHomeViewModel extends ViewModel {

    private final RideRepository rideRepository = new RideRepository();

    private final MutableLiveData<DriverHomeViewState> state = new MutableLiveData<>(DriverHomeViewState.loading());

    public LiveData<DriverHomeViewState> getState() {
        return state;
    }

    public void loadActiveRide() {
        state.setValue(DriverHomeViewState.loading());

        rideRepository.getActiveRideForDriver(new RideRepository.RepoCallback<ActiveRideResponse>() {
            @Override
            public void onSuccess(ActiveRideResponse data) {
                if (data != null) {
                    state.postValue(DriverHomeViewState.withActiveRide(data));
                } else {
                    state.postValue(DriverHomeViewState.idle());
                }
            }

            @Override
            public void onError(String message) {
                state.postValue(DriverHomeViewState.error(message));
            }
        });
    }

    public void cancelRide() {
        DriverHomeViewState current = state.getValue();
        if (current == null || current.activeRide == null) return;

        Long rideId = current.activeRide.getRideId();
        state.setValue(DriverHomeViewState.actionInProgress(current.activeRide));

        rideRepository.cancelRide(rideId, "Cancelled by driver", new RideRepository.RepoCallback<MessageResponse>() {
            @Override
            public void onSuccess(MessageResponse data) {
                // Reload to get updated state
                loadActiveRide();
            }

            @Override
            public void onError(String message) {
                DriverHomeViewState s = DriverHomeViewState.withActiveRide(current.activeRide);
                s.error = true;
                s.errorMessage = message;
                state.postValue(s);
            }
        });
    }

    public void startRide() {
        // Placeholder - not implemented yet
        // Will call rideRepository.startRide() when backend endpoint is available
    }

    public void clearError() {
        DriverHomeViewState current = state.getValue();
        if (current != null && current.error) {
            if (current.activeRide != null) {
                state.setValue(DriverHomeViewState.withActiveRide(current.activeRide));
            } else {
                state.setValue(DriverHomeViewState.idle());
            }
        }
    }

    // Helper methods for formatting
    public static String formatPrice(Number price) {
        if (price == null) return "";
        return new DecimalFormat("#,##0.00").format(price) + " RSD";
    }

    public static String formatDistance(Double km) {
        if (km == null) return "";
        return new DecimalFormat("#.##").format(km) + " km";
    }

    public static String formatDuration(Integer min) {
        if (min == null) return "";
        return min + " min";
    }

    public static String getStatusLabel(String status) {
        if (status == null) return "";
        switch (status) {
            case "ACCEPTED":
                return "Assigned";
            case "SCHEDULED":
                return "Scheduled";
            case "IN_PROGRESS":
                return "In Progress";
            case "STOP_REQUESTED":
                return "Stop Requested";
            case "COMPLETED":
                return "Completed";
            case "CANCELLED":
                return "Cancelled";
            default:
                return status;
        }
    }

    public static boolean canStartRide(String status) {
        return "ACCEPTED".equals(status) || "SCHEDULED".equals(status);
    }

    public static boolean canCancelRide(String status) {
        return !"COMPLETED".equals(status) && !"CANCELLED".equals(status);
    }
}
