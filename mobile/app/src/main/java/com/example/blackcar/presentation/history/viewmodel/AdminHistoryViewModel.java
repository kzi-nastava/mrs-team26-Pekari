package com.example.blackcar.presentation.history.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.AdminRideHistoryFilter;
import com.example.blackcar.data.api.model.AdminRideHistoryResponse;
import com.example.blackcar.data.api.model.PaginatedResponse;
import com.example.blackcar.data.repository.RideRepository;
import com.example.blackcar.presentation.history.viewstate.AdminHistoryViewState;
import com.example.blackcar.presentation.history.viewstate.AdminRideUIModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminHistoryViewModel extends ViewModel {

    private final MutableLiveData<AdminHistoryViewState> state = new MutableLiveData<>();
    private final RideRepository rideRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String currentSortField = "createdAt";
    private boolean currentSortAscending = false;  // default: newest first
    private List<AdminRideUIModel> currentRides = new ArrayList<>();

    public AdminHistoryViewModel() {
        this.rideRepository = new RideRepository();
    }

    public LiveData<AdminHistoryViewState> getState() {
        return state;
    }

    public void loadHistory(LocalDate from, LocalDate to) {
        state.setValue(new AdminHistoryViewState(true, false, null, new ArrayList<>()));

        // Backend expects LocalDateTime format (yyyy-MM-dd'T'HH:mm:ss)
        String startDate = (from != null) ? from.format(DATE_FORMATTER) + "T00:00:00" : null;
        String endDate = (to != null) ? to.format(DATE_FORMATTER) + "T23:59:59" : null;

        AdminRideHistoryFilter filter = new AdminRideHistoryFilter(startDate, endDate);

        rideRepository.getAdminRideHistory(filter, 0, 100,
                new RideRepository.RepoCallback<PaginatedResponse<AdminRideHistoryResponse>>() {
                    @Override
                    public void onSuccess(PaginatedResponse<AdminRideHistoryResponse> data) {
                        List<AdminRideUIModel> uiModels = mapToUIModels(data.getContent());
                        currentRides = uiModels;
                        sortRides(currentSortField, currentSortAscending);
                    }

                    @Override
                    public void onError(String message) {
                        state.setValue(new AdminHistoryViewState(
                                false,
                                true,
                                message,
                                new ArrayList<>()
                        ));
                    }
                });
    }

    private List<AdminRideUIModel> mapToUIModels(List<AdminRideHistoryResponse> responses) {
        if (responses == null) {
            return new ArrayList<>();
        }

        return responses.stream()
                .map(this::mapToUIModel)
                .collect(Collectors.toList());
    }

    private AdminRideUIModel mapToUIModel(AdminRideHistoryResponse response) {
        AdminRideUIModel model = new AdminRideUIModel();
        model.id = response.getId();
        model.status = response.getStatus();
        model.createdAt = response.getCreatedAt();
        model.startedAt = response.getStartedAt();
        model.completedAt = response.getCompletedAt();
        model.pickupAddress = response.getPickupAddress();
        model.dropoffAddress = response.getDropoffAddress();
        model.price = response.getPrice() != null ? response.getPrice().doubleValue() : 0.0;
        model.distanceKm = response.getDistanceKm();
        model.vehicleType = response.getVehicleType();
        model.cancelled = response.getCancelled() != null && response.getCancelled();
        model.cancelledBy = response.getCancelledBy();
        model.panicActivated = response.getPanicActivated() != null && response.getPanicActivated();
        model.panickedBy = response.getPanickedBy();

        // Driver info
        if (response.getDriver() != null) {
            model.driverName = response.getDriver().getFirstName() + " " + response.getDriver().getLastName();
            model.driverEmail = response.getDriver().getEmail();
        } else {
            model.driverName = "No driver";
            model.driverEmail = null;
        }

        // Passenger count
        model.passengerCount = response.getPassengers() != null ? response.getPassengers().size() : 0;

        return model;
    }

    public void sortRides(String sortField, boolean ascending) {
        this.currentSortField = sortField;
        this.currentSortAscending = ascending;

        if (currentRides == null || currentRides.isEmpty()) {
            return;
        }

        List<AdminRideUIModel> sortedRides = new ArrayList<>(currentRides);

        sortedRides.sort((a, b) -> {
            int comparison = 0;

            switch (sortField) {
                case "createdAt":
                    comparison = compareStrings(a.createdAt, b.createdAt);
                    break;
                case "startedAt":
                    comparison = compareStrings(a.startedAt, b.startedAt);
                    break;
                case "completedAt":
                    comparison = compareStrings(a.completedAt, b.completedAt);
                    break;
                case "price":
                    comparison = Double.compare(a.price, b.price);
                    break;
                case "distanceKm":
                    double distA = a.distanceKm != null ? a.distanceKm : 0.0;
                    double distB = b.distanceKm != null ? b.distanceKm : 0.0;
                    comparison = Double.compare(distA, distB);
                    break;
                case "status":
                    comparison = compareStrings(a.status, b.status);
                    break;
                case "pickup":
                    comparison = compareStrings(a.pickupAddress, b.pickupAddress);
                    break;
                case "dropoff":
                    comparison = compareStrings(a.dropoffAddress, b.dropoffAddress);
                    break;
            }

            return ascending ? comparison : -comparison;
        });

        state.setValue(new AdminHistoryViewState(
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
        sortRides("createdAt", currentSortAscending);
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
