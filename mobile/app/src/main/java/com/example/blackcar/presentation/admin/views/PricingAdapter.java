package com.example.blackcar.presentation.admin.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blackcar.R;
import com.example.blackcar.data.api.model.PricingResponse;

import java.util.Locale;

public class PricingAdapter extends ListAdapter<PricingResponse, PricingAdapter.PricingHolder> {

    public interface OnEditPricingListener {
        void onEditClick(PricingResponse pricing);
    }

    private final OnEditPricingListener listener;

    public PricingAdapter(OnEditPricingListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<PricingResponse> DIFF_CALLBACK = new DiffUtil.ItemCallback<PricingResponse>() {
        @Override
        public boolean areItemsTheSame(@NonNull PricingResponse oldItem, @NonNull PricingResponse newItem) {
            return oldItem.getVehicleType().equals(newItem.getVehicleType());
        }

        @Override
        public boolean areContentsTheSame(@NonNull PricingResponse oldItem, @NonNull PricingResponse newItem) {
            return oldItem.getBasePrice() == newItem.getBasePrice() && 
                   oldItem.getPricePerKm() == newItem.getPricePerKm();
        }
    };

    @NonNull
    @Override
    public PricingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pricing_card, parent, false);
        return new PricingHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PricingHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class PricingHolder extends RecyclerView.ViewHolder {
        TextView txtVehicleIcon, txtVehicleType, txtBasePrice, txtPricePerKm;
        Button btnEdit;

        PricingHolder(@NonNull View itemView) {
            super(itemView);
            txtVehicleIcon = itemView.findViewById(R.id.txtVehicleIcon);
            txtVehicleType = itemView.findViewById(R.id.txtVehicleType);
            txtBasePrice = itemView.findViewById(R.id.txtBasePrice);
            txtPricePerKm = itemView.findViewById(R.id.txtPricePerKm);
            btnEdit = itemView.findViewById(R.id.btnEditBasePrice);
        }

        void bind(PricingResponse pricing) {
            txtVehicleType.setText(pricing.getVehicleType());
            txtBasePrice.setText(String.format(Locale.getDefault(), "%.2f RSD", pricing.getBasePrice()));
            txtPricePerKm.setText(String.format(Locale.getDefault(), "%.2f RSD", pricing.getPricePerKm()));
            
            String icon = "🚗";
            if ("VAN".equalsIgnoreCase(pricing.getVehicleType())) icon = "🚐";
            else if ("LUXURY".equalsIgnoreCase(pricing.getVehicleType())) icon = "✨";
            txtVehicleIcon.setText(icon);

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(pricing);
            });
        }
    }
}
