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

public class PassengerHomeFragment extends Fragment {

    private FragmentPassengerHomeBinding binding;
    private PassengerHomeViewModel viewModel;
    private MapHelper mapHelper;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private static final int DEBOUNCE_MS = 300;

    private final List<View> stopViews = new ArrayList<>();
    private FocusedField focusedField = FocusedField.NONE;

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
            if (focusedField != FocusedField.NONE) {
                viewModel.reverseGeocode(lat, lon, new com.example.blackcar.data.repository.GeocodingRepository.ReverseGeocodeCallback() {
                    @Override
                    public void onSuccess(GeocodeResult result) {
                        LocationPoint lp = new LocationPoint(result.getDisplayName(), result.getLatitude(), result.getLongitude());
                        if (focusedField == FocusedField.PICKUP) {
                            viewModel.setPickup(lp);
                            setPickupAddress(result.getDisplayName());
                        } else if (focusedField == FocusedField.DROPOFF) {
                            viewModel.setDropoff(lp);
                            setDropoffAddress(result.getDisplayName());
                        }
                        updateMapMarkers();
                    }

                    @Override
                    public void onError(String message) {
                        LocationPoint lp = new LocationPoint(lat + ", " + lon, lat, lon);
                        if (focusedField == FocusedField.PICKUP) {
                            viewModel.setPickup(lp);
                            setPickupAddress(lp.getAddress());
                        } else if (focusedField == FocusedField.DROPOFF) {
                            viewModel.setDropoff(lp);
                            setDropoffAddress(lp.getAddress());
                        }
                        updateMapMarkers();
                    }
                });
            }
        });
        mapHelper.setupMapTapOverlay();
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
            recycler.setVisibility(View.GONE);
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
                        recycler.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
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
        binding.btnCancelRide.setOnClickListener(v -> {
            if (viewModel.getState().getValue() != null && viewModel.getState().getValue().orderResult != null) {
                Long rideId = viewModel.getState().getValue().orderResult.getRideId();
                if (rideId != null) viewModel.cancelRide(rideId);
            }
        });
        binding.btnRequestAnother.setOnClickListener(v -> viewModel.resetForm());
        binding.btnViewHistory.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.passengerHistoryFragment));
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
            edit.setText(result.getDisplayName());
            recycler.setVisibility(View.GONE);
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
                        recycler.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    @Override
                    public void onError(String message) {}
                });
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }
            @Override
            public void afterTextChanged(Editable s) {}
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
    }

    private void observeState() {
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            binding.txtError.setVisibility(state.error ? View.VISIBLE : View.GONE);
            if (state.error) binding.txtError.setText(state.errorMessage);

            binding.layoutEstimate.setVisibility(state.estimate != null ? View.VISIBLE : View.GONE);
            if (state.estimate != null) {
                binding.txtEstimatePrice.setText(PassengerHomeViewModel.formatPrice(state.estimate.getEstimatedPrice()));
                binding.txtEstimateDistance.setText(PassengerHomeViewModel.formatDistance(state.estimate.getDistanceKm()));
                binding.txtEstimateDuration.setText(PassengerHomeViewModel.formatDuration(state.estimate.getEstimatedDurationMinutes()));
            }

            binding.layoutOrderResult.setVisibility(state.orderResult != null ? View.VISIBLE : View.GONE);
            if (state.orderResult != null) {
                binding.txtOrderStatus.setText(state.orderResult.getStatus());
                binding.txtOrderMessage.setText(state.orderResult.getMessage());
                binding.txtOrderRideId.setText("Ride ID: " + state.orderResult.getRideId());
                binding.txtOrderDriver.setText(state.orderResult.getAssignedDriverEmail() != null
                        ? "Driver: " + state.orderResult.getAssignedDriverEmail() : "");
                boolean canCancel = "ACCEPTED".equals(state.orderResult.getStatus()) || "SCHEDULED".equals(state.orderResult.getStatus());
                binding.btnCancelRide.setVisibility(canCancel ? View.VISIBLE : View.GONE);
                binding.btnRequestAnother.setVisibility("CANCELLED".equals(state.orderResult.getStatus()) || "REJECTED".equals(state.orderResult.getStatus()) ? View.VISIBLE : View.GONE);
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
    public void onDestroyView() {
        super.onDestroyView();
        if (mapHelper != null) {
            mapHelper.removeMapTapOverlay();
        }
        binding = null;
    }
}
