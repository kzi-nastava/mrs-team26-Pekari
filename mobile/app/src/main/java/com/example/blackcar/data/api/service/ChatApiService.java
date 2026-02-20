package com.example.blackcar.data.api.service;

import com.example.blackcar.data.api.model.WebChatMessage;
import com.example.blackcar.data.api.model.WebConversation;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ChatApiService {

    @GET("chat/conversations/{email}")
    Call<List<WebConversation>> getUserConversations(@Path("email") String email);

    @GET("chat/history/{conversationId}")
    Call<List<WebChatMessage>> getConversationHistory(@Path("conversationId") Long conversationId);

    @POST("chat/conversations")
    Call<WebConversation> getOrCreateConversation(@Body List<String> participantEmails);
}
