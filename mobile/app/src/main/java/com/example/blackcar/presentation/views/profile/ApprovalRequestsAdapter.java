package com.example.blackcar.presentation.views.profile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blackcar.databinding.ItemApprovalRequestBinding;
import com.example.blackcar.domain.model.ApprovalRequest;
import com.example.blackcar.domain.model.ProfileUpdateRequest;

import java.util.ArrayList;
import java.util.List;

final class ApprovalRequestsAdapter extends RecyclerView.Adapter<ApprovalRequestsAdapter.VH> {

    interface OnApprovalAction {
        void onAction(@NonNull ApprovalRequest request);
    }

    private final OnApprovalAction onApprove;
    private final OnApprovalAction onReject;
    private final List<ApprovalRequest> items = new ArrayList<>();

    ApprovalRequestsAdapter(@NonNull OnApprovalAction onApprove, @NonNull OnApprovalAction onReject) {
        this.onApprove = onApprove;
        this.onReject = onReject;
    }

    void submit(@NonNull List<ApprovalRequest> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemApprovalRequestBinding binding = ItemApprovalRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ApprovalRequest request = items.get(position);
        holder.bind(request, onApprove, onReject);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class VH extends RecyclerView.ViewHolder {
        private final ItemApprovalRequestBinding binding;

        VH(@NonNull ItemApprovalRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ApprovalRequest request, @NonNull OnApprovalAction onApprove, @NonNull OnApprovalAction onReject) {
            binding.requestTitle.setText("User ID: " + request.getUserId());

            ProfileUpdateRequest c = request.getChanges();
            String body =
                    "First name: " + c.getFirstName() + "\n" +
                            "Last name: " + c.getLastName() + "\n" +
                            "Phone: " + c.getPhoneNumber() + "\n" +
                            "Address: " + c.getAddress();
            binding.requestBody.setText(body);

            binding.btnApprove.setOnClickListener(v -> onApprove.onAction(request));
            binding.btnReject.setOnClickListener(v -> onReject.onAction(request));
        }
    }
}
