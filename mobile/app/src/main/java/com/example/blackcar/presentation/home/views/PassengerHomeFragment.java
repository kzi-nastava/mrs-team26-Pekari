package com.example.blackcar.presentation.home.views;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blackcar.R;
import com.example.blackcar.data.api.model.GeocodeResult;
import com.example.blackcar.data.api.model.LocationPoint;
import com.example.blackcar.data.repository.GeocodingRepository;
import com.example.blackcar.databinding.FragmentPassengerHomeBinding;
import com.example.blackcar.presentation.ViewModelFactory;
import com.example.blackcar.presentation.home.viewmodel.PassengerHomeViewModel;
import com.example.blackcar.presentation.history.util.MapHelper;
import com.example.blackcar.data.repository.DriversRepository;
import com.example.blackcar.data.api.model.OnlineDriverWithVehicleResponse;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.util.Log;

public class PassengerHomeFragment extends Fragment {

    private static final String TAG = "PassengerHomeFragment";
    private FragmentPassengerHomeBinding binding;
    private PassengerHomeViewModel viewModel;
    private MapHelper mapHelper;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private static final int DEBOUNCE_MS = 300;

    // --- Real-time vehicles ---
    private final DriversRepository driversRepository = new DriversRepository();
    private final Handler vehiclesHandler = new Handler(Looper.getMainLooper());
    private Runnable vehiclesRunnable;
    private static final int VEHICLES_POLL_MS = 5000;
    private List<OnlineDriverWithVehicleResponse> lastVehicles = new ArrayList<>();

    private final List<View> stopViews = new ArrayList<>();
    private FocusedField focusedField = FocusedField.NONE;
    private int focusedStopIndex = -1; // Track which stop is focused for map click

