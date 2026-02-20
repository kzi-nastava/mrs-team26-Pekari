package com.example.blackcar.presentation.admin.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blackcar.R;
import com.example.blackcar.data.api.model.DriverRideHistoryResponse;
import com.example.blackcar.presentation.admin.viewmodel.PanicPanelViewModel;

import java.util.List;

public class PanicRideAdapter extends ListAdapter<DriverRideHistoryResponse, PanicRideAdapter.PanicRideViewHolder> {

    public PanicRideAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<DriverRideHistoryResponse> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DriverRideHistoryResponse>() {
                @Override
                public boolean areItemsTheSame(@NonNull DriverRideHistoryResponse oldItem,
                                               @NonNull DriverRideHistoryResponse newItem) {
                    return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull DriverRideHistoryResponse oldItem,
                                                  @NonNull DriverRideHistoryResponse newItem) {
                    return oldItem.getId() != null && oldItem.getId().equals(newItem.getId())
                            && (oldItem.getStatus() != null ? oldItem.getStatus().equals(newItem.getStatus()) : newItem.getStatus() == null);
                }
            };

    @NonNull
    @Override
    public PanicRideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_panic_ride, parent, false);
        return new PanicRideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PanicRideViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class PanicRideViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtRideId;
        private final TextView txtStatus;
        private final TextView txtPickupAddress;
        private final TextView txtDropoffAddress;
        private final TextView txtStartTime;
        private final TextView txtPanickedBy;
        private final View layoutPanickedBy;
        private final TextView txtCancelledBy;
        private final View layoutCancelledBy;
        private final LinearLayout passengersContainer;
        private final TextView txtPassengersHeader;

        PanicRideViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRideId = itemView.findViewById(R.id.txtRideId);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtPickupAddress = itemView.findViewById(R.id.txtPickupAddress);
            txtDropoffAddress = itemView.findViewById(R.id.txtDropoffAddress);
            txtStartTime = itemView.findViewById(R.id.txtStartTime);
            txtPanickedBy = itemView.findViewById(R.id.txtPanickedBy);
            layoutPanickedBy = itemView.findViewById(R.id.layoutPanickedBy);
            txtCancelledBy = itemView.findViewById(R.id.txtCancelledBy);
            layoutCancelledBy = itemView.findViewById(R.id.layoutCancelledBy);
            passengersContainer = itemView.findViewById(R.id.passengersContainer);
            txtPassengersHeader = itemView.findViewById(R.id.txtPassengersHeader);
        }

        void bind(DriverRideHistoryResponse ride) {
            // Ride ID
            txtRideId.setText(String.format("Ride #%d", ride.getId()));

            // Status badge
            txtStatus.setText(PanicPanelViewModel.getStatusLabel(ride.getStatus()));
            setStatusBackground(ride.getStatus());

            // Route info
            txtPickupAddress.setText(ride.getPickupLocation() != null ? ride.getPickupLocation() : "N/A");
            txtDropoffAddress.setText(ride.getDropoffLocation() != null ? ride.getDropoffLocation() : "N/A");

            // Start time
            txtStartTime.setText(PanicPanelViewModel.formatDateTime(ride.getStartTime()));

            // Panicked by
            if (ride.getPanickedBy() != null && !ride.getPanickedBy().isEmpty()) {
                layoutPanickedBy.setVisibility(View.VISIBLE);
                txtPanickedBy.setText(ride.getPanickedBy());
            } else {
                layoutPanickedBy.setVisibility(View.GONE);
            }

            // Cancelled by
            if (ride.getCancelled() != null && ride.getCancelled()) {
                layoutCancelledBy.setVisibility(View.VISIBLE);
                txtCancelledBy.setText(ride.getCancelledBy() != null ? ride.getCancelledBy() : "Unknown");
            } else {
                layoutCancelledBy.setVisibility(View.GONE);
            }

            // Passengers
            List<DriverRideHistoryResponse.PassengerInfo> passengers = ride.getPassengers();
            passengersContainer.removeAllViews();
            if (passengers != null && !passengers.isEmpty()) {
                txtPassengersHeader.setVisibility(View.VISIBLE);
                txtPassengersHeader.setText(String.format("Passengers (%d)", passengers.size()));
                passengersContainer.setVisibility(View.VISIBLE);

                for (DriverRideHistoryResponse.PassengerInfo passenger : passengers) {
                    View passengerView = LayoutInflater.from(itemView.getContext())
                            .inflate(R.layout.item_panic_passenger, passengersContainer, false);

                    TextView txtAvatar = passengerView.findViewById(R.id.txtPassengerAvatar);
                    TextView txtName = passengerView.findViewById(R.id.txtPassengerName);
                    TextView txtEmail = passengerView.findViewById(R.id.txtPassengerEmail);

                    String firstName = passenger.getFirstName() != null ? passenger.getFirstName() : "";
                    String lastName = passenger.getLastName() != null ? passenger.getLastName() : "";

                    txtAvatar.setText(firstName.isEmpty() ? "?" : firstName.substring(0, 1).toUpperCase());
                    txtName.setText(String.format("%s %s", firstName, lastName).trim());
                    txtEmail.setText(passenger.getEmail() != null ? passenger.getEmail() : "");

                    passengersContainer.addView(passengerView);
                }
            } else {
                txtPassengersHeader.setVisibility(View.GONE);
                passengersContainer.setVisibility(View.GONE);
            }
        }

        private void setStatusBackground(String status) {
            int backgroundRes;
            int textColor;
            if (status == null) {
                backgroundRes = R.drawable.bg_status_default;
                textColor = itemView.getContext().getColor(R.color.text_secondary);
            } else {
                switch (status) {
                    case "ACCEPTED":
                        backgroundRes = R.drawable.bg_status_assigned;
                        textColor = itemView.getContext().getColor(R.color.status_assigned);
                        break;
                    case "SCHEDULED":
                        backgroundRes = R.drawable.bg_status_scheduled;
                        textColor = itemView.getContext().getColor(R.color.status_scheduled);
                        break;
                    case "IN_PROGRESS":
                        backgroundRes = R.drawable.bg_status_in_progress;
                        textColor = itemView.getContext().getColor(R.color.status_in_progress);
                        break;
                    case "STOP_REQUESTED":
                        backgroundRes = R.drawable.bg_status_stop_requested;
                        textColor = itemView.getContext().getColor(R.color.status_stop_requested);
                        break;
                    case "COMPLETED":
                        backgroundRes = R.drawable.bg_status_completed;
                        textColor = itemView.getContext().getColor(R.color.text_secondary);
                        break;
                    case "CANCELLED":
                        backgroundRes = R.drawable.bg_status_cancelled;
                        textColor = itemView.getContext().getColor(R.color.panic_red);
                        break;
                    default:
                        backgroundRes = R.drawable.bg_status_default;
                        textColor = itemView.getContext().getColor(R.color.text_secondary);
                }
            }
            txtStatus.setBackgroundResource(backgroundRes);
            txtStatus.setTextColor(textColor);
        }
    }
}
