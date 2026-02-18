package com.example.blackcar.presentation.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;

import com.example.blackcar.data.api.model.WebConversation;
import com.example.blackcar.data.session.SessionManager;
import com.example.blackcar.databinding.FragmentChatBinding;
import com.example.blackcar.presentation.ViewModelFactory;

import java.util.Collections;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatViewModel viewModel;
    private ConversationAdapter conversationAdapter;
    private MessageAdapter messageAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(requireContext())).get(ChatViewModel.class);

        setupAdapters();
        setupClickListeners();
        observeState();

        viewModel.loadConversations();
    }

    private void setupAdapters() {
        conversationAdapter = new ConversationAdapter(conv -> {
            viewModel.selectConversation(conv);
        });
        binding.recyclerConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerConversations.setAdapter(conversationAdapter);

        messageAdapter = new MessageAdapter();
        binding.recyclerMessages.setLayoutManager(new SafeLinearLayoutManager(requireContext()));
        binding.recyclerMessages.setAdapter(messageAdapter);
    }

    /**
     * Custom LayoutManager to catch RecyclerView inconsistency exceptions during rapid updates.
     */
    private static class SafeLinearLayoutManager extends LinearLayoutManager {
        public SafeLinearLayoutManager(android.content.Context context) {
            super(context);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException e) {
                Log.e("ChatFragment", "RecyclerView inconsistency detected in onLayoutChildren", e);
            }
        }
    }

    private void setupClickListeners() {
        binding.btnSupportChat.setOnClickListener(v -> viewModel.startSupportChat());
        binding.btnBack.setOnClickListener(v -> viewModel.backToList());
        binding.btnClose.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.btnSend.setOnClickListener(v -> {
            String content = binding.editMessage.getText().toString();
            if (!content.trim().isEmpty()) {
                viewModel.sendMessage(content);
                binding.editMessage.setText("");
            }
        });
    }

    private void observeState() {
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state instanceof ChatViewState.Loading) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.txtError.setVisibility(View.GONE);
            } else if (state instanceof ChatViewState.Error) {
                binding.progressBar.setVisibility(View.GONE);
                binding.txtError.setVisibility(View.VISIBLE);
                binding.txtError.setText(((ChatViewState.Error) state).getMessage());
            } else if (state instanceof ChatViewState.ConversationsLoaded) {
                binding.progressBar.setVisibility(View.GONE);
                binding.layoutConversations.setVisibility(View.VISIBLE);
                binding.layoutChat.setVisibility(View.GONE);
                binding.btnBack.setVisibility(View.GONE);
                binding.txtTitle.setText("Messages");
                conversationAdapter.submitList(((ChatViewState.ConversationsLoaded) state).getConversations());

                // Show "Start Support Chat" only for non-admins
                String role = SessionManager.getRole();
                binding.btnSupportChat.setVisibility("admin".equalsIgnoreCase(role) ? View.GONE : View.VISIBLE);
            } else if (state instanceof ChatViewState.ConversationHistoryLoaded) {
                binding.progressBar.setVisibility(View.GONE);
                binding.layoutConversations.setVisibility(View.GONE);
                binding.layoutChat.setVisibility(View.VISIBLE);
                binding.btnBack.setVisibility(View.VISIBLE);
                ChatViewState.ConversationHistoryLoaded historyState = (ChatViewState.ConversationHistoryLoaded) state;
                messageAdapter.submitList(historyState.getMessages(), () -> {
                    if (isAdded() && !historyState.getMessages().isEmpty()) {
                        binding.recyclerMessages.scrollToPosition(historyState.getMessages().size() - 1);
                    }
                });

                WebConversation active = historyState.getActiveConversation();
                if (active != null) {
                    binding.txtTitle.setText(getChatPartner(active));
                }
            }
        });
    }

    private String getChatPartner(WebConversation conv) {
        String myEmail = SessionManager.getEmail();
        if (myEmail == null) return "Chat";
        for (String email : conv.getParticipantEmails()) {
            if (!email.equalsIgnoreCase(myEmail)) return email;
        }
        return "Support Chat";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
