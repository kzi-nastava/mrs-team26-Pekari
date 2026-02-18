package com.example.blackcar.data.repository;

import androidx.annotation.NonNull;

import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.data.api.model.WebChatMessage;
import com.example.blackcar.data.api.model.WebConversation;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRepository {

    public interface ListCallback<T> {
        void onSuccess(List<T> data);
        void onError(String message);
    }

    public interface SingleCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public void getUserConversations(String email, @NonNull ListCallback<WebConversation> callback) {
        ApiClient.getChatService().getUserConversations(email)
                .enqueue(new Callback<List<WebConversation>>() {
                    @Override
                    public void onResponse(Call<List<WebConversation>> call, Response<List<WebConversation>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<WebConversation>> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void getConversationHistory(Long conversationId, @NonNull ListCallback<WebChatMessage> callback) {
        ApiClient.getChatService().getConversationHistory(conversationId)
                .enqueue(new Callback<List<WebChatMessage>>() {
                    @Override
                    public void onResponse(Call<List<WebChatMessage>> call, Response<List<WebChatMessage>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<WebChatMessage>> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void getOrCreateConversation(List<String> participantEmails, @NonNull SingleCallback<WebConversation> callback) {
        ApiClient.getChatService().getOrCreateConversation(participantEmails)
                .enqueue(new Callback<WebConversation>() {
                    @Override
                    public void onResponse(Call<WebConversation> call, Response<WebConversation> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<WebConversation> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }
}
