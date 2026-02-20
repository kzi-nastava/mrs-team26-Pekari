package com.example.blackcar.data.repository;

import androidx.annotation.NonNull;

import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.data.api.model.OnlineDriverWithVehicleResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.util.Log;

public class DriversRepository {

    private static final String TAG = "DriversRepository";

    public interface ResultCallback {
        void onSuccess(List<OnlineDriverWithVehicleResponse> data);
        void onError(String message);
    }

    public void fetchOnlineWithVehicles(int page, int size, @NonNull ResultCallback callback) {
        Log.d(TAG, "Fetching online drivers with vehicles: page=" + page + ", size=" + size);
        ApiClient.getDriversService().getOnlineDriversWithVehicles(page, size)
                .enqueue(new Callback<List<OnlineDriverWithVehicleResponse>>() {
                    @Override
                    public void onResponse(Call<List<OnlineDriverWithVehicleResponse>> call, Response<List<OnlineDriverWithVehicleResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "Successfully fetched " + response.body().size() + " drivers");
                            callback.onSuccess(response.body());
                        } else {
                            Log.e(TAG, "Failed to fetch drivers: HTTP " + response.code());
                            callback.onError("HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<OnlineDriverWithVehicleResponse>> call, Throwable t) {
                        Log.e(TAG, "Error fetching drivers: " + t.getMessage(), t);
                        callback.onError(t.getMessage());
                    }
                });
    }
}
