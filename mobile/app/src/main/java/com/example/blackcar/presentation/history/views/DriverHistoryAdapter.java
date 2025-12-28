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

import java.util.Locale;

public class DriverHistoryAdapter extends ListAdapter<RideUIModel, DriverHistoryAdapter.Holder> {

    public DriverHistoryAdapter() {
        super(DIFF);
    }

    static DiffUtil.ItemCallback<RideUIModel> DIFF = new DiffUtil.ItemCallback<RideUIModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull RideUIModel oldItem, @NonNull RideUIModel newItem) {
            return oldItem.startTime != null && newItem.startTime != null &&
                   oldItem.endTime != null && newItem.endTime != null &&
                   oldItem.startTime.equals(newItem.startTime) &&
                   oldItem.endTime.equals(newItem.endTime);
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
                .inflate(R.layout.item_driver_history, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        RideUIModel ride = getItem(position);
        if (ride != null) {
            holder.bind(ride);
        }
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView txtRoute, txtTime, txtPrice, txtCanceled, txtPanic, txtPassengers;

        public Holder(@NonNull View itemView) {
            super(itemView);

            txtRoute = itemView.findViewById(R.id.txtRoute);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtCanceled = itemView.findViewById(R.id.txtCanceled);
            txtPanic = itemView.findViewById(R.id.txtPanic);
            txtPassengers = itemView.findViewById(R.id.txtPassengers);
        }

        public void bind(RideUIModel ride) {
            String route = (ride.origin != null ? ride.origin : "Unknown") +
                          " → " +
                          (ride.destination != null ? ride.destination : "Unknown");
            txtRoute.setText(route);

            String timeDisplay = formatTime(ride.startTime) + " - " + formatTime(ride.endTime);
            txtTime.setText(timeDisplay);

            // Price display (always show, 0 for canceled rides)
            txtPrice.setText(String.format(Locale.getDefault(), "%.2f RSD", ride.price));

            // Passengers display with null safety
            if (ride.passengers != null && !ride.passengers.isEmpty()) {
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

            // Panic indicator
            txtPanic.setVisibility(ride.panic ? View.VISIBLE : View.GONE);
        }


        private String formatTime(String timeString) {
            if (timeString == null || timeString.isEmpty()) {
                return "--:--";
            }

            try {
                String[] parts = timeString.split(" ");
                if (parts.length >= 2) {
                    return parts[1];
                }
                return timeString;
            } catch (Exception e) {
                return timeString;
            }
        }
    }
}
