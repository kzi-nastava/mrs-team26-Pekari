package com.example.blackcar.presentation.admin.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.blackcar.R;
import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.databinding.FragmentAddDriverBinding;
import com.example.blackcar.presentation.ViewModelFactory;
import com.example.blackcar.presentation.admin.viewmodel.AddDriverViewModel;
import com.example.blackcar.presentation.admin.viewstate.AddDriverViewState;

import java.util.Arrays;
import java.util.List;

public class AddDriverFragment extends Fragment {

    private static final List<String> VEHICLE_TYPE_VALUES = Arrays.asList("STANDARD", "VAN", "LUX");
    private static final int SEATS_MIN = 1;
    private static final int SEATS_MAX = 8;

    private FragmentAddDriverBinding binding;
    private AddDriverViewModel viewModel;
    private String selectedVehicleType = "STANDARD";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddDriverBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ApiClient.init(requireContext());
        ViewModelFactory factory = new ViewModelFactory(requireContext());
        viewModel = new ViewModelProvider(this, factory).get(AddDriverViewModel.class);

        setupVehicleTypeSpinner();
        setupClickListeners();
        observeViewModel();
    }

    private void setupVehicleTypeSpinner() {
        String[] labels = new String[]{
                getString(R.string.add_driver_vehicle_type_standard),
                getString(R.string.add_driver_vehicle_type_van),
                getString(R.string.add_driver_vehicle_type_lux)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.addDriverVehicleType.setAdapter(adapter);
        binding.addDriverVehicleType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedVehicleType = VEHICLE_TYPE_VALUES.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedVehicleType = "STANDARD";
            }
        });
    }

    private void setupClickListeners() {
        binding.addDriverSubmit.setOnClickListener(v -> submitIfValid());
    }

    private void submitIfValid() {
        hideMessages();
        String firstName = getText(binding.addDriverFirstName);
        String lastName = getText(binding.addDriverLastName);
        String email = getText(binding.addDriverEmail);
        String address = getText(binding.addDriverAddress);
        String phone = getText(binding.addDriverPhone);
        String vehicleModel = getText(binding.addDriverVehicleModel);
        String licensePlate = getText(binding.addDriverLicensePlate);
        String seatsStr = getText(binding.addDriverSeats);
        boolean babyFriendly = binding.addDriverBabyFriendly.isChecked();
        boolean petFriendly = binding.addDriverPetFriendly.isChecked();

        String error = validate(firstName, lastName, email, address, phone, vehicleModel, licensePlate, seatsStr);
        if (error != null) {
            showError(error);
            return;
        }

        int seats = Integer.parseInt(seatsStr);
        viewModel.registerDriver(email, firstName, lastName, address, phone,
                vehicleModel, selectedVehicleType, licensePlate, seats, babyFriendly, petFriendly);
    }

    private String getText(android.widget.EditText edit) {
        return edit != null && edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private String validate(String firstName, String lastName, String email, String address,
                            String phone, String vehicleModel, String licensePlate, String seatsStr) {
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || address.isEmpty()
                || phone.isEmpty() || vehicleModel.isEmpty() || licensePlate.isEmpty() || seatsStr.isEmpty()) {
            return getString(R.string.add_driver_validation_required);
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return getString(R.string.add_driver_validation_email);
        }
        String cleanPhone = phone.replaceAll("[^0-9+]", "");
        if (cleanPhone.length() < 10 || cleanPhone.length() > 15) {
            return getString(R.string.add_driver_validation_phone);
        }
        int seats;
        try {
            seats = Integer.parseInt(seatsStr);
        } catch (NumberFormatException e) {
            return getString(R.string.add_driver_validation_seats);
        }
        if (seats < SEATS_MIN || seats > SEATS_MAX) {
            return getString(R.string.add_driver_validation_seats);
        }
        return null;
    }

    private void observeViewModel() {
        viewModel.getAddDriverState().observe(getViewLifecycleOwner(), state -> {
            if (state instanceof AddDriverViewState.Idle) {
                binding.addDriverSubmit.setEnabled(true);
            } else if (state instanceof AddDriverViewState.Loading) {
                binding.addDriverSubmit.setEnabled(false);
                hideMessages();
            } else if (state instanceof AddDriverViewState.Success) {
                binding.addDriverSubmit.setEnabled(true);
                AddDriverViewState.Success success = (AddDriverViewState.Success) state;
                showSuccess(success.getMessage());
                clearForm();
            } else if (state instanceof AddDriverViewState.Error) {
                binding.addDriverSubmit.setEnabled(true);
                AddDriverViewState.Error error = (AddDriverViewState.Error) state;
                showError(error.getMessage());
            }
        });
    }

    private void hideMessages() {
        binding.addDriverError.setVisibility(View.GONE);
        binding.addDriverSuccess.setVisibility(View.GONE);
        binding.addDriverError.setText("");
        binding.addDriverSuccess.setText("");
    }

    private void showError(String message) {
        binding.addDriverError.setText(message);
        binding.addDriverError.setVisibility(View.VISIBLE);
        binding.addDriverSuccess.setVisibility(View.GONE);
    }

    private void showSuccess(String message) {
        binding.addDriverSuccess.setText(message);
        binding.addDriverSuccess.setVisibility(View.VISIBLE);
        binding.addDriverError.setVisibility(View.GONE);
    }

    private void clearForm() {
        binding.addDriverFirstName.getText().clear();
        binding.addDriverLastName.getText().clear();
        binding.addDriverEmail.getText().clear();
        binding.addDriverAddress.getText().clear();
        binding.addDriverPhone.getText().clear();
        binding.addDriverVehicleModel.getText().clear();
        binding.addDriverLicensePlate.getText().clear();
        binding.addDriverSeats.getText().clear();
        binding.addDriverBabyFriendly.setChecked(false);
        binding.addDriverPetFriendly.setChecked(false);
        binding.addDriverVehicleType.setSelection(0);
    }
}
