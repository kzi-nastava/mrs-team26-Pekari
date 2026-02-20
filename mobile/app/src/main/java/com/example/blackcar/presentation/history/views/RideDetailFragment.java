package com.example.blackcar.presentation.history.views;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.blackcar.R;
import com.example.blackcar.data.api.model.PassengerRideDetailResponse;
import com.example.blackcar.data.api.model.LocationPoint;
import com.example.blackcar.databinding.FragmentRideDetailBinding;
import com.example.blackcar.presentation.history.util.MapHelper;
import com.example.blackcar.presentation.history.viewmodel.RideDetailViewModel;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RideDetailFragment extends DialogFragment {

    private static final String ARG_RIDE_ID = "ride_id";

    private FragmentRideDetailBinding binding;
    private RideDetailViewModel viewModel;
    private Long rideId;
    private MapView mapView;
    private MapHelper mapHelper;

    public static RideDetailFragment newInstance(Long rideId) {
        RideDetailFragment fragment = new RideDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_RIDE_ID, rideId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_BlackCar);

        if (getArguments() != null) {
            rideId = getArguments().getLong(ARG_RIDE_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRideDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));

        viewModel = new ViewModelProvider(this).get(RideDetailViewModel.class);

        setupMap();
        setupUI();
        observeState();

        if (rideId != null) {
            viewModel.loadRideDetail(rideId);
        }
    }

    private void setupMap() {
        mapView = binding.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK); // OpenStreetMap tiles
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(13.0);
        mapView.getController().setCenter(new GeoPoint(45.2671, 19.8335)); // Default center (Novi Sad)

        mapHelper = new MapHelper(requireContext(), mapView);
    }

    private void setupUI() {
        binding.btnClose.setOnClickListener(v -> dismiss());
    }

    private void observeState() {
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            binding.progressDetail.setVisibility(state.loading ? View.VISIBLE : View.GONE);

            if (state.error) {
                binding.txtErrorDetail.setVisibility(View.VISIBLE);
                binding.txtErrorDetail.setText(state.errorMessage);
                binding.contentContainer.setVisibility(View.GONE);
            } else if (state.rideDetail != null) {
                binding.txtErrorDetail.setVisibility(View.GONE);
                binding.contentContainer.setVisibility(View.VISIBLE);
                displayRideDetail(state.rideDetail);
            }
        });
    }

    private void displayRideDetail(PassengerRideDetailResponse detail) {
        // Status
        if (detail.getStatus() != null) {
            binding.txtDetailStatus.setText(detail.getStatus());
            if ("COMPLETED".equals(detail.getStatus())) {
                binding.txtDetailStatus.setTextColor(requireContext().getColor(R.color.accent_success));
            } else if ("CANCELLED".equals(detail.getStatus())) {
                binding.txtDetailStatus.setTextColor(requireContext().getColor(R.color.accent_danger));
            }
        }

        // Route
        String pickup = detail.getPickupAddress() != null ? detail.getPickupAddress() : "Unknown";
        String dropoff = detail.getDropoffAddress() != null ? detail.getDropoffAddress() : "Unknown";
        binding.txtDetailPickup.setText(makeBoldLabel("Pickup: ", pickup));

        // Display stops
        if (detail.getStops() != null && !detail.getStops().isEmpty()) {
            binding.containerStops.setVisibility(View.VISIBLE);
            binding.containerStops.removeAllViews();

            for (int i = 0; i < detail.getStops().size(); i++) {
                LocationPoint stop = detail.getStops().get(i);
                String stopAddress = stop.getAddress() != null ? stop.getAddress() : "Stop " + (i + 1);

                TextView stopView = new TextView(requireContext());
                stopView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                stopView.setText(makeBoldLabel("Stop " + (i + 1) + ": ", stopAddress));
                stopView.setTextColor(requireContext().getColor(R.color.text_primary));
                stopView.setTextSize(14);

                // Add margin
                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(stopView.getLayoutParams());
                params.bottomMargin = (int) (4 * getResources().getDisplayMetrics().density);
                stopView.setLayoutParams(params);

                binding.containerStops.addView(stopView);
            }
        } else {
            binding.containerStops.setVisibility(View.GONE);
        }

        binding.txtDetailDropoff.setText(makeBoldLabel("Dropoff: ", dropoff));

        // Ride Information
        if (detail.getPrice() != null) {
            String priceValue = String.format(Locale.getDefault(), "%.2f RSD", detail.getPrice().doubleValue());
            binding.txtDetailPrice.setText(makeBoldLabel("Price: ", priceValue));
        }

        if (detail.getDistanceKm() != null) {
            String distanceValue = String.format(Locale.getDefault(), "%.1f km", detail.getDistanceKm());
            binding.txtDetailDistance.setText(makeBoldLabel("Distance: ", distanceValue));
        }

        if (detail.getVehicleType() != null) {
            binding.txtDetailVehicle.setText(makeBoldLabel("Vehicle: ", detail.getVehicleType()));
        }

        // Time
        String startTime = formatDateTime(detail.getStartedAt());
        String endTime = formatDateTime(detail.getCompletedAt());
        binding.txtDetailTime.setText(makeBoldLabel("Time: ", startTime + " - " + endTime));

        // Driver (for passenger view)
        if (detail.getDriver() != null) {
            binding.cardDriver.setVisibility(View.VISIBLE);
            PassengerRideDetailResponse.DriverDetailInfo driver = detail.getDriver();
            StringBuilder driverInfo = new StringBuilder();
            driverInfo.append(driver.getFirstName()).append(" ").append(driver.getLastName());
            if (driver.getEmail() != null && !driver.getEmail().isEmpty()) {
                driverInfo.append("\n").append(driver.getEmail());
            }
            if (driver.getPhoneNumber() != null && !driver.getPhoneNumber().isEmpty()) {
                driverInfo.append("\n").append(driver.getPhoneNumber());
            }
            binding.txtDetailDriver.setText(driverInfo.toString());
        } else {
            binding.cardDriver.setVisibility(View.GONE);
        }

        // Ratings
        if (detail.getRatings() != null && !detail.getRatings().isEmpty()) {
            binding.cardRatings.setVisibility(View.VISIBLE);
            StringBuilder ratingsText = new StringBuilder();
            for (PassengerRideDetailResponse.RideRatingInfo rating : detail.getRatings()) {
                ratingsText.append("Driver: ").append(getStars(rating.getDriverRating())).append("\n");
                ratingsText.append("Vehicle: ").append(getStars(rating.getVehicleRating())).append("\n");
                if (rating.getComment() != null && !rating.getComment().isEmpty()) {
                    ratingsText.append("\"").append(rating.getComment()).append("\"\n");
                }
                ratingsText.append("\n");
            }
            binding.txtDetailRatings.setText(ratingsText.toString().trim());
        } else {
            binding.cardRatings.setVisibility(View.GONE);
        }

        // Inconsistency Reports
        if (detail.getInconsistencyReports() != null && !detail.getInconsistencyReports().isEmpty()) {
            binding.cardInconsistencies.setVisibility(View.VISIBLE);
            StringBuilder reportsText = new StringBuilder();
            for (PassengerRideDetailResponse.InconsistencyReportInfo report : detail.getInconsistencyReports()) {
                reportsText.append("• ").append(report.getDescription()).append("\n");
                reportsText.append("  Reported by: ").append(report.getReportedByName()).append("\n");
                reportsText.append("  Date: ").append(formatDateTime(report.getReportedAt())).append("\n\n");
            }
            binding.txtDetailInconsistencies.setText(reportsText.toString().trim());
        } else {
            binding.cardInconsistencies.setVisibility(View.GONE);
        }

        // Draw route on map
        drawRouteOnMap(detail);
    }

    private void drawRouteOnMap(PassengerRideDetailResponse detail) {
        if (mapHelper == null) return;

        mapHelper.clearMap();

        List<LocationPoint> allPoints = new ArrayList<>();

        // Add pickup marker
        if (detail.getPickup() != null && detail.getPickup().getLatitude() != null) {
            mapHelper.addPickupMarker(
                detail.getPickup().getLatitude(),
                detail.getPickup().getLongitude(),
                "Pickup"
            );
            allPoints.add(detail.getPickup());
        }

        // Add stop markers
        if (detail.getStops() != null) {
            for (int i = 0; i < detail.getStops().size(); i++) {
                LocationPoint stop = detail.getStops().get(i);
                if (stop.getLatitude() != null) {
                    mapHelper.addStopMarker(
                        stop.getLatitude(),
                        stop.getLongitude(),
                        "Stop " + (i + 1)
                    );
                    allPoints.add(stop);
                }
            }
        }

        // Add dropoff marker
        if (detail.getDropoff() != null && detail.getDropoff().getLatitude() != null) {
            mapHelper.addDropoffMarker(
                detail.getDropoff().getLatitude(),
                detail.getDropoff().getLongitude(),
                "Dropoff"
            );
            allPoints.add(detail.getDropoff());
        }

        // Draw route line if we have route coordinates
        if (detail.getRouteCoordinates() != null && !detail.getRouteCoordinates().isEmpty()) {
            List<LocationPoint> routePoints = parseRouteCoordinates(detail.getRouteCoordinates());
            if (routePoints != null && !routePoints.isEmpty()) {
                mapHelper.drawRoute(routePoints, Color.parseColor("#3b82f6")); // Blue color like frontend
                allPoints.addAll(routePoints);
            }
        }

        // Fit map bounds to show all points
        if (!allPoints.isEmpty()) {
            mapHelper.fitBounds(allPoints);
        }
    }

    private List<LocationPoint> parseRouteCoordinates(String routeCoordinatesJson) {
        List<LocationPoint> points = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(routeCoordinatesJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                Object item = jsonArray.get(i);
                if (item instanceof JSONArray) {
                    JSONArray pointArray = (JSONArray) item;
                    if (pointArray.length() >= 2) {
                        LocationPoint point = new LocationPoint();
                        point.setLatitude(pointArray.getDouble(0));
                        point.setLongitude(pointArray.getDouble(1));
                        points.add(point);
                    }
                } else if (item instanceof JSONObject) {
                    JSONObject pointObj = (JSONObject) item;
                    LocationPoint point = new LocationPoint();
                    point.setLatitude(pointObj.getDouble("latitude"));
                    point.setLongitude(pointObj.getDouble("longitude"));
                    if (pointObj.has("address")) {
                        point.setAddress(pointObj.getString("address"));
                    }
                    points.add(point);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return points;
    }

    private String formatDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return "--:--";
        }
        try {
            // Parse ISO datetime and format it
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(dateTime);
            return date != null ? outputFormat.format(date) : dateTime;
        } catch (Exception e) {
            return dateTime;
        }
    }

    private String getStars(Integer rating) {
        if (rating == null || rating < 0) return "N/A";
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            stars.append(i < rating ? "★" : "☆");
        }
        return stars.toString();
    }

    /**
     * Helper method to create text with bold label and regular value
     */
    private SpannableString makeBoldLabel(String label, String value) {
        String fullText = label + value;
        SpannableString spannable = new SpannableString(fullText);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.onDetach();
            mapView = null;
        }
        mapHelper = null;
        binding = null;
    }
}
