package com.example.blackcar.presentation.home.views;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.blackcar.R;
import com.example.blackcar.data.api.model.LocationPoint;
import com.example.blackcar.data.api.model.PassengerRideDetailResponse;
import com.example.blackcar.data.api.model.WebRideTrackingResponse;
import com.example.blackcar.databinding.FragmentRideTrackingBinding;
import com.example.blackcar.presentation.ViewModelFactory;
import com.example.blackcar.presentation.history.util.MapHelper;
import com.example.blackcar.presentation.home.viewmodel.RideTrackingViewModel;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Collections;

public class RideTrackingFragment extends Fragment {

    private FragmentRideTrackingBinding binding;
    private RideTrackingViewModel viewModel;
    private MapHelper mapHelper;
    private Marker driverMarker;
    private Marker pickupMarker;
    private Marker destinationMarker;
    private Long rideId;
    private boolean initialZoomDone = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            rideId = getArguments().getLong("rideId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRideTrackingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));

        viewModel = new ViewModelProvider(this, new ViewModelFactory(requireContext()))
                .get(RideTrackingViewModel.class);

        setupMap();
        setupClickListeners();
        observeViewModel();

        if (rideId != null) {
            viewModel.startTracking(rideId);
        } else {
            Toast.makeText(requireContext(), "Ride ID missing", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();
        }
    }

    private void setupMap() {
        MapView mapView = (MapView) binding.includeMap.getRoot();
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        
        mapHelper = new MapHelper(requireContext(), mapView);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        
        binding.btnShowReport.setOnClickListener(v -> {
            boolean visible = binding.layoutReportForm.getVisibility() == View.VISIBLE;
            binding.layoutReportForm.setVisibility(visible ? View.GONE : View.VISIBLE);
            binding.btnShowReport.setText(visible ? "Report Driver" : "Cancel Report");
        });

        binding.btnSubmitReport.setOnClickListener(v -> {
            String text = binding.editReport.getText().toString();
            if (!text.trim().isEmpty()) {
                viewModel.reportInconsistency(text);
                binding.editReport.setText("");
                binding.layoutReportForm.setVisibility(View.GONE);
                binding.btnShowReport.setText("Report Driver");
                Toast.makeText(requireContext(), "Report submitted", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getRideDetails().observe(getViewLifecycleOwner(), details -> {
            if (details == null) return;
            updateDriverInfo(details);
            drawRoute(details);
        });

        viewModel.getTrackingState().observe(getViewLifecycleOwner(), tracking -> {
            if (tracking == null) return;
            updateUI(tracking);
            updateMap(tracking);
        });

        viewModel.getErrorState().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getRideEnded().observe(getViewLifecycleOwner(), ended -> {
            if (ended) {
                showRideEndedDialog();
            }
        });
    }

    private void updateDriverInfo(PassengerRideDetailResponse details) {
        if (details.getDriver() != null) {
            String name = details.getDriver().getFirstName() + " " + details.getDriver().getLastName();
            binding.txtDriverName.setText(name);
        }
        
        String vehicleInfo = details.getVehicleType();
        binding.txtVehicleInfo.setText(vehicleInfo);
    }

    private void drawRoute(PassengerRideDetailResponse details) {
        if (mapHelper == null) return;
        if (details.getPickup() != null) {
            if (pickupMarker != null) pickupMarker.remove(((MapView)binding.includeMap.getRoot()));
            pickupMarker = mapHelper.addPickupMarker(details.getPickup().getLatitude(), details.getPickup().getLongitude(), "Pickup");
        }
        if (details.getDropoff() != null) {
            if (destinationMarker != null) destinationMarker.remove(((MapView)binding.includeMap.getRoot()));
            destinationMarker = mapHelper.addDropoffMarker(details.getDropoff().getLatitude(), details.getDropoff().getLongitude(), "Destination");
        }
        // TODO: Could also draw the polyline if routeCoordinates are available
    }

    private void showRideEndedDialog() {
        if (!isAdded()) return;
        
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ride_ended, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialogView.findViewById(R.id.btnDialogOk).setOnClickListener(v -> {
            dialog.dismiss();
            Navigation.findNavController(requireView()).popBackStack();
        });

        dialog.show();
    }

    private void updateUI(WebRideTrackingResponse tracking) {
        binding.txtStatus.setText(tracking.getRideStatus());
        binding.txtEta.setText(tracking.getEstimatedTimeToDestinationMinutes() != null 
                ? tracking.getEstimatedTimeToDestinationMinutes() + " min" : "--");
        
        if (tracking.getVehicle() != null) {
            binding.txtVehicleInfo.setText(tracking.getVehicle().getType() + " (" + tracking.getVehicle().getLicensePlate() + ")");
        }
        
        // Note: Driver name might not be in tracking response, could be loaded from ride details if needed
        // For now keep placeholder or use what we have.
    }

    private void updateMap(WebRideTrackingResponse tracking) {
        if (mapHelper == null) return;
        if (tracking.getVehicleLatitude() == null || tracking.getVehicleLongitude() == null) {
            Log.d("RideTracking", "No location in tracking update");
            return;
        }

        double lat = tracking.getVehicleLatitude();
        double lon = tracking.getVehicleLongitude();
        GeoPoint vehiclePos = new GeoPoint(lat, lon);

        Log.d("RideTracking", "Updating car position to: " + lat + ", " + lon);

        if (driverMarker == null) {
            driverMarker = mapHelper.addCarMarker(lat, lon, "Your Driver");
        } else {
            driverMarker.setPosition(vehiclePos);
            mapHelper.ensureMarkerOnMap(driverMarker);
        }
        
        if (!initialZoomDone) {
            ((MapView)binding.includeMap.getRoot()).getController().setCenter(vehiclePos);
            ((MapView)binding.includeMap.getRoot()).getController().setZoom(17.0);
            initialZoomDone = true;
        }
        
        ((MapView)binding.includeMap.getRoot()).invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MapView)binding.includeMap.getRoot()).onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MapView)binding.includeMap.getRoot()).onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null && binding.includeMap != null) {
            ((MapView)binding.includeMap.getRoot()).onDetach();
        }
        mapHelper = null;
        binding = null;
    }
}
