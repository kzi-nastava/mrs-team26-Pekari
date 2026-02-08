package com.example.blackcar.data.repository;

import androidx.annotation.NonNull;

import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.data.api.model.LoginRequest;
import com.example.blackcar.data.api.model.LoginResponse;
import com.example.blackcar.data.api.model.RegisterResponse;
import com.example.blackcar.data.api.service.AuthApiService;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    public interface RepoCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final AuthApiService api = ApiClient.getAuthService();

    public void login(String email, String password, RepoCallback<String> callback) {
        api.login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    String userId = body.getId() != null ? body.getId() : (body.getUserId() != null ? body.getUserId() : body.getEmail());
                    callback.onSuccess(userId);
                } else {
                    String message = "Login failed";
                    if (response.code() == 401) {
                        message = "Invalid email or password";
                    } else if (response.code() == 403) {
                        message = "Access denied";
                    } else if (response.code() == 404) {
                        message = "User not found";
                    }
                    callback.onError(message);
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                callback.onError("Network error. Please check your internet connection.");
            }
        });
    }

    public void register(String firstName, String lastName, String email, String address, String phoneNumber, String password, RepoCallback<Void> callback) {
        String username = email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String cleanPhone = phoneNumber != null ? phoneNumber.replaceAll("[^0-9+]", "") : "";

        MediaType text = MediaType.parse("text/plain");
        RequestBody emailBody = RequestBody.create(text, email);
        RequestBody usernameBody = RequestBody.create(text, username);
        RequestBody passwordBody = RequestBody.create(text, password);
        RequestBody firstNameBody = RequestBody.create(text, firstName);
        RequestBody lastNameBody = RequestBody.create(text, lastName);
        RequestBody addressBody = RequestBody.create(text, address);
        RequestBody phoneBody = RequestBody.create(text, cleanPhone);

        api.registerUser(emailBody, usernameBody, passwordBody, firstNameBody, lastNameBody, addressBody, phoneBody)
                .enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                        if (response.isSuccessful()) {
                            callback.onSuccess(null);
                        } else {
                            String message = "Registration failed";
                            if (response.code() == 409) {
                                message = "User with this email already exists";
                            } else if (response.code() == 400) {
                                message = "Invalid registration data";
                            }
                            callback.onError(message);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                        callback.onError("Network error. Please check your internet connection.");
                    }
                });
    }

    public void logout(RepoCallback<Void> callback) {
        api.logout().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                // Even if we get an error from the server, we consider the user locally logged out
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // Same for network errors
                callback.onSuccess(null);
            }
        });
    }
}
