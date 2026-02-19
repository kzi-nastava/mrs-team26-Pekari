package com.example.blackcar.presentation.home.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.data.api.ChatRealtimeService;
import com.example.blackcar.data.api.model.InconsistencyReportRequest;
import com.example.blackcar.data.api.model.MessageResponse;
import com.example.blackcar.data.api.model.PassengerRideDetailResponse;
import com.example.blackcar.data.api.model.WebRideTrackingResponse;
import com.example.blackcar.data.repository.RideRepository;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideTrackingViewModel extends AndroidViewModel {
    private static final String TAG = "RideTrackingViewModel";
    private final MutableLiveData<WebRideTrackingResponse> trackingState = new MutableLiveData<>();
    private final MutableLiveData<PassengerRideDetailResponse> rideDetails = new MutableLiveData<>();
    private final MutableLiveData<String> errorState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> rideEnded = new MutableLiveData<>(false);
    private final RideRepository rideRepository = new RideRepository();
    private final ChatRealtimeService realtimeService; // Reuse for STOMP
    private final Gson gson = new Gson();
    private Long rideId;

    public RideTrackingViewModel(@NonNull Application application) {
        super(application);
        this.realtimeService = new ChatRealtimeService(application);
        this.realtimeService.connect();
    }

    public LiveData<WebRideTrackingResponse> getTrackingState() { return trackingState; }
    public LiveData<PassengerRideDetailResponse> getRideDetails() { return rideDetails; }
    public LiveData<String> getErrorState() { return errorState; }
    public LiveData<Boolean> getRideEnded() { return rideEnded; }

    public void startTracking(Long rideId) {
        this.rideId = rideId;
        loadInitialTracking();
        loadRideDetails();
        subscribeToTracking();
    }

    private void loadRideDetails() {
        rideRepository.getPassengerRideDetail(rideId, new RideRepository.RepoCallback<PassengerRideDetailResponse>() {
            @Override
            public void onSuccess(PassengerRideDetailResponse data) {
                rideDetails.postValue(data);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Failed to load ride details: " + message);
            }
        });
    }

    private void loadInitialTracking() {
        ApiClient.getRideService().trackRide(rideId).enqueue(new Callback<WebRideTrackingResponse>() {
            @Override
            public void onResponse(@NonNull Call<WebRideTrackingResponse> call, @NonNull Response<WebRideTrackingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    trackingState.postValue(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<WebRideTrackingResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to load initial tracking", t);
            }
        });
    }

    private void subscribeToTracking() {
        realtimeService.subscribeToRideTracking(rideId, tracking -> {
            trackingState.postValue(tracking);
            
            // Check for COMPLETED or CANCELLED status to stop tracking
            String status = tracking.getRideStatus() != null ? tracking.getRideStatus() : tracking.getStatus();
            if ("COMPLETED".equals(status) || "CANCELLED".equals(status) || "REJECTED".equals(status)) {
                rideEnded.postValue(true);
            }
        });
    }

    public void reportInconsistency(String description) {
        if (rideId == null || description == null || description.trim().isEmpty()) return;

        ApiClient.getRideService().reportInconsistency(rideId, new InconsistencyReportRequest(description.trim()))
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (!response.isSuccessful()) {
                            errorState.postValue("Failed to submit report");
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        errorState.postValue("Network error");
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        realtimeService.disconnect();
    }
}
