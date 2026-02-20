package com.example.blackcar.presentation.history.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blackcar.R;
import com.example.blackcar.presentation.history.viewstate.AdminRideUIModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminHistoryAdapter extends ListAdapter<AdminRideUIModel, AdminHistoryAdapter.Holder> {

    public interface OnRideClickListener {
        void onRideClick(AdminRideUIModel ride);
    }

    private final OnRideClickListener listener;

    public AdminHistoryAdapter(OnRideClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    static DiffUtil.ItemCallback<AdminRideUIModel> DIFF = new DiffUtil.ItemCallback<AdminRideUIModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull AdminRideUIModel oldItem, @NonNull AdminRideUIModel newItem) {
            return oldItem.id != null && newItem.id != null && oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull AdminRideUIModel oldItem, @NonNull AdminRideUIModel newItem) {
            return oldItem.equals(newItem);
        }
    };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_history, parent, false);
        return new Holder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AdminRideUIModel ride = getItem(position);
        if (ride != null) {
            holder.bind(ride);
        }
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView txtRoute, txtCreatedAt, txtStartedAt, txtCompletedAt;
        TextView txtStatus, txtPrice, txtDistance, txtVehicleType;
        TextView txtDriver, txtPassengerCount, txtCancelled, txtPanic;
        private AdminRideUIModel currentRide;

        public Holder(@NonNull View itemView, OnRideClickListener listener) {
            super(itemView);

            txtRoute = itemView.findViewById(R.id.txtRoute);
            txtCreatedAt = itemView.findViewById(R.id.txtCreatedAt);
            txtStartedAt = itemView.findViewById(R.id.txtStartedAt);
            txtCompletedAt = itemView.findViewById(R.id.txtCompletedAt);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtDistance = itemView.findViewById(R.id.txtDistance);
            txtVehicleType = itemView.findViewById(R.id.txtVehicleType);
            txtDriver = itemView.findViewById(R.id.txtDriver);
            txtPassengerCount = itemView.findViewById(R.id.txtPassengerCount);
            txtCancelled = itemView.findViewById(R.id.txtCancelled);
            txtPanic = itemView.findViewById(R.id.txtPanic);

            itemView.setOnClickListener(v -> {
                if (listener != null && currentRide != null) {
                    listener.onRideClick(currentRide);
                }
            });
        }

        public void bind(AdminRideUIModel ride) {
            this.currentRide = ride;

            // Route
            String pickup = ride.pickupAddress != null ? ride.pickupAddress : "Unknown";
            String dropoff = ride.dropoffAddress != null ? ride.dropoffAddress : "Unknown";
            txtRoute.setText(pickup + " → " + dropoff);

            // Dates
            txtCreatedAt.setText(formatDateTimeShort(ride.createdAt));
            txtStartedAt.setText(formatDateTimeShort(ride.startedAt));
            txtCompletedAt.setText(formatDateTimeShort(ride.completedAt));

            // Status with color
            if (ride.status != null) {
                txtStatus.setText(ride.status);
                if (ride.panicActivated) {
                    txtStatus.setText("PANIC");
                    txtStatus.setTextColor(itemView.getContext().getColor(R.color.accent_danger));
                } else if (ride.cancelled) {
                    txtStatus.setText("CANCELLED");
                    txtStatus.setTextColor(itemView.getContext().getColor(R.color.accent_danger));
                } else if ("COMPLETED".equals(ride.status)) {
                    txtStatus.setTextColor(itemView.getContext().getColor(R.color.accent_success));
                } else if ("IN_PROGRESS".equals(ride.status) || "STOP_REQUESTED".equals(ride.status)) {
                    txtStatus.setTextColor(itemView.getContext().getColor(R.color.accent_info));
                } else if ("ACCEPTED".equals(ride.status) || "SCHEDULED".equals(ride.status)) {
                    txtStatus.setTextColor(itemView.getContext().getColor(R.color.status_accepted));
                } else {
                    txtStatus.setTextColor(itemView.getContext().getColor(R.color.text_tertiary));
                }
            }

            // Price
            txtPrice.setText(String.format(Locale.getDefault(), "%.2f RSD", ride.price));

            // Distance
            if (ride.distanceKm != null && ride.distanceKm > 0) {
                txtDistance.setVisibility(View.VISIBLE);
                txtDistance.setText(String.format(Locale.getDefault(), "%.1f km", ride.distanceKm));
            } else {
                txtDistance.setVisibility(View.GONE);
            }

            // Vehicle Type
            if (ride.vehicleType != null && !ride.vehicleType.isEmpty()) {
                txtVehicleType.setVisibility(View.VISIBLE);
                txtVehicleType.setText(ride.vehicleType);
            } else {
                txtVehicleType.setVisibility(View.GONE);
            }

            // Driver
            txtDriver.setText("Driver: " + (ride.driverName != null ? ride.driverName : "No driver"));

            // Passenger count
            txtPassengerCount.setText(ride.passengerCount + " passenger" + (ride.passengerCount != 1 ? "s" : ""));

            // Cancelled indicator
            if (ride.cancelled && ride.cancelledBy != null) {
                txtCancelled.setVisibility(View.VISIBLE);
                txtCancelled.setText("Cancelled by " + ride.cancelledBy);
            } else {
                txtCancelled.setVisibility(View.GONE);
            }

            // Panic indicator
            txtPanic.setVisibility(ride.panicActivated ? View.VISIBLE : View.GONE);
        }

        private String formatDateTimeShort(String dateTimeString) {
            if (dateTimeString == null || dateTimeString.isEmpty()) {
                return "--";
            }

            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
                Date date = isoFormat.parse(dateTimeString);
                return date != null ? outputFormat.format(date) : "--";
            } catch (Exception e) {
                // Try alternative format
                try {
                    SimpleDateFormat altFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
                    Date date = altFormat.parse(dateTimeString);
                    return date != null ? outputFormat.format(date) : "--";
                } catch (Exception ex) {
                    return "--";
                }
            }
        }
    }
}
