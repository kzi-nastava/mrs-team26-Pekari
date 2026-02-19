package com.example.blackcar.presentation.history.views;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blackcar.R;
import com.example.blackcar.presentation.history.viewstate.RideUIModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PassengerHistoryAdapter extends ListAdapter<RideUIModel, PassengerHistoryAdapter.Holder> {

    public interface OnRideClickListener {
        void onRideClick(RideUIModel ride);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(RideUIModel ride);
    }

    private final OnRideClickListener listener;
    private final OnFavoriteClickListener favoriteListener;

    public PassengerHistoryAdapter(OnRideClickListener listener, OnFavoriteClickListener favoriteListener) {
        super(DIFF);
        this.listener = listener;
        this.favoriteListener = favoriteListener;
    }

    static DiffUtil.ItemCallback<RideUIModel> DIFF = new DiffUtil.ItemCallback<RideUIModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull RideUIModel oldItem, @NonNull RideUIModel newItem) {
            return oldItem.id != null && newItem.id != null && oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull RideUIModel oldItem, @NonNull RideUIModel newItem) {
            return oldItem.equals(newItem);
        }
    };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_passenger_history, parent, false);
        return new Holder(v, listener, favoriteListener);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        RideUIModel ride = getItem(position);
        if (ride != null) {
            holder.bind(ride);
        }
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView txtRoute, txtTime, txtPrice, txtCanceled, txtPassengers;
        TextView txtDistance, txtVehicleType, txtStatus;
        android.widget.Button btnFavorite;
        private RideUIModel currentRide;
        private final OnRideClickListener listener;
        private final OnFavoriteClickListener favoriteListener;

        public Holder(@NonNull View itemView, OnRideClickListener listener, OnFavoriteClickListener favoriteListener) {
            super(itemView);
            this.listener = listener;
            this.favoriteListener = favoriteListener;

            txtRoute = itemView.findViewById(R.id.txtRoute);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtCanceled = itemView.findViewById(R.id.txtCanceled);
            txtPassengers = itemView.findViewById(R.id.txtPassengers);
            txtDistance = itemView.findViewById(R.id.txtDistance);
            txtVehicleType = itemView.findViewById(R.id.txtVehicleType);
            txtStatus = itemView.findViewById(R.id.txtStatus);

            itemView.setOnClickListener(v -> {
                if (listener != null && currentRide != null) {
                    listener.onRideClick(currentRide);
                }
            });

            if (btnFavorite != null) {
                btnFavorite.setOnClickListener(v -> {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    if (favoriteListener != null && currentRide != null) {
                        favoriteListener.onFavoriteClick(currentRide);
                    }
                });
            }
        }

        public void bind(RideUIModel ride) {
            this.currentRide = ride;
            String route = (ride.origin != null ? ride.origin : "Unknown") +
                          " → " +
                          (ride.destination != null ? ride.destination : "Unknown");
            txtRoute.setText(route);

            String timeDisplay = formatDateTimeSimple(ride.startTime);
            txtTime.setText(timeDisplay);

            // Price display (always show, 0 for canceled rides)
            txtPrice.setText(String.format(Locale.getDefault(), "%.2f RSD", ride.price));

            // Driver display for passenger history
            if (ride.driverName != null && !ride.driverName.isEmpty()) {
                txtPassengers.setVisibility(View.VISIBLE);
                txtPassengers.setText("Driver: " + ride.driverName);
            } else if (ride.passengers != null && !ride.passengers.isEmpty()) {
                txtPassengers.setVisibility(View.VISIBLE);
                txtPassengers.setText("Passengers: " + TextUtils.join(", ", ride.passengers));
            } else {
                txtPassengers.setVisibility(View.GONE);
            }

            if (ride.canceledBy != null && !ride.canceledBy.isEmpty()) {
                txtCanceled.setVisibility(View.VISIBLE);
                txtCanceled.setText("Canceled by " + ride.canceledBy);
            } else {
                txtCanceled.setVisibility(View.GONE);
            }

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

            // Status
            if (ride.status != null && !ride.status.isEmpty()) {
                txtStatus.setVisibility(View.VISIBLE);
                txtStatus.setText(ride.status);
                // Color based on status
                if ("COMPLETED".equals(ride.status)) {
                    txtStatus.setTextColor(itemView.getContext().getColor(R.color.accent_success));
                } else if ("CANCELLED".equals(ride.status)) {
                    txtStatus.setTextColor(itemView.getContext().getColor(R.color.accent_danger));
                } else {
                    txtStatus.setTextColor(itemView.getContext().getColor(R.color.text_tertiary));
                }
            } else {
                txtStatus.setVisibility(View.GONE);
            }

            // Favorite button - show ★ when favorited, ☆ when not
            if (btnFavorite != null) {
                boolean isFavorite = ride.favoriteRouteId != null;
                btnFavorite.setText(isFavorite ? "★" : "☆");
                btnFavorite.setTextColor(itemView.getContext().getColor(
                        isFavorite ? R.color.accent_success : R.color.text_secondary));
                // Disable if ride has no coordinates (can't add to favorites)
                boolean canAdd = ride.pickup != null && ride.dropoff != null
                        && ride.pickup.getLatitude() != null && ride.pickup.getLongitude() != null
                        && ride.dropoff.getLatitude() != null && ride.dropoff.getLongitude() != null;
                btnFavorite.setEnabled(canAdd || isFavorite);
                btnFavorite.setContentDescription(itemView.getContext().getString(
                        isFavorite ? R.string.favorite_remove : R.string.favorite_add));
            }
        }


        private String formatDateTimeSimple(String dateTimeString) {
            if (dateTimeString == null || dateTimeString.isEmpty()) {
                return "No date";
            }

            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(dateTimeString);
                return date != null ? outputFormat.format(date) : dateTimeString;
            } catch (Exception e) {
                // Try alternative ISO format
                try {
                    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                    Date date = isoFormat.parse(dateTimeString);
                    return date != null ? outputFormat.format(date) : dateTimeString;
                } catch (Exception ex) {
                    return dateTimeString;
                }
            }
        }
    }
}