    private enum FocusedField { NONE, PICKUP, DROPOFF, STOP }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPassengerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));

        viewModel = new ViewModelProvider(this, new ViewModelFactory(requireContext()))
                .get(PassengerHomeViewModel.class);

        setupMap();
        setupAddressInputs();
        setupVehicleType();
        setupSchedule();
        setupSwitches();
        setupButtons();
        observeState();

        // Start polling vehicles on map
        startVehiclesPolling();

        viewModel.loadActiveRide();
    }

    private void setupMap() {
        MapView mapView = (MapView) binding.includeMap.getRoot();
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(13.0);
        mapView.getController().setCenter(new GeoPoint(45.2671, 19.8335));

        mapHelper = new MapHelper(requireContext(), mapView);
        mapHelper.setOnMapClickListener((lat, lon) -> {
            // Determine target field - use focused field if any, otherwise auto-detect like web frontend
            FocusedField target = focusedField;
            final int targetStopIndex = focusedStopIndex;

            if (target == FocusedField.NONE) {
                // Auto-detect: if pickup is empty, set pickup; else if dropoff is empty, set dropoff
                if (viewModel.getPickup() == null || viewModel.getPickup().getLatitude() == null) {
                    target = FocusedField.PICKUP;
                } else if (viewModel.getDropoff() == null || viewModel.getDropoff().getLatitude() == null) {
                    target = FocusedField.DROPOFF;
                } else {
                    target = FocusedField.DROPOFF; // Default to dropoff if both filled
                }
            }

            final FocusedField finalTarget = target;
            viewModel.reverseGeocode(lat, lon, new com.example.blackcar.data.repository.GeocodingRepository.ReverseGeocodeCallback() {
                @Override
                public void onSuccess(GeocodeResult result) {
                    LocationPoint lp = new LocationPoint(result.getDisplayName(), result.getLatitude(), result.getLongitude());
                    applyLocationToField(finalTarget, targetStopIndex, lp, result.getDisplayName());
                }

                @Override
                public void onError(String message) {
                    String fallbackAddress = lat + ", " + lon;
                    LocationPoint lp = new LocationPoint(fallbackAddress, lat, lon);
                    applyLocationToField(finalTarget, targetStopIndex, lp, fallbackAddress);
                }
            });
        });
        mapHelper.setupMapTapOverlay();
    }

    private void startVehiclesPolling() {
        Log.d(TAG, "Starting vehicles polling");
        stopVehiclesPolling();
        vehiclesRunnable = new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Polling vehicles...");
                driversRepository.fetchOnlineWithVehicles(0, 100, new DriversRepository.ResultCallback() {
                    @Override
                    public void onSuccess(List<OnlineDriverWithVehicleResponse> data) {
                        Log.v(TAG, "Received " + (data != null ? data.size() : 0) + " vehicles");
                        lastVehicles = data != null ? data : new ArrayList<>();
                        renderVehicles();
                    }
                    @Override
                    public void onError(String message) {
                        Log.w(TAG, "Vehicles polling error: " + message);
                        // Ignore transient errors; try again next tick
                    }
                });
                vehiclesHandler.postDelayed(this, VEHICLES_POLL_MS);
            }
        };
        vehiclesHandler.post(vehiclesRunnable);
    }

    private void stopVehiclesPolling() {
        Log.d(TAG, "Stopping vehicles polling");
        if (vehiclesRunnable != null) {
            vehiclesHandler.removeCallbacks(vehiclesRunnable);
            vehiclesRunnable = null;
        }
        if (mapHelper != null) {
            mapHelper.clearVehicleMarkers();
        }
    }

    private void renderVehicles() {
        if (mapHelper == null) {
            Log.w(TAG, "mapHelper is null, cannot render vehicles");
            return;
        }
        mapHelper.clearVehicleMarkers();
        if (lastVehicles == null) return;
        Log.v(TAG, "Rendering " + lastVehicles.size() + " vehicles on map");
        for (OnlineDriverWithVehicleResponse v : lastVehicles) {
            if (v == null || v.driverState == null) continue;
            Double lat = v.driverState.latitude;
            Double lon = v.driverState.longitude;
            Boolean busy = v.driverState.busy;
            if (lat == null || lon == null) continue;
            String title = (v.vehicleRegistration != null && !v.vehicleRegistration.isEmpty())
                    ? v.vehicleRegistration
                    : (v.vehicleType != null ? v.vehicleType : "Vehicle");
            mapHelper.addVehicleMarker(lat, lon, title, Boolean.TRUE.equals(busy));
        }
    }

    private void applyLocationToField(FocusedField target, int stopIndex, LocationPoint lp, String address) {
        if (target == FocusedField.PICKUP) {
            viewModel.setPickup(lp);
            setPickupAddress(address);
        } else if (target == FocusedField.DROPOFF) {
            viewModel.setDropoff(lp);
            setDropoffAddress(address);
        } else if (target == FocusedField.STOP && stopIndex >= 0) {
            // Update the stop in ViewModel
            List<LocationPoint> stops = new ArrayList<>(viewModel.getStops());
            while (stops.size() <= stopIndex) stops.add(null);
            stops.set(stopIndex, lp);
            viewModel.setStops(stops);
            // Update the stop's EditText
            setStopAddress(stopIndex, address);
        }
        updateMapMarkers();
    }

    private void setStopAddress(int index, String address) {
        if (index >= 0 && index < stopViews.size()) {
            View stopRow = stopViews.get(index);
            // stopRow is the LinearLayout containing the address_autocomplete and remove button
            View autocompleteView = ((ViewGroup) stopRow).getChildAt(0);
            TextInputEditText edit = autocompleteView.findViewById(R.id.editAddress);
            if (edit != null) {
                edit.setText(address);
            }
        }
    }

    private void setupAddressInputs() {
        TextInputLayout pickupLayout = binding.includePickup.getRoot().findViewById(R.id.textInputLayout);
        pickupLayout.setHint(getString(R.string.ride_order_pickup));
        setupAutocomplete(binding.includePickup.getRoot(), this::onPickupSelected, () -> focusedField = FocusedField.PICKUP);

        TextInputLayout dropoffLayout = binding.includeDropoff.getRoot().findViewById(R.id.textInputLayout);
        dropoffLayout.setHint(getString(R.string.ride_order_dropoff));
        setupAutocomplete(binding.includeDropoff.getRoot(), this::onDropoffSelected, () -> focusedField = FocusedField.DROPOFF);
    }

    private void setupAutocomplete(View includeRoot, AddressSelectionListener listener, Runnable onFocus) {
        TextInputEditText edit = includeRoot.findViewById(R.id.editAddress);
        RecyclerView recycler = includeRoot.findViewById(R.id.recyclerSuggestions);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        AddressAutocompleteAdapter adapter = new AddressAutocompleteAdapter();
        adapter.setOnAddressSelectedListener(result -> {
            listener.onSelected(result.getDisplayName(), result.getLatitude(), result.getLongitude());
            edit.clearFocus();
            recycler.setVisibility(View.GONE);
            adapter.submitList(new ArrayList<>());
            edit.setText(result.getDisplayName());
        });
        recycler.setAdapter(adapter);

        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> viewModel.searchAddress(s.toString(), new GeocodingRepository.GeocodeCallback() {
                    @Override
                    public void onSuccess(List<GeocodeResult> results) {
                        adapter.submitList(results);
                        recycler.setVisibility((results.isEmpty() || !edit.hasFocus()) ? View.GONE : View.VISIBLE);
                    }
                    @Override
                    public void onError(String message) {}
                });
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        edit.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) onFocus.run();
            else recycler.setVisibility(View.GONE);
        });
    }

    private interface AddressSelectionListener {
        void onSelected(String address, double lat, double lon);
    }

    private void onPickupSelected(String address, double lat, double lon) {
        LocationPoint lp = new LocationPoint(address, lat, lon);
        viewModel.setPickup(lp);
        updateMapMarkers();
    }

    private void onDropoffSelected(String address, double lat, double lon) {
        LocationPoint lp = new LocationPoint(address, lat, lon);
        viewModel.setDropoff(lp);
        updateMapMarkers();
    }

    private void setPickupAddress(String address) {
        TextInputEditText edit = binding.includePickup.getRoot().findViewById(R.id.editAddress);
        edit.setText(address);
    }

    private void setDropoffAddress(String address) {
        TextInputEditText edit = binding.includeDropoff.getRoot().findViewById(R.id.editAddress);
        edit.setText(address);
    }

    private void setupVehicleType() {
        MaterialButtonToggleGroup toggle = binding.toggleVehicleType;
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnVehicleStandard) viewModel.setVehicleType("STANDARD");
                else if (checkedId == R.id.btnVehicleVan) viewModel.setVehicleType("VAN");
                else if (checkedId == R.id.btnVehicleLux) viewModel.setVehicleType("LUX");
            }
        });
        toggle.check(R.id.btnVehicleStandard);
    }

    private void setupSchedule() {
        binding.btnSchedule.setText("Now");
        binding.btnSchedule.setOnClickListener(v -> showSchedulePicker());
    }

    private void showSchedulePicker() {
        Calendar c = Calendar.getInstance();
        if (viewModel.getScheduledAt() != null) {
            c.setTime(viewModel.getScheduledAt());
        }
        new android.app.DatePickerDialog(requireContext(), (d, y, m, day) -> {
            c.set(y, m, day);
            new android.app.TimePickerDialog(requireContext(), (t, hour, min) -> {
                c.set(Calendar.HOUR_OF_DAY, hour);
                c.set(Calendar.MINUTE, min);
                viewModel.setScheduledAt(c.getTime());
                binding.btnSchedule.setText(String.format("%1$td/%1$tm %1$tH:%1$tM", c));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupSwitches() {
        binding.switchBaby.setOnCheckedChangeListener((v, checked) -> viewModel.setBabyTransport(checked));
        binding.switchPet.setOnCheckedChangeListener((v, checked) -> viewModel.setPetTransport(checked));
    }

    private void setupButtons() {
        binding.btnAddStop.setOnClickListener(v -> addStop());
        binding.btnEstimate.setOnClickListener(v -> viewModel.estimateRide());
        binding.btnRequestRide.setOnClickListener(v -> {
            String emailsStr = binding.editPassengerEmails.getText() != null ? binding.editPassengerEmails.getText().toString() : "";
            List<String> emails = new ArrayList<>();
            for (String e : emailsStr.split(",")) {
                String t = e.trim();
                if (!t.isEmpty()) emails.add(t);
            }
            viewModel.setPassengerEmails(emails);
            viewModel.orderRide();
        });
        binding.btnRequestStop.setOnClickListener(v -> {
            if (viewModel.getState().getValue() != null && viewModel.getState().getValue().orderResult != null) {
                Long rideId = viewModel.getState().getValue().orderResult.getRideId();
                if (rideId != null) viewModel.requestStopRide(rideId);
            }
        });
        binding.btnPanic.setOnClickListener(v -> {
            if (viewModel.getState().getValue() != null && viewModel.getState().getValue().orderResult != null) {
                Long rideId = viewModel.getState().getValue().orderResult.getRideId();
                if (rideId != null) viewModel.activatePanic(rideId);
            }
        });
        binding.btnCancelRide.setOnClickListener(v -> {
            if (viewModel.getState().getValue() != null && viewModel.getState().getValue().orderResult != null) {
                Long rideId = viewModel.getState().getValue().orderResult.getRideId();
                if (rideId != null) viewModel.cancelRide(rideId);
            }
        });
        binding.btnRequestAnother.setOnClickListener(v -> viewModel.resetForm());
        binding.fabChat.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_home_to_chat));
    }

    private void addStop() {
        int index = stopViews.size();
        View stopRow = getLayoutInflater().inflate(R.layout.address_autocomplete, binding.containerStops, false);
        TextInputLayout layout = stopRow.findViewById(R.id.textInputLayout);
        layout.setHint(getString(R.string.ride_order_stop, index + 1));
        Button btnRemove = new Button(requireContext());
        btnRemove.setText("×");
        btnRemove.setBackground(null);
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(stopRow, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(btnRemove);
        binding.containerStops.addView(row);
        stopViews.add(row);
        final int idx = index;
        btnRemove.setOnClickListener(v -> removeStop(row, idx));
        setupStopAutocomplete(stopRow, idx);
    }

    private void removeStop(View row, int index) {
        binding.containerStops.removeView(row);
        stopViews.remove(row);
        List<LocationPoint> stops = new ArrayList<>(viewModel.getStops());
        if (index < stops.size()) {
            stops.remove(index);
            viewModel.setStops(stops);
        }
        updateMapMarkers();
    }

    private void setupStopAutocomplete(View stopRow, int index) {
        TextInputEditText edit = stopRow.findViewById(R.id.editAddress);
        RecyclerView recycler = stopRow.findViewById(R.id.recyclerSuggestions);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        AddressAutocompleteAdapter adapter = new AddressAutocompleteAdapter();
        adapter.setOnAddressSelectedListener(result -> {
            LocationPoint lp = new LocationPoint(result.getDisplayName(), result.getLatitude(), result.getLongitude());
            List<LocationPoint> stops = new ArrayList<>(viewModel.getStops());
            while (stops.size() <= index) stops.add(null);
            stops.set(index, lp);
            viewModel.setStops(stops);
            edit.clearFocus();
            recycler.setVisibility(View.GONE);
            adapter.submitList(new ArrayList<>());
            edit.setText(result.getDisplayName());
            updateMapMarkers();
        });
        recycler.setAdapter(adapter);
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> viewModel.searchAddress(s.toString(), new GeocodingRepository.GeocodeCallback() {
                    @Override
                    public void onSuccess(List<GeocodeResult> results) {
                        adapter.submitList(results);
                        recycler.setVisibility((results.isEmpty() || !edit.hasFocus()) ? View.GONE : View.VISIBLE);
                    }
                    @Override
                    public void onError(String message) {}
                });
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        edit.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Set focused field to STOP with the correct index for map click support
                focusedField = FocusedField.STOP;
                focusedStopIndex = index;
            } else {
                recycler.setVisibility(View.GONE);
            }
        });
    }

    private void updateMapMarkers() {
        mapHelper.clearMap();
        if (viewModel.getPickup() != null && viewModel.getPickup().getLatitude() != null) {
            mapHelper.addPickupMarker(viewModel.getPickup().getLatitude(), viewModel.getPickup().getLongitude(), "Pickup");
        }
        if (viewModel.getDropoff() != null && viewModel.getDropoff().getLatitude() != null) {
            mapHelper.addDropoffMarker(viewModel.getDropoff().getLatitude(), viewModel.getDropoff().getLongitude(), "Dropoff");
        }
        for (int i = 0; i < viewModel.getStops().size(); i++) {
            LocationPoint s = viewModel.getStops().get(i);
            if (s.getLatitude() != null && s.getLongitude() != null) {
                mapHelper.addStopMarker(s.getLatitude(), s.getLongitude(), "Stop " + (i + 1));
            }
        }
        List<LocationPoint> points = new ArrayList<>();
        if (viewModel.getPickup() != null) points.add(viewModel.getPickup());
        points.addAll(viewModel.getStops());
        if (viewModel.getDropoff() != null) points.add(viewModel.getDropoff());
        if (points.size() >= 2 && viewModel.getState().getValue() != null && viewModel.getState().getValue().estimate != null) {
            mapHelper.drawRoute(viewModel.getState().getValue().estimate.getRoutePoints());
        }
        if (points.size() >= 2) {
            mapHelper.fitBounds(points);
        }
        // Re-render vehicles after map overlays were cleared
        renderVehicles();
    }

    private void observeState() {
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            boolean hasOrderResult = state.orderResult != null;

            // Show error only when no order result
            binding.txtError.setVisibility(state.error && !hasOrderResult ? View.VISIBLE : View.GONE);
            if (state.error) binding.txtError.setText(state.errorMessage);

            // Hide form container when order result exists (matches web behavior)
            binding.layoutFormContainer.setVisibility(hasOrderResult ? View.GONE : View.VISIBLE);

            // Show estimate only when there's no order result (matches web behavior)
            binding.layoutEstimate.setVisibility(state.estimate != null && !hasOrderResult ? View.VISIBLE : View.GONE);
            if (state.estimate != null) {
                binding.txtEstimatePrice.setText(PassengerHomeViewModel.formatPrice(state.estimate.getEstimatedPrice()));
                binding.txtEstimateDistance.setText(PassengerHomeViewModel.formatDistance(state.estimate.getDistanceKm()));
                binding.txtEstimateDuration.setText(PassengerHomeViewModel.formatDuration(state.estimate.getEstimatedDurationMinutes()));
            }

            // Show order result modal
            binding.layoutOrderResult.setVisibility(hasOrderResult ? View.VISIBLE : View.GONE);
            if (hasOrderResult) {
                String status = state.orderResult.getStatus();
                binding.txtOrderStatus.setText(status);

                // Set status color based on status
                int statusColor;
                if ("CANCELLED".equals(status) || "REJECTED".equals(status)) {
                    statusColor = getResources().getColor(R.color.accent_danger, null);
                } else if ("ACCEPTED".equals(status) || "PENDING".equals(status) || "SCHEDULED".equals(status)) {
                    statusColor = getResources().getColor(R.color.accent_success, null);
                } else {
                    statusColor = getResources().getColor(R.color.text_primary, null);
                }
                binding.txtOrderStatus.setTextColor(statusColor);

                binding.txtOrderMessage.setText(state.orderResult.getMessage());
                binding.txtOrderRideId.setText("Ride ID: " + state.orderResult.getRideId());
                binding.txtOrderDriver.setText(state.orderResult.getAssignedDriverEmail() != null
                        ? "Driver: " + state.orderResult.getAssignedDriverEmail() : "");
                binding.txtOrderDriver.setVisibility(state.orderResult.getAssignedDriverEmail() != null ? View.VISIBLE : View.GONE);

                // Show request stop button only during IN_PROGRESS and not already requested
                boolean canRequestStop = PassengerHomeViewModel.canRequestStop(status) && !state.stopRequested;
                binding.btnRequestStop.setVisibility(canRequestStop ? View.VISIBLE : View.GONE);

                // Show panic button during IN_PROGRESS or STOP_REQUESTED
                boolean canPanic = PassengerHomeViewModel.canActivatePanic(status);
                binding.btnPanic.setVisibility(canPanic ? View.VISIBLE : View.GONE);
                binding.btnPanic.setEnabled(!state.panicActivated);
                if (state.panicActivated) {
                    binding.btnPanic.setText(R.string.passenger_panic_activated);
                    binding.btnPanic.setBackgroundTintList(getResources().getColorStateList(R.color.btn_panic_disabled, null));
                } else {
                    binding.btnPanic.setText(R.string.passenger_btn_panic);
                    binding.btnPanic.setBackgroundTintList(getResources().getColorStateList(R.color.btn_panic, null));
                }

                // Show cancel button only for active rides that can be cancelled (but not during IN_PROGRESS or STOP_REQUESTED)
                boolean canCancel = ("ACCEPTED".equals(status) || "SCHEDULED".equals(status) || "PENDING".equals(status))
                        && !"IN_PROGRESS".equals(status) && !"STOP_REQUESTED".equals(status);
                binding.btnCancelRide.setVisibility(canCancel ? View.VISIBLE : View.GONE);

                // Show "Request Another" button when ride is finished/cancelled
                boolean rideEnded = "CANCELLED".equals(status) || "REJECTED".equals(status) || "COMPLETED".equals(status);
                binding.btnRequestAnother.setVisibility(rideEnded ? View.VISIBLE : View.GONE);
            }

            setFormEnabled(!state.formDisabled);
            updateMapMarkers();
        });
    }

    private void setFormEnabled(boolean enabled) {
        binding.includePickup.getRoot().setEnabled(enabled);
        binding.includeDropoff.getRoot().setEnabled(enabled);
        binding.editPassengerEmails.setEnabled(enabled);
        binding.toggleVehicleType.setEnabled(enabled);
        binding.btnSchedule.setEnabled(enabled);
        binding.switchBaby.setEnabled(enabled);
        binding.switchPet.setEnabled(enabled);
        binding.btnEstimate.setEnabled(enabled);
        binding.btnRequestRide.setEnabled(enabled);
        binding.btnAddStop.setEnabled(enabled);
        if (enabled) {
            mapHelper.setupMapTapOverlay();
        } else {
            mapHelper.removeMapTapOverlay();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && binding.includeMap != null) {
            MapView mapView = (MapView) binding.includeMap.getRoot();
            if (mapView != null) {
                mapView.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null && binding.includeMap != null) {
            MapView mapView = (MapView) binding.includeMap.getRoot();
            if (mapView != null) {
                mapView.onPause();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapHelper != null) {
            mapHelper.removeMapTapOverlay();
        }
        stopVehiclesPolling();
        
        // Safety null check for mapView which is accessed via includeMap binding
        if (binding != null && binding.includeMap != null) {
            MapView mapView = (MapView) binding.includeMap.getRoot();
            if (mapView != null) {
                mapView.onDetach();
            }
        }
        
        binding = null;
    }
}
