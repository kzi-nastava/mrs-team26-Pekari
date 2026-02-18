package com.example.blackcar.presentation.home.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.blackcar.R;
import com.example.blackcar.data.api.model.ActiveRideResponse;
import com.example.blackcar.data.api.model.LocationPoint;
import com.example.blackcar.databinding.FragmentHomeDriverBinding;
import com.example.blackcar.presentation.ViewModelFactory;
import com.example.blackcar.presentation.home.viewmodel.DriverHomeViewModel;

import java.util.List;

public class HomeDriverFragment extends Fragment {

    private FragmentHomeDriverBinding binding;
    private DriverHomeViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeDriverBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(requireContext()))
                .get(DriverHomeViewModel.class);

        setupButtons();
        observeState();

        viewModel.loadActiveRide();
        viewModel.startPolling();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.stopPolling();
        binding = null;
    }

    private void setupButtons() {
        binding.btnRetry.setOnClickListener(v -> {
            viewModel.clearError();
            viewModel.loadActiveRide();
        });

        binding.btnStartRide.setOnClickListener(v -> {
            viewModel.startRide();
        });

        binding.btnCompleteRide.setOnClickListener(v -> {
            viewModel.completeRide();
        });

        binding.btnStopEarly.setOnClickListener(v -> {
            viewModel.stopRideEarly();
        });

        binding.btnPanic.setOnClickListener(v -> {
            viewModel.activatePanic();
        });

        binding.btnCancelRide.setOnClickListener(v -> {
            viewModel.cancelRide();
        });
    }

    private void observeState() {
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            // Hide all views first
            binding.layoutLoading.setVisibility(View.GONE);
            binding.layoutError.setVisibility(View.GONE);
            binding.layoutNoRide.setVisibility(View.GONE);
            binding.scrollActiveRide.setVisibility(View.GONE);

            if (state.loading) {
                binding.layoutLoading.setVisibility(View.VISIBLE);
            } else if (state.error) {
                binding.layoutError.setVisibility(View.VISIBLE);
                binding.txtError.setText(state.errorMessage);
            } else if (state.activeRide == null) {
                binding.layoutNoRide.setVisibility(View.VISIBLE);
            } else {
                binding.scrollActiveRide.setVisibility(View.VISIBLE);
                populateRideDetails(state.activeRide, state.actionInProgress, state.panicActivated);
            }
        });
    }

    private void populateRideDetails(ActiveRideResponse ride, boolean actionInProgress, boolean panicActivated) {
        // Ride Header
        binding.txtRideTitle.setText(getString(R.string.driver_active_ride, ride.getRideId()));

        String status = ride.getStatus();
        binding.txtStatus.setText(DriverHomeViewModel.getStatusLabel(status));
        applyStatusBadgeStyle(status);

        // Stop Request Notification
        boolean stopRequested = DriverHomeViewModel.isStopRequested(status);
        binding.layoutStopRequestNotification.setVisibility(stopRequested ? View.VISIBLE : View.GONE);

        // Route Information
        binding.txtPickupAddress.setText(ride.getPickup().getAddress());
        binding.txtDropoffAddress.setText(ride.getDropoff().getAddress());

        // Stops
        binding.containerStops.removeAllViews();
        List<LocationPoint> stops = ride.getStops();
        if (stops != null && !stops.isEmpty()) {
            for (int i = 0; i < stops.size(); i++) {
                View stopView = createStopView(stops.get(i), i + 1);
                binding.containerStops.addView(stopView);
            }
        }

        // Ride Details
        binding.txtVehicleType.setText(ride.getVehicleType());
        binding.txtDistance.setText(DriverHomeViewModel.formatDistance(ride.getDistanceKm()));
        binding.txtDuration.setText(DriverHomeViewModel.formatDuration(ride.getEstimatedDurationMinutes()));
        binding.txtPrice.setText(DriverHomeViewModel.formatPrice(ride.getEstimatedPrice()));

        // Conditional fields
        binding.itemBabyTransport.setVisibility(
                Boolean.TRUE.equals(ride.getBabyTransport()) ? View.VISIBLE : View.GONE);
        binding.itemPetTransport.setVisibility(
                Boolean.TRUE.equals(ride.getPetTransport()) ? View.VISIBLE : View.GONE);

        // Passengers
        List<ActiveRideResponse.PassengerInfo> passengers = ride.getPassengers();
        binding.txtPassengersTitle.setText(getString(R.string.driver_passengers, passengers != null ? passengers.size() : 0));
        binding.containerPassengers.removeAllViews();
        if (passengers != null) {
            for (ActiveRideResponse.PassengerInfo passenger : passengers) {
                View passengerView = createPassengerView(passenger);
                binding.containerPassengers.addView(passengerView);
            }
        }

        // Action Buttons
        boolean canStart = DriverHomeViewModel.canStartRide(status);
        boolean canComplete = DriverHomeViewModel.canCompleteRide(status);
        boolean canPanic = DriverHomeViewModel.canActivatePanic(status);
        boolean canCancel = DriverHomeViewModel.canCancelRide(status);

        binding.btnStartRide.setVisibility(canStart ? View.VISIBLE : View.GONE);
        binding.btnStartRide.setEnabled(!actionInProgress);

        binding.btnCompleteRide.setVisibility(canComplete && !stopRequested ? View.VISIBLE : View.GONE);
        binding.btnCompleteRide.setEnabled(!actionInProgress);

        binding.btnStopEarly.setVisibility(canComplete ? View.VISIBLE : View.GONE);
        binding.btnStopEarly.setEnabled(!actionInProgress);

        binding.btnPanic.setVisibility(canPanic ? View.VISIBLE : View.GONE);
        binding.btnPanic.setEnabled(!actionInProgress && !panicActivated);
        if (panicActivated) {
            binding.btnPanic.setText(R.string.driver_panic_activated);
            binding.btnPanic.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.btn_panic_disabled));
        } else {
            binding.btnPanic.setText(R.string.driver_btn_panic);
            binding.btnPanic.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.btn_panic));
        }

        binding.btnCancelRide.setVisibility(canCancel ? View.VISIBLE : View.GONE);
        binding.btnCancelRide.setEnabled(!actionInProgress);
    }

    private void applyStatusBadgeStyle(String status) {
        int backgroundRes;
        int textColorRes;

        switch (status) {
            case "ACCEPTED":
                backgroundRes = R.drawable.bg_status_accepted;
                textColorRes = R.color.status_accepted;
                break;
            case "SCHEDULED":
                backgroundRes = R.drawable.bg_status_scheduled;
                textColorRes = R.color.status_scheduled;
                break;
            case "IN_PROGRESS":
                backgroundRes = R.drawable.bg_status_in_progress;
                textColorRes = R.color.status_in_progress;
                break;
            case "STOP_REQUESTED":
                backgroundRes = R.drawable.bg_status_stop_requested;
                textColorRes = R.color.status_stop_requested;
                break;
            case "CANCELLED":
            case "REJECTED":
                backgroundRes = R.drawable.bg_status_cancelled;
                textColorRes = R.color.status_cancelled;
                break;
            default:
                backgroundRes = R.drawable.bg_status_accepted;
                textColorRes = R.color.text_secondary;
        }

        binding.txtStatus.setBackgroundResource(backgroundRes);
        binding.txtStatus.setTextColor(ContextCompat.getColor(requireContext(), textColorRes));
    }

    private View createStopView(LocationPoint stop, int number) {
        LinearLayout stopLayout = new LinearLayout(requireContext());
        stopLayout.setOrientation(LinearLayout.HORIZONTAL);
        stopLayout.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        stopLayout.setBackgroundResource(R.drawable.bg_location_row);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, dpToPx(8));
        stopLayout.setLayoutParams(layoutParams);

        // Icon
        TextView icon = new TextView(requireContext());
        icon.setText("📍");
        icon.setTextSize(18);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        iconParams.setMarginEnd(dpToPx(12));
        iconParams.topMargin = dpToPx(2);
        icon.setLayoutParams(iconParams);
        stopLayout.addView(icon);

        // Content
        LinearLayout contentLayout = new LinearLayout(requireContext());
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        contentLayout.setLayoutParams(contentParams);

        TextView label = new TextView(requireContext());
        label.setText(getString(R.string.driver_stop, number));
        label.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        label.setTextSize(13);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.bottomMargin = dpToPx(4);
        label.setLayoutParams(labelParams);
        contentLayout.addView(label);

        TextView address = new TextView(requireContext());
        address.setText(stop.getAddress());
        address.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        address.setTextSize(14);
        contentLayout.addView(address);

        stopLayout.addView(contentLayout);
        return stopLayout;
    }

    private View createPassengerView(ActiveRideResponse.PassengerInfo passenger) {
        LinearLayout passengerLayout = new LinearLayout(requireContext());
        passengerLayout.setOrientation(LinearLayout.HORIZONTAL);
        passengerLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        passengerLayout.setBackgroundResource(R.drawable.bg_info_item);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, dpToPx(8));
        passengerLayout.setLayoutParams(layoutParams);

        // Avatar
        TextView avatar = new TextView(requireContext());
        String initial = passenger.getName() != null && !passenger.getName().isEmpty()
                ? passenger.getName().substring(0, 1).toUpperCase()
                : "?";
        avatar.setText(initial);
        avatar.setTextSize(14);
        avatar.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        avatar.setTypeface(null, android.graphics.Typeface.BOLD);
        avatar.setGravity(android.view.Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_passenger_avatar);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
                dpToPx(36),
                dpToPx(36)
        );
        avatarParams.setMarginEnd(dpToPx(12));
        avatar.setLayoutParams(avatarParams);
        passengerLayout.addView(avatar);

        // Info
        LinearLayout infoLayout = new LinearLayout(requireContext());
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        infoLayout.setLayoutParams(infoParams);

        TextView name = new TextView(requireContext());
        name.setText(passenger.getName());
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        name.setTextSize(14);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nameParams.bottomMargin = dpToPx(2);
        name.setLayoutParams(nameParams);
        infoLayout.addView(name);

        TextView phone = new TextView(requireContext());
        phone.setText(passenger.getPhoneNumber());
        phone.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        phone.setTextSize(12);
        LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        phoneParams.topMargin = dpToPx(2);
        phone.setLayoutParams(phoneParams);
        infoLayout.addView(phone);

        TextView email = new TextView(requireContext());
        email.setText(passenger.getEmail());
        email.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        email.setTextSize(12);
        infoLayout.addView(email);

        passengerLayout.addView(infoLayout);
        return passengerLayout;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
