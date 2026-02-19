package com.example.blackcar.presentation.chat;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blackcar.R;
import com.example.blackcar.data.api.model.WebChatMessage;
import com.example.blackcar.data.session.SessionManager;

public class MessageAdapter extends ListAdapter<WebChatMessage, MessageAdapter.Holder> {

    public MessageAdapter() {
        super(DIFF);
    }

    static final DiffUtil.ItemCallback<WebChatMessage> DIFF = new DiffUtil.ItemCallback<WebChatMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull WebChatMessage oldItem, @NonNull WebChatMessage newItem) {
            if (oldItem.getId() != null && newItem.getId() != null) {
                return oldItem.getId().equals(newItem.getId());
            }
            // Fallback for messages without IDs (e.g. locally sent or transient)
            return oldItem == newItem || (oldItem.getContent().equals(newItem.getContent()) && 
                   oldItem.getSenderEmail().equals(newItem.getSenderEmail()));
        }

        @Override
        public boolean areContentsTheSame(@NonNull WebChatMessage oldItem, @NonNull WebChatMessage newItem) {
            boolean sameContent = (oldItem.getContent() == null && newItem.getContent() == null) ||
                    (oldItem.getContent() != null && oldItem.getContent().equals(newItem.getContent()));
            boolean sameTime = (oldItem.getCreatedAt() == null && newItem.getCreatedAt() == null) ||
                    (oldItem.getCreatedAt() != null && oldItem.getCreatedAt().equals(newItem.getCreatedAt()));
            boolean sameSender = (oldItem.getSenderEmail() == null && newItem.getSenderEmail() == null) ||
                    (oldItem.getSenderEmail() != null && oldItem.getSenderEmail().equalsIgnoreCase(newItem.getSenderEmail()));
            return sameContent && sameTime && sameSender;
        }
    };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (position >= 0 && position < getItemCount()) {
            holder.bind(getItem(position));
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        LinearLayout layoutBubble, messageContainer;
        TextView txtContent, txtTime;

        public Holder(@NonNull View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            layoutBubble = itemView.findViewById(R.id.layoutMessageBubble);
            txtContent = itemView.findViewById(R.id.txtMessageContent);
            txtTime = itemView.findViewById(R.id.txtMessageTime);
        }

        public void bind(WebChatMessage msg) {
            txtContent.setText(msg.getContent());
            txtTime.setText(msg.getCreatedAt());

            boolean isMe = msg.getSenderEmail().equalsIgnoreCase(SessionManager.getEmail());
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layoutBubble.getLayoutParams();
            if (isMe) {
                params.gravity = Gravity.END;
                layoutBubble.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.bg_chat_bubble_me));
                txtContent.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.black));
                txtTime.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.black));
            } else {
                params.gravity = Gravity.START;
                layoutBubble.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.bg_chat_bubble_partner));
                txtContent.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
                txtTime.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
            }
            layoutBubble.setLayoutParams(params);
        }
    }
}
