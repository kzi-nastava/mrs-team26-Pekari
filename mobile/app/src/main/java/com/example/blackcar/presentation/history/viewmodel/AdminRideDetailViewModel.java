package com.example.blackcar.presentation.history.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.blackcar.data.api.ChatRealtimeService;
import com.example.blackcar.data.api.model.AdminRideDetailResponse;
import com.example.blackcar.data.api.model.WebRideTrackingResponse;
import com.example.blackcar.data.repository.RideRepository;
import com.example.blackcar.presentation.history.viewstate.AdminRideDetailViewState;

public class AdminRideDetailViewModel extends AndroidViewModel {

    private static final String TAG = "AdminRideDetailVM";
    private final MutableLiveData<AdminRideDetailViewState> state = new MutableLiveData<>();
    private final MutableLiveData<WebRideTrackingResponse> trackingState = new MutableLiveData<>();
    private final RideRepository rideRepository;
    private final ChatRealtimeService realtimeService;
    private Long currentRideId;

    public AdminRideDetailViewModel(@NonNull Application application) {
        super(application);
        this.rideRepository = new RideRepository();
        this.realtimeService = new ChatRealtimeService(application);
        this.realtimeService.connect();
    }

    public LiveData<AdminRideDetailViewState> getState() {
        return state;
    }

    public LiveData<WebRideTrackingResponse> getTrackingState() {
        return trackingState;
    }

    public void loadRideDetail(Long rideId) {
        this.currentRideId = rideId;
        state.setValue(new AdminRideDetailViewState(true, false, null, null));

        rideRepository.getAdminRideDetail(rideId, new RideRepository.RepoCallback<AdminRideDetailResponse>() {
            @Override
            public void onSuccess(AdminRideDetailResponse data) {
                state.setValue(new AdminRideDetailViewState(false, false, null, data));
                
                // If ride is active, subscribe to tracking
                if (isActiveStatus(data.getStatus())) {
                    subscribeToTracking(rideId);
                }
            }

            @Override
            public void onError(String message) {
                state.setValue(new AdminRideDetailViewState(false, true, message, null));
            }
        });
    }

    private boolean isActiveStatus(String status) {
        if (status == null) return false;
        return status.equals("ACCEPTED") || status.equals("IN_PROGRESS") || 
               status.equals("STOP_REQUESTED") || status.equals("SCHEDULED");
    }

    private void subscribeToTracking(Long rideId) {
        Log.d(TAG, "Subscribing to tracking for ride: " + rideId);
        realtimeService.subscribeToRideTracking(rideId, tracking -> {
            Log.d(TAG, "Received tracking update for ride: " + rideId);
            trackingState.postValue(tracking);
            
            // If ride completed or cancelled, we might want to refresh details once
            String status = tracking.getRideStatus() != null ? tracking.getRideStatus() : tracking.getStatus();
            if ("COMPLETED".equals(status) || "CANCELLED".equals(status) || "REJECTED".equals(status)) {
                loadRideDetail(rideId);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        realtimeService.disconnect();
    }
}
