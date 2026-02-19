package com.example.blackcar.presentation.chat;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.blackcar.data.api.ChatRealtimeService;
import com.example.blackcar.data.api.model.WebChatMessage;
import com.example.blackcar.data.api.model.WebConversation;
import com.example.blackcar.data.repository.ChatRepository;
import com.example.blackcar.data.session.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatViewModel extends AndroidViewModel {

    private final MutableLiveData<ChatViewState> state = new MutableLiveData<>(new ChatViewState.Loading());
    private final ChatRepository repository = new ChatRepository();
    private final ChatRealtimeService realtimeService;
    private WebConversation activeConversation;
    private List<WebChatMessage> currentMessages = new CopyOnWriteArrayList<>();

    public ChatViewModel(@NonNull Application application) {
        super(application);
        this.realtimeService = new ChatRealtimeService(application);
        this.realtimeService.connect();

        String role = SessionManager.getRole();
        if ("admin".equalsIgnoreCase(role)) {
            this.realtimeService.subscribeToAdminsTopic(message -> {
                // Refresh list if new conversation or update messages if active
                if (activeConversation != null && activeConversation.getId().equals(message.getConversationId())) {
                    appendMessage(message);
                } else {
                    loadConversations();
                }
            });
        }
    }

    public LiveData<ChatViewState> getState() { return state; }

    public synchronized void loadConversations() {
        String email = SessionManager.getEmail();
        if (email == null) {
            state.setValue(new ChatViewState.Error("Not logged in"));
            return;
        }

        repository.getUserConversations(email, new ChatRepository.ListCallback<WebConversation>() {
            @Override
            public void onSuccess(List<WebConversation> data) {
                state.postValue(new ChatViewState.ConversationsLoaded(data));
            }

            @Override
            public void onError(String message) {
                state.postValue(new ChatViewState.Error(message));
            }
        });
    }

    public synchronized void selectConversation(WebConversation conversation) {
        if (activeConversation != null && activeConversation.getId().equals(conversation.getId())) return;
        this.activeConversation = conversation;
        loadHistory(conversation.getId());
        subscribeToActiveChat(conversation.getId());
    }

    private void loadHistory(Long conversationId) {
        state.setValue(new ChatViewState.Loading());
        repository.getConversationHistory(conversationId, new ChatRepository.ListCallback<WebChatMessage>() {
            @Override
            public void onSuccess(List<WebChatMessage> data) {
                synchronized (ChatViewModel.this) {
                    currentMessages = new CopyOnWriteArrayList<>(data);
                    state.postValue(new ChatViewState.ConversationHistoryLoaded(new ArrayList<>(currentMessages), activeConversation));
                }
            }

            @Override
            public void onError(String message) {
                state.postValue(new ChatViewState.Error(message));
            }
        });
    }

    private void subscribeToActiveChat(Long conversationId) {
        realtimeService.subscribeToConversation(conversationId, message -> {
            appendMessage(message);
        });
    }

    private synchronized void appendMessage(WebChatMessage message) {
        // Prevent duplicates
        for (WebChatMessage m : currentMessages) {
            if (m.getId() != null && m.getId().equals(message.getId())) return;
        }
        currentMessages.add(message);
        state.postValue(new ChatViewState.ConversationHistoryLoaded(new ArrayList<>(currentMessages), activeConversation));
    }

    public void sendMessage(String content) {
        if (activeConversation == null || content == null || content.trim().isEmpty()) return;

        WebChatMessage msg = new WebChatMessage();
        msg.setConversationId(activeConversation.getId());
        msg.setSenderEmail(SessionManager.getEmail());
        msg.setContent(content.trim());

        realtimeService.sendMessage(msg);
    }

    public void startSupportChat() {
        String email = SessionManager.getEmail();
        if (email == null) return;

        state.setValue(new ChatViewState.Loading());
        repository.getOrCreateConversation(Collections.singletonList(email), new ChatRepository.SingleCallback<WebConversation>() {
            @Override
            public void onSuccess(WebConversation data) {
                selectConversation(data);
            }

            @Override
            public void onError(String message) {
                state.postValue(new ChatViewState.Error(message));
            }
        });
    }

    public synchronized void backToList() {
        this.activeConversation = null;
        synchronized (this) {
            this.currentMessages.clear();
        }
        loadConversations();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        realtimeService.disconnect();
    }
}
