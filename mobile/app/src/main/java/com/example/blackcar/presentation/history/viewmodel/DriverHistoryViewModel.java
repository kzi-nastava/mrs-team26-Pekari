package com.example.blackcar.presentation.history.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.presentation.history.viewstate.DriverHistoryViewState;
import com.example.blackcar.presentation.history.viewstate.RideUIModel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DriverHistoryViewModel extends ViewModel {

    private final MutableLiveData<DriverHistoryViewState> state = new MutableLiveData<>();
    private final List<RideUIModel> allRides;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public DriverHistoryViewModel() {
        this.allRides = MockRideDataHelper.generateMockRides();
    }

    public LiveData<DriverHistoryViewState> getState() {
        return state;
    }


    public void loadHistory(LocalDate from, LocalDate to) {
        state.setValue(new DriverHistoryViewState(true, false, null, new ArrayList<>()));

        try {
            List<RideUIModel> filteredRides = filterRidesByDate(allRides, from, to);

            state.setValue(new DriverHistoryViewState(
                    false,
                    false,
                    null,
                    filteredRides
            ));
        } catch (Exception e) {
            state.setValue(new DriverHistoryViewState(
                    false,
                    true,
                    "Failed to load ride history: " + e.getMessage(),
                    new ArrayList<>()
            ));
        }
    }


    private List<RideUIModel> filterRidesByDate(List<RideUIModel> rides, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return new ArrayList<>(rides);
        }

        return rides.stream()
                .filter(ride -> isRideInDateRange(ride, from, to))
                .collect(Collectors.toList());
    }


    private boolean isRideInDateRange(RideUIModel ride, LocalDate from, LocalDate to) {
        try {
            LocalDateTime rideDateTime = LocalDateTime.parse(ride.startTime, DATE_TIME_FORMATTER);
            LocalDate rideDate = rideDateTime.toLocalDate();

            boolean afterOrEqualFrom = (from == null) || !rideDate.isBefore(from);
            boolean beforeOrEqualTo = (to == null) || !rideDate.isAfter(to);

            return afterOrEqualFrom && beforeOrEqualTo;
        } catch (DateTimeParseException e) {
            return false;
        }
    }


    public void clearFilter() {
        loadHistory(null, null);
    }
}
