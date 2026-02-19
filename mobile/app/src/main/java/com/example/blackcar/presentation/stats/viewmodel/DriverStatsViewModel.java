package com.example.blackcar.presentation.stats.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.RideStatsResponse;
import com.example.blackcar.data.repository.RideRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DriverStatsViewModel extends ViewModel {

    private final RideRepository repository = new RideRepository();

    private final MutableLiveData<DriverStatsViewState> state = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public LiveData<DriverStatsViewState> getState() {
        return state;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void loadStats(String dateFrom, String dateTo) {
        state.setValue(new DriverStatsViewState(true, null, null));
        repository.getDriverRideStats(dateFrom, dateTo, new RideRepository.RepoCallback<RideStatsResponse>() {
            @Override
            public void onSuccess(RideStatsResponse data) {
                state.setValue(new DriverStatsViewState(false, data, null));
            }

            @Override
            public void onError(String message) {
                state.setValue(new DriverStatsViewState(false, null, message));
            }
        });
    }

    public void clearToastMessage() {
        toastMessage.setValue(null);
    }

    public static String getDefaultDateFrom() {
        return "2024-01-01";
    }

    public static String getDefaultDateTo() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static class DriverStatsViewState {
        public final boolean loading;
        public final RideStatsResponse stats;
        public final String error;

        public DriverStatsViewState(boolean loading, RideStatsResponse stats, String error) {
            this.loading = loading;
            this.stats = stats;
            this.error = error;
        }
    }
}
