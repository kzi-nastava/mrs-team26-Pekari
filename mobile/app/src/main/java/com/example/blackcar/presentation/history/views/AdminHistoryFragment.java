package com.example.blackcar.presentation.history.views;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.blackcar.R;
import com.example.blackcar.databinding.FragmentAdminHistoryBinding;
import com.example.blackcar.presentation.ViewModelFactory;
import com.example.blackcar.presentation.history.sensor.ShakeDetector;
import com.example.blackcar.presentation.history.viewmodel.AdminHistoryViewModel;
import com.example.blackcar.presentation.history.viewstate.AdminRideUIModel;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class AdminHistoryFragment extends Fragment implements AdminHistoryAdapter.OnRideClickListener {

    private FragmentAdminHistoryBinding binding;
    private AdminHistoryViewModel viewModel;
    private AdminHistoryAdapter adapter;

    // Shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;

    // Sort options
    private final String[] sortOptions = {"Created", "Started", "Completed", "Price", "Distance", "Status", "Pickup", "Dropoff"};
    private final String[] sortValues = {"createdAt", "startedAt", "completedAt", "price", "distanceKm", "status", "pickup", "dropoff"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentAdminHistoryBinding.inflate(inflater, container, false);

        ViewModelFactory factory = new ViewModelFactory(requireContext());
        viewModel = new ViewModelProvider(this, factory).get(AdminHistoryViewModel.class);

        setupRecycler();
        setupFilter();
        setupSortControls();
        setupShakeDetector();

        observeState();

        viewModel.loadHistory(null, null);

        return binding.getRoot();
    }

    private void setupRecycler() {
        adapter = new AdminHistoryAdapter(this);
        binding.recyclerHistory.setAdapter(adapter);
    }

    private void setupFilter() {
        binding.btnFilter.setOnClickListener(v -> openDatePicker());
    }

    private void setupSortControls() {
        // Setup sort field spinner with white text for dark theme
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_white,
                sortOptions
        );
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        binding.spinnerSortField.setAdapter(spinnerAdapter);

        binding.spinnerSortField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sortField = sortValues[position];
                boolean ascending = viewModel.getState().getValue() != null &&
                        viewModel.getState().getValue().sortAscending;
                viewModel.sortRides(sortField, ascending);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Setup sort direction button
        binding.btnSortDirection.setOnClickListener(v -> {
            if (viewModel.getState().getValue() != null) {
                String currentField = viewModel.getState().getValue().sortField;
                boolean currentAscending = viewModel.getState().getValue().sortAscending;
                viewModel.sortRides(currentField, !currentAscending);
            }
        });

        // Setup reset button - resets both sorting and date filter
        binding.btnResetSort.setOnClickListener(v -> {
            binding.spinnerSortField.setSelection(0); // Reset to "Created"
            binding.btnFilter.setText("Filter by date"); // Reset filter button text
            viewModel.loadHistory(null, null); // Reload without date filter
        });
    }

    private void setupShakeDetector() {
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            if (accelerometer != null) {
                shakeDetector = new ShakeDetector(() -> {
                    // Shake detected - toggle date sorting
                    viewModel.toggleDateSort();
                    Toast.makeText(requireContext(), "Date sort toggled!", Toast.LENGTH_SHORT).show();
                });
            } else {
                // No accelerometer available
                Toast.makeText(requireContext(), "Shake detection not available on this device", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openDatePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker
                .Builder
                .dateRangePicker()
                .setTitleText("Filter by date")
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                Long startMillis = selection.first;
                Long endMillis = selection.second;

                if (startMillis != null && endMillis != null) {
                    LocalDate fromDate = Instant.ofEpochMilli(startMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    LocalDate toDate = Instant.ofEpochMilli(endMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    // Update button text to show selected range
                    String filterText = fromDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d")) +
                            " - " + toDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"));
                    binding.btnFilter.setText(filterText);

                    viewModel.loadHistory(fromDate, toDate);
                }
            }
        });

        picker.addOnNegativeButtonClickListener(dialog -> {
        });

        picker.show(getParentFragmentManager(), "datePicker");
    }

    private void observeState() {
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {

            binding.progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);

            if (state.error) {
                binding.errorLayout.setVisibility(View.VISIBLE);
                binding.txtError.setText(state.errorMessage);
                binding.layoutEmptyState.setVisibility(View.GONE);
                binding.recyclerHistory.setVisibility(View.GONE);
            } else if (state.rides == null || state.rides.isEmpty()) {
                binding.errorLayout.setVisibility(View.GONE);
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerHistory.setVisibility(View.GONE);
            } else {
                binding.errorLayout.setVisibility(View.GONE);
                binding.layoutEmptyState.setVisibility(View.GONE);
                binding.recyclerHistory.setVisibility(View.VISIBLE);
            }

            // Update adapter with sorted rides
            adapter.submitList(state.rides);

            // Update sort direction button text
            if (state.sortAscending) {
                binding.btnSortDirection.setText("↑");
            } else {
                binding.btnSortDirection.setText("↓");
            }

            // Update spinner selection based on sort field
            for (int i = 0; i < sortValues.length; i++) {
                if (sortValues[i].equals(state.sortField)) {
                    binding.spinnerSortField.setSelection(i);
                    break;
                }
            }
        });
    }

    @Override
    public void onRideClick(AdminRideUIModel ride) {
        if (ride.id != null) {
            AdminRideDetailFragment detailFragment = AdminRideDetailFragment.newInstance(ride.id);
            detailFragment.show(getParentFragmentManager(), "AdminRideDetailDialog");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register shake detector
        if (sensorManager != null && accelerometer != null && shakeDetector != null) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister shake detector
        if (sensorManager != null && shakeDetector != null) {
            sensorManager.unregisterListener(shakeDetector);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
