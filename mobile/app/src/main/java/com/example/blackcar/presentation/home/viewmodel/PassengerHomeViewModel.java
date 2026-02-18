package com.example.blackcar.presentation.home.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.ActiveRideResponse;
import com.example.blackcar.data.api.model.EstimateRideRequest;
import com.example.blackcar.data.api.model.LocationPoint;
import com.example.blackcar.data.api.model.OrderRideRequest;
import com.example.blackcar.data.api.model.OrderRideResponse;
import com.example.blackcar.data.api.model.RideEstimateResponse;
import com.example.blackcar.data.repository.GeocodingRepository;
import com.example.blackcar.data.repository.RideRepository;
import com.example.blackcar.presentation.home.viewstate.PassengerHomeViewState;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PassengerHomeViewModel extends ViewModel {

    private final RideRepository rideRepository = new RideRepository();
    private final GeocodingRepository geocodingRepository = new GeocodingRepository();

    private final MutableLiveData<PassengerHomeViewState> state = new MutableLiveData<>(PassengerHomeViewState.idle());

    private LocationPoint pickup;
    private LocationPoint dropoff;
    private final List<LocationPoint> stops = new ArrayList<>();
    private String vehicleType = "STANDARD";
    private boolean babyTransport;
    private boolean petTransport;
    private Date scheduledAt;
    private List<String> passengerEmails = new ArrayList<>();

    public LiveData<PassengerHomeViewState> getState() {
        return state;
    }

    public void setPickup(LocationPoint p) {
        pickup = p;
        clearEstimate();
    }

    public void setDropoff(LocationPoint p) {
        dropoff = p;
        clearEstimate();
    }

    public void setStops(List<LocationPoint> s) {
        stops.clear();
        if (s != null) stops.addAll(s);
        clearEstimate();
    }

    public void setVehicleType(String vt) {
        vehicleType = vt != null ? vt : "STANDARD";
        clearEstimate();
    }

    public void setBabyTransport(boolean b) { babyTransport = b; }
    public void setPetTransport(boolean p) { petTransport = p; }
    public void setScheduledAt(Date d) { scheduledAt = d; }
    public void setPassengerEmails(List<String> e) {
        passengerEmails = e != null ? e : new ArrayList<>();
    }

    public LocationPoint getPickup() { return pickup; }
    public LocationPoint getDropoff() { return dropoff; }
    public List<LocationPoint> getStops() { return stops; }
    public String getVehicleType() { return vehicleType; }
    public boolean isBabyTransport() { return babyTransport; }
    public boolean isPetTransport() { return petTransport; }
    public Date getScheduledAt() { return scheduledAt; }

    private void clearEstimate() {
        PassengerHomeViewState s = state.getValue();
        if (s != null && s.estimate != null) {
            PassengerHomeViewState next = PassengerHomeViewState.idle();
            next.orderResult = s.orderResult;
            next.activeRide = s.activeRide;
            next.formDisabled = s.formDisabled;
            state.setValue(next);
        }
    }

    public void loadActiveRide() {
        rideRepository.getActiveRideForPassenger(new RideRepository.RepoCallback<ActiveRideResponse>() {
            @Override
            public void onSuccess(ActiveRideResponse data) {
                if (data != null) {
                    state.postValue(PassengerHomeViewState.withActiveRide(data));
                } else {
                    state.postValue(PassengerHomeViewState.idle());
                }
            }

            @Override
            public void onError(String message) {
                state.postValue(PassengerHomeViewState.idle());
            }
        });
    }

    public void estimateRide() {
        if (pickup == null || dropoff == null) {
            state.setValue(PassengerHomeViewState.error("Please enter pickup and destination"));
            return;
        }
        if (pickup.getLatitude() == null || pickup.getLongitude() == null
                || dropoff.getLatitude() == null || dropoff.getLongitude() == null) {
            state.setValue(PassengerHomeViewState.error("Please select a valid address from suggestions"));
            return;
        }

        state.setValue(PassengerHomeViewState.loading());

        EstimateRideRequest req = new EstimateRideRequest();
        req.setPickup(pickup);
        req.setDropoff(dropoff);
        req.setStops(stops.isEmpty() ? null : new ArrayList<>(stops));
        req.setVehicleType(vehicleType);

        rideRepository.estimateRide(req, new RideRepository.RepoCallback<RideEstimateResponse>() {
            @Override
            public void onSuccess(RideEstimateResponse data) {
                state.postValue(PassengerHomeViewState.withEstimate(data));
            }

            @Override
            public void onError(String message) {
                state.postValue(PassengerHomeViewState.error(message));
            }
        });
    }

    public void orderRide() {
        if (pickup == null || dropoff == null) {
            state.setValue(PassengerHomeViewState.error("Please enter pickup and destination"));
            return;
        }
        if (pickup.getLatitude() == null || pickup.getLongitude() == null
                || dropoff.getLatitude() == null || dropoff.getLongitude() == null) {
            state.setValue(PassengerHomeViewState.error("Please select a valid address from suggestions"));
            return;
        }

        String scheduledAtStr = null;
        if (scheduledAt != null) {
            long now = System.currentTimeMillis();
            long max = now + 5 * 60 * 60 * 1000;
            if (scheduledAt.getTime() < now + 60_000) {
                scheduledAt = null;
            } else if (scheduledAt.getTime() > max) {
                state.setValue(PassengerHomeViewState.error("Scheduled time can be at most 5 hours in advance"));
                return;
            } else {
                scheduledAtStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(scheduledAt);
            }
        }

        state.setValue(PassengerHomeViewState.loading());

        OrderRideRequest req = new OrderRideRequest();
        req.setPickup(pickup);
        req.setDropoff(dropoff);
        req.setStops(stops.isEmpty() ? null : new ArrayList<>(stops));
        req.setPassengerEmails(passengerEmails.isEmpty() ? null : passengerEmails);
        req.setVehicleType(vehicleType);
        req.setBabyTransport(babyTransport);
        req.setPetTransport(petTransport);
        req.setScheduledAt(scheduledAtStr);

        rideRepository.orderRide(req, new RideRepository.RepoCallback<OrderRideResponse>() {
            @Override
            public void onSuccess(OrderRideResponse data) {
                boolean formDisabled = "ACCEPTED".equals(data.getStatus()) || "SCHEDULED".equals(data.getStatus());
                state.postValue(PassengerHomeViewState.withOrderResult(data, formDisabled));
            }

            @Override
            public void onError(String message) {
                state.postValue(PassengerHomeViewState.error(message));
            }
        });
    }

    public void cancelRide(Long rideId) {
        state.setValue(PassengerHomeViewState.loading());
        rideRepository.cancelRide(rideId, "Cancelled by passenger", new RideRepository.RepoCallback<com.example.blackcar.data.api.model.MessageResponse>() {
            @Override
            public void onSuccess(com.example.blackcar.data.api.model.MessageResponse data) {
                PassengerHomeViewState s = PassengerHomeViewState.idle();
                if (state.getValue() != null && state.getValue().orderResult != null) {
                    OrderRideResponse o = new OrderRideResponse();
                    o.setRideId(state.getValue().orderResult.getRideId());
                    o.setStatus("CANCELLED");
                    o.setMessage(data != null ? data.getMessage() : "Ride cancelled");
                    s.orderResult = o;
                }
                state.postValue(s);
            }

            @Override
            public void onError(String message) {
                state.postValue(PassengerHomeViewState.error(message));
            }
        });
    }

    public void searchAddress(String query, GeocodingRepository.GeocodeCallback callback) {
        geocodingRepository.searchAddress(query, callback);
    }

    public void reverseGeocode(double lat, double lon, GeocodingRepository.ReverseGeocodeCallback callback) {
        geocodingRepository.reverseGeocode(lat, lon, callback);
    }

    public void clearError() {
        PassengerHomeViewState s = state.getValue();
        if (s != null && s.error) {
            PassengerHomeViewState next = PassengerHomeViewState.idle();
            next.estimate = s.estimate;
            next.orderResult = s.orderResult;
            next.activeRide = s.activeRide;
            next.formDisabled = s.formDisabled;
            state.setValue(next);
        }
    }

    public void resetForm() {
        pickup = null;
        dropoff = null;
        stops.clear();
        scheduledAt = null;
        state.setValue(PassengerHomeViewState.idle());
    }

    public static String formatPrice(Number price) {
        if (price == null) return "";
        return new DecimalFormat("#,##0.00").format(price) + " RSD";
    }

    public static String formatDistance(Double km) {
        if (km == null) return "";
        return new DecimalFormat("#.#").format(km) + " km";
    }

    public static String formatDuration(Integer min) {
        if (min == null) return "";
        return min + " min";
    }

    public static Date getScheduleMin() {
        return new Date();
    }

    public static Date getScheduleMax() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR_OF_DAY, 5);
        return c.getTime();
    }
}
