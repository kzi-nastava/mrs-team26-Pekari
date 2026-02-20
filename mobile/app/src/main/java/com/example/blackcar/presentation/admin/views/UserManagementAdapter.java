package com.example.blackcar.presentation.admin.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blackcar.R;
import com.example.blackcar.data.api.model.UserListItemResponse;
import com.example.blackcar.presentation.admin.viewmodel.UserManagementViewModel;

public class UserManagementAdapter extends ListAdapter<UserListItemResponse, UserManagementAdapter.Holder> {

    public interface OnUserActionListener {
        void onBlockClick(UserListItemResponse user);
        void onUnblockClick(UserListItemResponse user);
    }

    private final OnUserActionListener listener;

    public UserManagementAdapter(OnUserActionListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    static final DiffUtil.ItemCallback<UserListItemResponse> DIFF = new DiffUtil.ItemCallback<UserListItemResponse>() {
        @Override
        public boolean areItemsTheSame(@NonNull UserListItemResponse oldItem, @NonNull UserListItemResponse newItem) {
            return oldItem.getId() != null && newItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull UserListItemResponse oldItem, @NonNull UserListItemResponse newItem) {
            return oldItem.getBlocked() == newItem.getBlocked()
                    && eq(oldItem.getBlockedNote(), newItem.getBlockedNote());
        }

        private boolean eq(String a, String b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            return a.equals(b);
        }
    };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_management, parent, false);
        return new Holder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        UserListItemResponse user = getItem(position);
        if (user != null) {
            holder.bind(user);
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView txtName, txtEmail, txtStatus, txtNote;
        Button btnBlock, btnUnblock;
        private UserListItemResponse currentUser;

        Holder(@NonNull View itemView, OnUserActionListener listener) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtEmail = itemView.findViewById(R.id.txtEmail);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtNote = itemView.findViewById(R.id.txtNote);
            btnBlock = itemView.findViewById(R.id.btnBlock);
            btnUnblock = itemView.findViewById(R.id.btnUnblock);

            btnBlock.setOnClickListener(v -> {
                if (listener != null && currentUser != null) {
                    listener.onBlockClick(currentUser);
                }
            });
            btnUnblock.setOnClickListener(v -> {
                if (listener != null && currentUser != null) {
                    listener.onUnblockClick(currentUser);
                }
            });
        }

        void bind(UserListItemResponse user) {
            this.currentUser = user;

            txtName.setText(UserManagementViewModel.fullName(user));
            txtEmail.setText(user.getEmail() != null ? user.getEmail() : "");

            boolean blocked = Boolean.TRUE.equals(user.getBlocked());
            txtStatus.setText(blocked ? "Blocked" : "Active");
            txtStatus.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    blocked ? R.color.accent_danger : R.color.text_primary));
            txtStatus.setBackgroundResource(blocked ? R.drawable.bg_status_blocked : R.drawable.bg_status_badge);

            String note = user.getBlockedNote();
            if (note != null && !note.trim().isEmpty()) {
                txtNote.setVisibility(View.VISIBLE);
                txtNote.setText(UserManagementViewModel.notePreview(note, 50));
            } else {
                txtNote.setVisibility(View.GONE);
            }

            btnBlock.setVisibility(blocked ? View.GONE : View.VISIBLE);
            btnUnblock.setVisibility(blocked ? View.VISIBLE : View.GONE);
        }
    }
}
