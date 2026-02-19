package com.example.blackcar.presentation.stats.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.RideStatsResponse;
import com.example.blackcar.data.repository.RideRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PassengerStatsViewModel extends ViewModel {

    private final RideRepository repository = new RideRepository();

    private final MutableLiveData<PassengerStatsViewState> state = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public LiveData<PassengerStatsViewState> getState() {
        return state;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void loadStats(String dateFrom, String dateTo) {
        state.setValue(new PassengerStatsViewState(true, null, null));
        repository.getPassengerRideStats(dateFrom, dateTo, new RideRepository.RepoCallback<RideStatsResponse>() {
            @Override
            public void onSuccess(RideStatsResponse data) {
                state.setValue(new PassengerStatsViewState(false, data, null));
            }

            @Override
            public void onError(String message) {
                state.setValue(new PassengerStatsViewState(false, null, message));
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

    public static class PassengerStatsViewState {
        public final boolean loading;
        public final RideStatsResponse stats;
        public final String error;

        public PassengerStatsViewState(boolean loading, RideStatsResponse stats, String error) {
            this.loading = loading;
            this.stats = stats;
            this.error = error;
        }
    }
}
