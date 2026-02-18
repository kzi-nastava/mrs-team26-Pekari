package com.example.blackcar.presentation.home.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blackcar.R;
import com.example.blackcar.data.api.model.GeocodeResult;

public class AddressAutocompleteAdapter extends ListAdapter<GeocodeResult, AddressAutocompleteAdapter.ViewHolder> {

    public interface OnAddressSelectedListener {
        void onAddressSelected(GeocodeResult result);
    }

    private OnAddressSelectedListener listener;

    public AddressAutocompleteAdapter() {
        super(new DiffUtil.ItemCallback<GeocodeResult>() {
            @Override
            public boolean areItemsTheSame(@NonNull GeocodeResult oldItem, @NonNull GeocodeResult newItem) {
                return oldItem.getDisplayName().equals(newItem.getDisplayName())
                        && oldItem.getLatitude() == newItem.getLatitude()
                        && oldItem.getLongitude() == newItem.getLongitude();
            }

            @Override
            public boolean areContentsTheSame(@NonNull GeocodeResult oldItem, @NonNull GeocodeResult newItem) {
                return areItemsTheSame(oldItem, newItem);
            }
        });
    }

    public void setOnAddressSelectedListener(OnAddressSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_address_suggestion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GeocodeResult item = getItem(position);
        holder.txtAddress.setText(item.getDisplayName());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddressSelected(item);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtAddress;

        ViewHolder(View itemView) {
            super(itemView);
            txtAddress = itemView.findViewById(R.id.txtAddress);
        }
    }
}
