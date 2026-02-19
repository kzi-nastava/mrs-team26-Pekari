package com.example.blackcar.presentation.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blackcar.R;
import com.example.blackcar.data.api.model.WebConversation;
import com.example.blackcar.data.session.SessionManager;

public class ConversationAdapter extends ListAdapter<WebConversation, ConversationAdapter.Holder> {

    public interface OnConversationClickListener {
        void onConversationClick(WebConversation conversation);
    }

    private final OnConversationClickListener listener;

    public ConversationAdapter(OnConversationClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    static final DiffUtil.ItemCallback<WebConversation> DIFF = new DiffUtil.ItemCallback<WebConversation>() {
        @Override
        public boolean areItemsTheSame(@NonNull WebConversation oldItem, @NonNull WebConversation newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull WebConversation oldItem, @NonNull WebConversation newItem) {
            return oldItem.getLastMessage() != null && newItem.getLastMessage() != null
                    ? oldItem.getLastMessage().getId().equals(newItem.getLastMessage().getId())
                    : oldItem.getId().equals(newItem.getId());
        }
    };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new Holder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView txtPartner, txtLastMessage, txtTime;
        private WebConversation current;

        public Holder(@NonNull View itemView, OnConversationClickListener listener) {
            super(itemView);
            txtPartner = itemView.findViewById(R.id.txtPartner);
            txtLastMessage = itemView.findViewById(R.id.txtLastMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
            itemView.setOnClickListener(v -> {
                if (listener != null && current != null) listener.onConversationClick(current);
            });
        }

        public void bind(WebConversation conv) {
            this.current = conv;
            String partner = "Unknown";
            String myEmail = SessionManager.getEmail();
            if (conv.getParticipantEmails() != null) {
                for (String e : conv.getParticipantEmails()) {
                    if (!e.equalsIgnoreCase(myEmail)) {
                        partner = e;
                        break;
                    }
                }
            }
            txtPartner.setText(partner);
            if (conv.getLastMessage() != null) {
                txtLastMessage.setText(conv.getLastMessage().getContent());
                txtTime.setText(conv.getLastMessage().getCreatedAt());
            } else {
                txtLastMessage.setText("No messages yet");
                txtTime.setText("");
            }
        }
    }
}
