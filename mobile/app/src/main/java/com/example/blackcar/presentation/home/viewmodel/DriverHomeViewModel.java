package com.example.blackcar.presentation.home.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.data.api.model.ActiveRideResponse;
import com.example.blackcar.data.api.model.LocationPoint;
import com.example.blackcar.data.api.model.MessageResponse;
import com.example.blackcar.data.api.model.RideLocationUpdateRequest;
import com.example.blackcar.data.repository.RideRepository;
import com.example.blackcar.presentation.home.viewstate.DriverHomeViewState;

import java.text.DecimalFormat;

public class DriverHomeViewModel extends ViewModel {

    private static final long POLL_INTERVAL_MS = 5000; // 5 seconds

    private final RideRepository rideRepository = new RideRepository();
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private boolean isPolling = false;

    private final MutableLiveData<DriverHomeViewState> state = new MutableLiveData<>(DriverHomeViewState.loading());

    public LiveData<DriverHomeViewState> getState() {
        return state;
    }

    public void loadActiveRide() {
        state.setValue(DriverHomeViewState.loading());

        rideRepository.getActiveRideForDriver(new RideRepository.RepoCallback<ActiveRideResponse>() {
            @Override
            public void onSuccess(ActiveRideResponse data) {
                DriverHomeViewState current = state.getValue();
                boolean wasPanicActivated = current != null && current.panicActivated;

                if (data != null) {
                    DriverHomeViewState newState = DriverHomeViewState.withActiveRide(data);
                    newState.panicActivated = wasPanicActivated;
                    state.postValue(newState);
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

    public void startRide() {
        DriverHomeViewState current = state.getValue();
        if (current == null || current.activeRide == null) return;

        Long rideId = current.activeRide.getRideId();
        state.setValue(DriverHomeViewState.actionInProgress(current.activeRide));

        rideRepository.startRide(rideId, new RideRepository.RepoCallback<MessageResponse>() {
            @Override
            public void onSuccess(MessageResponse data) {
                // Immediately send a location update to trigger passenger refresh via WebSocket
                if (current.currentLocation != null) {
                    updateCurrentLocation(current.currentLocation);
                } else if (current.activeRide.getPickup() != null) {
                    updateCurrentLocation(current.activeRide.getPickup());
                }
                
                // Reload to get updated status
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

    public void completeRide() {
        DriverHomeViewState current = state.getValue();
        if (current == null || current.activeRide == null) return;

        Long rideId = current.activeRide.getRideId();
        state.setValue(DriverHomeViewState.actionInProgress(current.activeRide));

        rideRepository.completeRide(rideId, new RideRepository.RepoCallback<MessageResponse>() {
            @Override
            public void onSuccess(MessageResponse data) {
                // Reload to check for new rides
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

    public void stopRideEarly() {
        DriverHomeViewState current = state.getValue();
        if (current == null || current.activeRide == null) return;

        LocationPoint currentLocation = current.currentLocation;
        if (currentLocation == null) {
            // Use pickup location as fallback
            currentLocation = current.activeRide.getPickup();
        }

        if (currentLocation == null) {
            DriverHomeViewState s = DriverHomeViewState.withActiveRide(current.activeRide);
            s.error = true;
            s.errorMessage = "Current location not available. Please try again.";
            state.postValue(s);
            return;
        }

        Long rideId = current.activeRide.getRideId();
        state.setValue(DriverHomeViewState.actionInProgress(current.activeRide));

        rideRepository.stopRideEarly(rideId, currentLocation, new RideRepository.RepoCallback<MessageResponse>() {
            @Override
            public void onSuccess(MessageResponse data) {
                // Reload to check for new rides
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

    public void activatePanic() {
        DriverHomeViewState current = state.getValue();
        if (current == null || current.activeRide == null) return;

        Long rideId = current.activeRide.getRideId();
        state.setValue(DriverHomeViewState.actionInProgress(current.activeRide));

        rideRepository.activatePanic(rideId, new RideRepository.RepoCallback<MessageResponse>() {
            @Override
            public void onSuccess(MessageResponse data) {
                DriverHomeViewState s = DriverHomeViewState.withActiveRide(current.activeRide);
                s.panicActivated = true;
                state.postValue(s);
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

    public void updateCurrentLocation(LocationPoint location) {
        DriverHomeViewState current = state.getValue();
        if (current != null) {
            current.currentLocation = location;
            state.setValue(current);

            // Send to backend if ride is ACCEPTED, IN_PROGRESS or STOP_REQUESTED
            if (current.activeRide != null) {
                String status = current.activeRide.getStatus();
                if ("ACCEPTED".equals(status) || "IN_PROGRESS".equals(status) || "STOP_REQUESTED".equals(status)) {
                    RideLocationUpdateRequest request = new RideLocationUpdateRequest();
                    request.setLatitude(location.getLatitude());
                    request.setLongitude(location.getLongitude());

                    rideRepository.updateRideLocation(current.activeRide.getRideId(), request, new RideRepository.RepoCallback<MessageResponse>() {
                        @Override
                        public void onSuccess(MessageResponse data) {
                            // Silent update
                        }

                        @Override
                        public void onError(String message) {
                            // Silent failure
                        }
                    });
                }
            }
        }
    }

    public void startPolling() {
        if (isPolling) return;
        isPolling = true;

        pollRunnable = new Runnable() {
            @Override
            public void run() {
                DriverHomeViewState current = state.getValue();
                if (current != null && current.activeRide != null) {
                    String status = current.activeRide.getStatus();
                    // Also simulate location updates
                    if ("ACCEPTED".equals(status) || "IN_PROGRESS".equals(status) || "STOP_REQUESTED".equals(status)) {
                        pollForUpdates();
                        simulateLocationUpdate();
                    }
                }
                if (isPolling) {
                    pollHandler.postDelayed(this, POLL_INTERVAL_MS);
                }
            }
        };
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private double lastSimLat = 45.2671;
    private double lastSimLon = 19.8335;

    private void simulateLocationUpdate() {
        // Jitter movement to simulate driving
        lastSimLat += (Math.random() - 0.5) * 0.0005;
        lastSimLon += (Math.random() - 0.5) * 0.0005;
        
        LocationPoint lp = new LocationPoint("Current Location", lastSimLat, lastSimLon);
        updateCurrentLocation(lp);
    }

    public void stopPolling() {
        isPolling = false;
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }

    private void pollForUpdates() {
        rideRepository.getActiveRideForDriver(new RideRepository.RepoCallback<ActiveRideResponse>() {
            @Override
            public void onSuccess(ActiveRideResponse data) {
                if (data != null) {
                    DriverHomeViewState current = state.getValue();
                    boolean wasPanicActivated = current != null && current.panicActivated;
                    LocationPoint currentLoc = current != null ? current.currentLocation : null;

                    DriverHomeViewState newState = DriverHomeViewState.withActiveRide(data);
                    newState.panicActivated = wasPanicActivated;
                    newState.currentLocation = currentLoc;
                    state.postValue(newState);
                }
            }

            @Override
            public void onError(String message) {
                // Silently fail polling - don't show error to user
            }
        });
    }

    public void clearError() {
        DriverHomeViewState current = state.getValue();
        if (current != null && current.error) {
            if (current.activeRide != null) {
                DriverHomeViewState s = DriverHomeViewState.withActiveRide(current.activeRide);
                s.panicActivated = current.panicActivated;
                state.setValue(s);
            } else {
                state.setValue(DriverHomeViewState.idle());
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopPolling();
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

    public static boolean canCompleteRide(String status) {
        return "IN_PROGRESS".equals(status) || "STOP_REQUESTED".equals(status);
    }

    public static boolean canActivatePanic(String status) {
        return "IN_PROGRESS".equals(status) || "STOP_REQUESTED".equals(status);
    }

    public static boolean canCancelRide(String status) {
        return !"COMPLETED".equals(status) && !"CANCELLED".equals(status);
    }

    public static boolean isStopRequested(String status) {
        return "STOP_REQUESTED".equals(status);
    }
}
