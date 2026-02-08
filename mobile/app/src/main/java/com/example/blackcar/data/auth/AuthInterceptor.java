package com.example.blackcar.data.auth;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    public AuthInterceptor(Context context) {
        this.tokenManager = TokenManager.getInstance(context);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String token = tokenManager.getToken();

        // If no token, proceed with original request
        if (token == null || token.isEmpty()) {
            return chain.proceed(originalRequest);
        }

        // Add Authorization header with Bearer token
        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(authenticatedRequest);
    }
}
