package com.example.blackcar.presentation.admin.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.DriverRideHistoryResponse;
import com.example.blackcar.data.repository.RideRepository;
import com.example.blackcar.presentation.admin.viewstate.PanicPanelViewState;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PanicPanelViewModel extends ViewModel {

    private static final long POLL_INTERVAL_MS = 3000; // 3 seconds

    private final RideRepository rideRepository;
    private final MutableLiveData<PanicPanelViewState> state = new MutableLiveData<>(PanicPanelViewState.Idle.getInstance());

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private boolean isPolling = false;
    private int previousCount = 0;

    // LiveData for new panic alert notification
    private final MutableLiveData<Integer> newPanicAlert = new MutableLiveData<>();

    public PanicPanelViewModel() {
        this.rideRepository = new RideRepository();
    }

    public LiveData<PanicPanelViewState> getState() {
        return state;
    }

    public LiveData<Integer> getNewPanicAlert() {
        return newPanicAlert;
    }

    public void loadPanicRides() {
        state.setValue(PanicPanelViewState.Loading.getInstance());
        fetchPanicRides(true);
    }

    public void refreshPanicRides() {
        fetchPanicRides(false);
    }

    private void fetchPanicRides(boolean isInitialLoad) {
        rideRepository.getActivePanicRides(new RideRepository.RepoCallback<List<DriverRideHistoryResponse>>() {
            @Override
            public void onSuccess(List<DriverRideHistoryResponse> data) {
                int newCount = data != null ? data.size() : 0;

                // Notify about new panic rides (not on initial load)
                if (!isInitialLoad && newCount > previousCount) {
                    newPanicAlert.setValue(newCount - previousCount);
                }
                previousCount = newCount;

                state.setValue(new PanicPanelViewState.Success(data));
            }

            @Override
            public void onError(String message) {
                state.setValue(new PanicPanelViewState.Error(message));
            }
        });
    }

    public void startPolling() {
        if (isPolling) return;
        isPolling = true;
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    public void stopPolling() {
        isPolling = false;
        pollHandler.removeCallbacks(pollRunnable);
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPolling) return;
            fetchPanicRides(false);
            pollHandler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Override
    protected void onCleared() {
        super.onCleared();
        stopPolling();
    }

    // Utility methods for UI

    public static String getStatusLabel(String status) {
        if (status == null) return "";
        switch (status) {
            case "ACCEPTED": return "Assigned";
            case "SCHEDULED": return "Scheduled";
            case "IN_PROGRESS": return "In Progress";
            case "STOP_REQUESTED": return "Stop Requested";
            case "COMPLETED": return "Completed";
            case "CANCELLED": return "Cancelled";
            default: return status;
        }
    }

    public static String formatDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) return "N/A";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(dateTime);
            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            // Try alternative format
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = inputFormat.parse(dateTime);
                if (date != null) {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                    return outputFormat.format(date);
                }
            } catch (Exception ignored) {}
        }
        return dateTime;
    }
}
