package com.example.blackcar.data.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.blackcar.data.api.model.WebChatMessage;
import com.example.blackcar.data.session.SessionManager;
import com.google.gson.Gson;

import java.util.concurrent.ConcurrentHashMap;

public class ChatRealtimeService {
    private static final String TAG = "ChatRealtimeService";
    private final StompClient stompClient;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface MessageListener {
        void onMessageReceived(WebChatMessage message);
    }

    private final ConcurrentHashMap<Long, MessageListener> conversationListeners = new ConcurrentHashMap<>();
    private MessageListener adminListener;

    public ChatRealtimeService(Context context) {
        this.stompClient = new StompClient(context);
    }

    public void connect() {
        stompClient.connect();
    }

    public void subscribeToConversation(Long conversationId, MessageListener listener) {
        conversationListeners.put(conversationId, listener);
        stompClient.subscribe("/topic/chat/" + conversationId, payload -> {
            WebChatMessage message = gson.fromJson(payload, WebChatMessage.class);
            mainHandler.post(() -> {
                MessageListener l = conversationListeners.get(conversationId);
                if (l != null) l.onMessageReceived(message);
            });
        });
    }

    public void subscribeToAdminsTopic(MessageListener listener) {
        this.adminListener = listener;
        stompClient.subscribe("/topic/chat/admins", payload -> {
            WebChatMessage message = gson.fromJson(payload, WebChatMessage.class);
            mainHandler.post(() -> {
                if (adminListener != null) adminListener.onMessageReceived(message);
            });
        });
    }

    public void sendMessage(WebChatMessage message) {
        stompClient.send("/app/chat.send", message);
    }

    public void disconnect() {
        stompClient.disconnect();
        conversationListeners.clear();
        adminListener = null;
    }
}
