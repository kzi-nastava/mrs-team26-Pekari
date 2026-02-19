package com.example.blackcar.presentation.home.views;

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
import com.example.blackcar.data.api.model.FavoriteRouteResponse;
import com.example.blackcar.data.api.model.LocationPoint;

public class FavoriteRoutesAdapter extends ListAdapter<FavoriteRouteResponse, FavoriteRoutesAdapter.Holder> {

    public interface OnRouteClickListener {
        void onRouteClick(FavoriteRouteResponse route);
    }

    private final OnRouteClickListener listener;

    public FavoriteRoutesAdapter(OnRouteClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    static DiffUtil.ItemCallback<FavoriteRouteResponse> DIFF = new DiffUtil.ItemCallback<FavoriteRouteResponse>() {
        @Override
        public boolean areItemsTheSame(@NonNull FavoriteRouteResponse oldItem, @NonNull FavoriteRouteResponse newItem) {
            return oldItem.getId() != null && newItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull FavoriteRouteResponse oldItem, @NonNull FavoriteRouteResponse newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
    };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_route, parent, false);
        return new Holder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView txtRouteName, txtFrom, txtTo, txtVehicleType, txtBabyBadge, txtPetBadge;
        LinearLayout containerStops;

        Holder(@NonNull View itemView, OnRouteClickListener listener) {
            super(itemView);
            txtRouteName = itemView.findViewById(R.id.txtRouteName);
            txtFrom = itemView.findViewById(R.id.txtFrom);
            txtTo = itemView.findViewById(R.id.txtTo);
            txtVehicleType = itemView.findViewById(R.id.txtVehicleType);
            txtBabyBadge = itemView.findViewById(R.id.txtBabyBadge);
            txtPetBadge = itemView.findViewById(R.id.txtPetBadge);
            containerStops = itemView.findViewById(R.id.containerStops);

            itemView.setOnClickListener(v -> {
                Object tag = itemView.getTag();
                if (listener != null && tag instanceof FavoriteRouteResponse) {
                    listener.onRouteClick((FavoriteRouteResponse) tag);
                }
            });
        }

        void bind(FavoriteRouteResponse route) {
            itemView.setTag(route);

            String pickupAddr = route.getPickup() != null ? route.getPickup().getAddress() : "";
            String dropoffAddr = route.getDropoff() != null ? route.getDropoff().getAddress() : "";
            String name = route.getName() != null && !route.getName().isEmpty()
                    ? route.getName()
                    : (pickupAddr + " → " + dropoffAddr);

            txtRouteName.setText(name);
            txtFrom.setText("From: " + pickupAddr);
            txtTo.setText("To: " + dropoffAddr);
            txtVehicleType.setText(route.getVehicleType() != null ? route.getVehicleType() : "STANDARD");

            txtBabyBadge.setVisibility(route.getBabyTransport() != null && route.getBabyTransport() ? View.VISIBLE : View.GONE);
            txtPetBadge.setVisibility(route.getPetTransport() != null && route.getPetTransport() ? View.VISIBLE : View.GONE);

            containerStops.removeAllViews();
            if (route.getStops() != null && !route.getStops().isEmpty()) {
                containerStops.setVisibility(View.VISIBLE);
                for (int i = 0; i < route.getStops().size(); i++) {
                    LocationPoint stop = route.getStops().get(i);
                    if (stop != null) {
                        TextView stopTv = new TextView(itemView.getContext());
                        stopTv.setText("Stop " + (i + 1) + ": " + stop.getAddress());
                        stopTv.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                        stopTv.setTextSize(12);
                        stopTv.setPadding(0, 4, 0, 0);
                        containerStops.addView(stopTv);
                    }
                }
            } else {
                containerStops.setVisibility(View.GONE);
            }
        }
    }
}
