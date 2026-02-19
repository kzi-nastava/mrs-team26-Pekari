package com.example.blackcar.presentation.history.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.CreateFavoriteRouteRequest;
import com.example.blackcar.data.api.model.FavoriteRouteResponse;
import com.example.blackcar.data.api.model.LocationPoint;
import com.example.blackcar.data.api.model.PaginatedResponse;
import com.example.blackcar.data.api.model.PassengerRideHistoryResponse;
import com.example.blackcar.data.api.model.RideHistoryFilterRequest;
import com.example.blackcar.data.repository.FavoriteRoutesRepository;
import com.example.blackcar.data.repository.RideRepository;
import com.example.blackcar.presentation.history.viewstate.PassengerHistoryViewState;
import com.example.blackcar.presentation.history.viewstate.RideUIModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PassengerHistoryViewModel extends ViewModel {

    private final MutableLiveData<PassengerHistoryViewState> state = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final RideRepository rideRepository;
    private final FavoriteRoutesRepository favoriteRoutesRepository = new FavoriteRoutesRepository();
    private final List<FavoriteRouteResponse> favoriteRoutes = new ArrayList<>();
    private final Set<Long> favoriteRouteIds = new HashSet<>();
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

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }


    public void loadHistory(LocalDate from, LocalDate to) {
        state.setValue(new PassengerHistoryViewState(true, false, null, new ArrayList<>()));
        loadFavoriteRoutes();

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
                        refreshFavoriteStateOnRides();
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
        model.pickup = response.getPickup();
        model.dropoff = response.getDropoff();
        model.stops = response.getStops();
        model.babyTransport = response.getBabyTransport();
        model.petTransport = response.getPetTransport();

        // Set driver name for passenger view
        if (response.getDriver() != null) {
            model.driverName = response.getDriver().getFirstName() + " " + response.getDriver().getLastName();
        }

        // Passengers list not needed for passenger history, but initialize to avoid null
        model.passengers = new ArrayList<>();

        // Check if this ride matches a favorite route
        model.favoriteRouteId = findMatchingFavoriteRouteId(model);

        return model;
    }

    private void loadFavoriteRoutes() {
        favoriteRoutesRepository.getFavoriteRoutes(new FavoriteRoutesRepository.RepoCallback<List<FavoriteRouteResponse>>() {
            @Override
            public void onSuccess(List<FavoriteRouteResponse> data) {
                favoriteRoutes.clear();
                favoriteRouteIds.clear();
                if (data != null) {
                    favoriteRoutes.addAll(data);
                    for (FavoriteRouteResponse r : data) {
                        if (r.getId() != null) favoriteRouteIds.add(r.getId());
                    }
                }
                // Refresh ride list to update favoriteRouteId on each ride
                refreshFavoriteStateOnRides();
            }

            @Override
            public void onError(String message) {
                // Silently fail
            }
        });
    }

    private void refreshFavoriteStateOnRides() {
        if (currentRides == null || currentRides.isEmpty()) return;
        for (RideUIModel ride : currentRides) {
            ride.favoriteRouteId = findMatchingFavoriteRouteId(ride);
        }
        sortRides(currentSortField, currentSortAscending);
    }

    private Long findMatchingFavoriteRouteId(RideUIModel ride) {
        String pickupAddr = ride.pickup != null ? ride.pickup.getAddress() : ride.origin;
        String dropoffAddr = ride.dropoff != null ? ride.dropoff.getAddress() : ride.destination;
        if (pickupAddr == null || dropoffAddr == null) return null;
        for (FavoriteRouteResponse fav : favoriteRoutes) {
            String favPickup = fav.getPickup() != null ? fav.getPickup().getAddress() : null;
            String favDropoff = fav.getDropoff() != null ? fav.getDropoff().getAddress() : null;
            if (pickupAddr.equals(favPickup) && dropoffAddr.equals(favDropoff)) {
                return fav.getId();
            }
        }
        return null;
    }

    public void toggleFavorite(RideUIModel ride) {
        if (ride == null) return;

        FavoriteRouteResponse matching = findMatchingFavoriteRoute(ride);
        if (matching != null) {
            // Remove from favorites
            favoriteRoutesRepository.deleteFavoriteRoute(matching.getId(), new FavoriteRoutesRepository.RepoCallback<com.example.blackcar.data.api.model.MessageResponse>() {
                @Override
                public void onSuccess(com.example.blackcar.data.api.model.MessageResponse data) {
                    favoriteRouteIds.remove(matching.getId());
                    favoriteRoutes.remove(matching);
                    ride.favoriteRouteId = null;
                    refreshFavoriteStateOnRides();
                }

                @Override
                public void onError(String message) {
                    toastMessage.setValue("Failed to remove from favorites");
                    refreshFavoriteStateOnRides();
                }
            });
        } else {
            // Add to favorites - need coordinates
            LocationPoint pickup = ride.pickup;
            LocationPoint dropoff = ride.dropoff;
            if (pickup == null || dropoff == null || pickup.getLatitude() == null || pickup.getLongitude() == null
                    || dropoff.getLatitude() == null || dropoff.getLongitude() == null) {
                toastMessage.setValue("Cannot add to favorites: missing location coordinates");
                return;
            }

            String name = (ride.origin != null ? ride.origin : "") + " → " + (ride.destination != null ? ride.destination : "");
            CreateFavoriteRouteRequest req = new CreateFavoriteRouteRequest();
            req.setName(name);
            req.setPickup(pickup);
            req.setDropoff(dropoff);
            req.setStops(ride.stops);
            req.setVehicleType(ride.vehicleType);
            req.setBabyTransport(ride.babyTransport != null && ride.babyTransport);
            req.setPetTransport(ride.petTransport != null && ride.petTransport);

            favoriteRoutesRepository.createFavoriteRoute(req, new FavoriteRoutesRepository.RepoCallback<FavoriteRouteResponse>() {
                @Override
                public void onSuccess(FavoriteRouteResponse created) {
                    if (created != null && created.getId() != null) {
                        favoriteRoutes.add(created);
                        favoriteRouteIds.add(created.getId());
                        ride.favoriteRouteId = created.getId();
                        refreshFavoriteStateOnRides();
                    }
                }

                @Override
                public void onError(String message) {
                    toastMessage.setValue("Failed to add to favorites");
                    refreshFavoriteStateOnRides();
                }
            });
        }
    }

    private FavoriteRouteResponse findMatchingFavoriteRoute(RideUIModel ride) {
        if (ride.favoriteRouteId != null) {
            for (FavoriteRouteResponse fav : favoriteRoutes) {
                if (ride.favoriteRouteId.equals(fav.getId())) return fav;
            }
        }
        String pickupAddr = ride.pickup != null ? ride.pickup.getAddress() : ride.origin;
        String dropoffAddr = ride.dropoff != null ? ride.dropoff.getAddress() : ride.destination;
        if (pickupAddr == null || dropoffAddr == null) return null;
        for (FavoriteRouteResponse fav : favoriteRoutes) {
            String favPickup = fav.getPickup() != null ? fav.getPickup().getAddress() : null;
            String favDropoff = fav.getDropoff() != null ? fav.getDropoff().getAddress() : null;
            if (pickupAddr.equals(favPickup) && dropoffAddr.equals(favDropoff)) {
                return fav;
            }
        }
        return null;
    }

    public boolean isFavorite(RideUIModel ride) {
        return ride != null && ride.favoriteRouteId != null;
    }

    public boolean canAddToFavorites(RideUIModel ride) {
        if (ride == null) return false;
        LocationPoint pickup = ride.pickup;
        LocationPoint dropoff = ride.dropoff;
        return pickup != null && dropoff != null
                && pickup.getLatitude() != null && pickup.getLongitude() != null
                && dropoff.getLatitude() != null && dropoff.getLongitude() != null;
    }

    public void clearToastMessage() {
        toastMessage.setValue(null);
    }

    public void sortRides(String sortField, boolean ascending) {
        this.currentSortField = sortField;
        this.currentSortAscending = ascending;

        if (currentRides == null || currentRides.isEmpty()) {
            state.setValue(new PassengerHistoryViewState(
                    false,
                    false,
                    null,
                    new ArrayList<>(),
                    currentSortField,
                    currentSortAscending
            ));
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
