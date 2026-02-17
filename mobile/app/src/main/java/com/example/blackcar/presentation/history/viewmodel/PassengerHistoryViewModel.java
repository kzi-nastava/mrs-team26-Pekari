package com.example.blackcar.presentation.history.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.PaginatedResponse;
import com.example.blackcar.data.api.model.PassengerRideHistoryResponse;
import com.example.blackcar.data.api.model.RideHistoryFilterRequest;
import com.example.blackcar.data.repository.RideRepository;
import com.example.blackcar.presentation.history.viewstate.PassengerHistoryViewState;
import com.example.blackcar.presentation.history.viewstate.RideUIModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PassengerHistoryViewModel extends ViewModel {

    private final MutableLiveData<PassengerHistoryViewState> state = new MutableLiveData<>();
    private final RideRepository rideRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String currentSortField = "date";
    private boolean currentSortAscending = false;  // default: newest first
    private List<RideUIModel> currentRides = new ArrayList<>();

    public PassengerHistoryViewModel() {
        this.rideRepository = new RideRepository();
    }

    public LiveData<PassengerHistoryViewState> getState() {
        return state;
    }


    public void loadHistory(LocalDate from, LocalDate to) {
        state.setValue(new PassengerHistoryViewState(true, false, null, new ArrayList<>()));

        // Backend expects LocalDateTime format (yyyy-MM-dd'T'HH:mm:ss)
        // Start of day for start date, end of day for end date
        String startDate = (from != null) ? from.format(DATE_FORMATTER) + "T00:00:00" : null;
        String endDate = (to != null) ? to.format(DATE_FORMATTER) + "T23:59:59" : null;

        RideHistoryFilterRequest filter = new RideHistoryFilterRequest(startDate, endDate);

        rideRepository.getPassengerRideHistory(filter, 0, 100,
                new RideRepository.RepoCallback<PaginatedResponse<PassengerRideHistoryResponse>>() {
                    @Override
                    public void onSuccess(PaginatedResponse<PassengerRideHistoryResponse> data) {
                        List<RideUIModel> uiModels = mapToUIModels(data.getContent());
                        currentRides = uiModels;
                        sortRides(currentSortField, currentSortAscending);
                    }

                    @Override
                    public void onError(String message) {
                        state.setValue(new PassengerHistoryViewState(
                                false,
                                true,
                                message,
                                new ArrayList<>()
                        ));
                    }
                });
    }


    private List<RideUIModel> mapToUIModels(List<PassengerRideHistoryResponse> responses) {
        if (responses == null) {
            return new ArrayList<>();
        }

        return responses.stream()
                .map(this::mapToUIModel)
                .collect(Collectors.toList());
    }

    private RideUIModel mapToUIModel(PassengerRideHistoryResponse response) {
        RideUIModel model = new RideUIModel();
        model.id = response.getId();
        model.startTime = response.getStartTime();
        model.endTime = response.getEndTime();
        model.origin = response.getPickupLocation();
        model.destination = response.getDropoffLocation();
        model.canceledBy = (response.getCancelled() != null && response.getCancelled())
                ? response.getCancelledBy()
                : null;
        model.panic = response.getPanicActivated() != null && response.getPanicActivated();
        model.price = response.getPrice() != null ? response.getPrice().doubleValue() : 0.0;
        model.distanceKm = response.getDistanceKm();
        model.vehicleType = response.getVehicleType();
        model.status = response.getStatus();

        // Set driver name for passenger view
        if (response.getDriver() != null) {
            model.driverName = response.getDriver().getFirstName() + " " + response.getDriver().getLastName();
        }

        // Passengers list not needed for passenger history, but initialize to avoid null
        model.passengers = new ArrayList<>();

        return model;
    }

    public void sortRides(String sortField, boolean ascending) {
        this.currentSortField = sortField;
        this.currentSortAscending = ascending;

        if (currentRides == null || currentRides.isEmpty()) {
            return;
        }

        List<RideUIModel> sortedRides = new ArrayList<>(currentRides);

        sortedRides.sort((a, b) -> {
            int comparison = 0;

            switch (sortField) {
                case "date":
                    comparison = compareStrings(a.startTime, b.startTime);
                    break;
                case "price":
                    comparison = Double.compare(a.price, b.price);
                    break;
                case "distance":
                    double distA = a.distanceKm != null ? a.distanceKm : 0.0;
                    double distB = b.distanceKm != null ? b.distanceKm : 0.0;
                    comparison = Double.compare(distA, distB);
                    break;
                case "vehicleType":
                    comparison = compareStrings(a.vehicleType, b.vehicleType);
                    break;
                case "status":
                    comparison = compareStrings(a.status, b.status);
                    break;
                case "pickup":
                    comparison = compareStrings(a.origin, b.origin);
                    break;
                case "dropoff":
                    comparison = compareStrings(a.destination, b.destination);
                    break;
            }

            return ascending ? comparison : -comparison;
        });

        state.setValue(new PassengerHistoryViewState(
                false,
                false,
                null,
                sortedRides,
                currentSortField,
                currentSortAscending
        ));
    }

    public void toggleDateSort() {
        // Toggle between ascending and descending for date sorting
        currentSortAscending = !currentSortAscending;
        sortRides("date", currentSortAscending);
    }

    private int compareStrings(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    public void clearFilter() {
        loadHistory(null, null);
    }
}
