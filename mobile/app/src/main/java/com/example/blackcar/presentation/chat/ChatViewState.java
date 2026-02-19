package com.example.blackcar.presentation.chat;

import com.example.blackcar.data.api.model.WebChatMessage;
import com.example.blackcar.data.api.model.WebConversation;

import java.util.List;

public abstract class ChatViewState {
    private ChatViewState() {}

    public static class Loading extends ChatViewState {}

    public static class ConversationsLoaded extends ChatViewState {
        private final List<WebConversation> conversations;
        public ConversationsLoaded(List<WebConversation> conversations) {
            this.conversations = conversations;
        }
        public List<WebConversation> getConversations() { return conversations; }
    }

    public static class ConversationHistoryLoaded extends ChatViewState {
        private final List<WebChatMessage> messages;
        private final WebConversation activeConversation;
        public ConversationHistoryLoaded(List<WebChatMessage> messages, WebConversation activeConversation) {
            this.messages = messages;
            this.activeConversation = activeConversation;
        }
        public List<WebChatMessage> getMessages() { return messages; }
        public WebConversation getActiveConversation() { return activeConversation; }
    }

    public static class Error extends ChatViewState {
        private final String message;
        public Error(String message) { this.message = message; }
        public String getMessage() { return message; }
    }
}
