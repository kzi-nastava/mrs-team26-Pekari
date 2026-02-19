package com.example.blackcar.presentation.stats.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.DriverBasicInfo;
import com.example.blackcar.data.api.model.PassengerBasicInfo;
import com.example.blackcar.data.api.model.RideStatsResponse;
import com.example.blackcar.data.repository.AdminRepository;
import com.example.blackcar.data.repository.RideRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminStatsViewModel extends ViewModel {

    private final RideRepository rideRepository = new RideRepository();
    private final AdminRepository adminRepository = new AdminRepository();

    private final MutableLiveData<AdminStatsViewState> state = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public LiveData<AdminStatsViewState> getState() {
        return state;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void loadUserLists() {
        adminRepository.getDriversBasic(new AdminRepository.RepoCallback<List<DriverBasicInfo>>() {
            @Override
            public void onSuccess(List<DriverBasicInfo> data) {
                AdminStatsViewState current = state.getValue();
                state.setValue(new AdminStatsViewState(
                        current != null ? current.loading : false,
                        current != null ? current.stats : null,
                        current != null ? current.error : null,
                        data,
                        current != null ? current.passengers : null
                ));
            }

            @Override
            public void onError(String message) {
                // Keep existing drivers, just log
            }
        });
        adminRepository.getPassengersBasic(new AdminRepository.RepoCallback<List<PassengerBasicInfo>>() {
            @Override
            public void onSuccess(List<PassengerBasicInfo> data) {
                AdminStatsViewState current = state.getValue();
                state.setValue(new AdminStatsViewState(
                        current != null ? current.loading : false,
                        current != null ? current.stats : null,
                        current != null ? current.error : null,
                        current != null ? current.drivers : null,
                        data
                ));
            }

            @Override
            public void onError(String message) {
                // Keep existing passengers
            }
        });
    }

    public void loadStats(String dateFrom, String dateTo, String scope, Long userId) {
        state.setValue(new AdminStatsViewState(
                true,
                null,
                null,
                state.getValue() != null ? state.getValue().drivers : null,
                state.getValue() != null ? state.getValue().passengers : null
        ));
        rideRepository.getAdminRideStats(dateFrom, dateTo, scope, userId, new RideRepository.RepoCallback<RideStatsResponse>() {
            @Override
            public void onSuccess(RideStatsResponse data) {
                state.setValue(new AdminStatsViewState(
                        false,
                        data,
                        null,
                        state.getValue() != null ? state.getValue().drivers : null,
                        state.getValue() != null ? state.getValue().passengers : null
                ));
            }

            @Override
            public void onError(String message) {
                state.setValue(new AdminStatsViewState(
                        false,
                        null,
                        message,
                        state.getValue() != null ? state.getValue().drivers : null,
                        state.getValue() != null ? state.getValue().passengers : null
                ));
            }
        });
    }

    public void clearToastMessage() {
        toastMessage.setValue(null);
    }

    public static String getDefaultDateFrom() {
        return "2026-01-01";
    }

    public static String getDefaultDateTo() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static class AdminStatsViewState {
        public final boolean loading;
        public final RideStatsResponse stats;
        public final String error;
        public final List<DriverBasicInfo> drivers;
        public final List<PassengerBasicInfo> passengers;

        public AdminStatsViewState(boolean loading, RideStatsResponse stats, String error,
                                  List<DriverBasicInfo> drivers, List<PassengerBasicInfo> passengers) {
            this.loading = loading;
            this.stats = stats;
            this.error = error;
            this.drivers = drivers;
            this.passengers = passengers;
        }
    }
}
