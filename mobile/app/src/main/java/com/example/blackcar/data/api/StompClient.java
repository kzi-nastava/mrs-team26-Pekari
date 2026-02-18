package com.example.blackcar.data.api;

import android.content.Context;
import android.util.Log;

import com.example.blackcar.BuildConfig;
import com.example.blackcar.data.auth.TokenManager;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class StompClient {
    private static final String TAG = "StompClient";
    private final String url;
    private final Map<String, String> connectHeaders;
    private OkHttpClient client;
    private WebSocket webSocket;
    private final Map<String, List<StompSubscription>> subscriptions = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private boolean connected = false;

    public interface StompMessageListener {
        void onMessage(String payload);
    }

    public static class StompSubscription {
        public final String id;
        public final String destination;
        public final StompMessageListener listener;

        public StompSubscription(String id, String destination, StompMessageListener listener) {
            this.id = id;
            this.destination = destination;
            this.listener = listener;
        }
    }

    public StompClient(Context context) {
        String baseUrl = BuildConfig.API_BASE_URL;
        // http://10.0.2.2:8080/api/v1 -> ws://10.0.2.2:8080/ws
        this.url = baseUrl.replace("/api/v1", "/ws").replace("http", "ws");
        this.connectHeaders = new HashMap<>();
        String token = TokenManager.getInstance(context).getToken();
        if (token != null) {
            connectHeaders.put("Authorization", "Bearer " + token);
        }
        this.client = new OkHttpClient();
    }

    public void connect() {
        Request request = new Request.Builder().url(url).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket Opened");
                sendConnectFrame();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleFrame(text);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket Closing: " + reason);
                connected = false;
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket Failure: " + t.getMessage());
                connected = false;
            }
        });
    }

    private void sendConnectFrame() {
        StringBuilder frame = new StringBuilder("CONNECT\n");
        frame.append("accept-version:1.2\n");
        for (Map.Entry<String, String> entry : connectHeaders.entrySet()) {
            frame.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }
        frame.append("\n\0");
        webSocket.send(frame.toString());
    }

    private void handleFrame(String text) {
        if (text.startsWith("CONNECTED")) {
            connected = true;
            Log.d(TAG, "STOMP Connected");
            resubscribeAll();
        } else if (text.startsWith("MESSAGE")) {
            handleMessageFrame(text);
        }
    }

    private void handleMessageFrame(String text) {
        String[] lines = text.split("\n");
        String destination = null;
        StringBuilder body = new StringBuilder();
        boolean inBody = false;
        for (String line : lines) {
            if (inBody) {
                if (line.endsWith("\0")) {
                    body.append(line, 0, line.length() - 1);
                } else {
                    body.append(line).append("\n");
                }
            } else if (line.isEmpty()) {
                inBody = true;
            } else if (line.startsWith("destination:")) {
                destination = line.substring("destination:".length()).trim();
            }
        }
        if (destination != null) {
            List<StompSubscription> subs = subscriptions.get(destination);
            if (subs != null) {
                for (StompSubscription s : subs) {
                    s.listener.onMessage(body.toString());
                }
            }
        }
    }

    public synchronized void subscribe(String destination, StompMessageListener listener) {
        String id = "sub-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        StompSubscription sub = new StompSubscription(id, destination, listener);
        List<StompSubscription> subs = subscriptions.get(destination);
        if (subs == null) {
            subs = new ArrayList<>();
            subscriptions.put(destination, subs);
        }
        subs.add(sub);
        if (connected) {
            sendSubscribeFrame(sub);
        }
    }

    private void sendSubscribeFrame(StompSubscription sub) {
        String frame = "SUBSCRIBE\nid:" + sub.id + "\ndestination:" + sub.destination + "\n\n\0";
        webSocket.send(frame);
    }

    private void resubscribeAll() {
        for (List<StompSubscription> subs : subscriptions.values()) {
            for (StompSubscription s : subs) {
                sendSubscribeFrame(s);
            }
        }
    }

    public void send(String destination, Object payload) {
        String json = gson.toJson(payload);
        String frame = "SEND\ndestination:" + destination + "\ncontent-type:application/json\n\n" + json + "\0";
        webSocket.send(frame);
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Goodbye");
        }
        connected = false;
    }
}
