package com.example.blackcar.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.data.api.model.LoginRequest;
import com.example.blackcar.data.api.model.LoginResponse;
import com.example.blackcar.data.api.model.RegisterDriverResponse;
import com.example.blackcar.data.api.model.RegisterResponse;
import com.example.blackcar.data.api.service.AuthApiService;
import com.example.blackcar.data.session.SessionManager;
import com.example.blackcar.data.auth.TokenManager;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private static final String TAG = "AuthRepository";

    public interface RepoCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final AuthApiService api = ApiClient.getAuthService();
    private final TokenManager tokenManager;
    private final NotificationRepository notificationRepository;

    public AuthRepository(Context context) {
        this.tokenManager = TokenManager.getInstance(context);
        this.notificationRepository = new NotificationRepository();
    }

    public void login(String email, String password, RepoCallback<String> callback) {
        api.login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();

                    // Store token and user info
                    if (body.getToken() != null) {
                        tokenManager.saveToken(body.getToken());
                    }
                    if (body.getRole() != null) {
                        tokenManager.saveRole(body.getRole());
                    }

                    String userId = body.getId() != null ? body.getId() : (body.getUserId() != null ? body.getUserId() : body.getEmail());
                    SessionManager.setSession(body.getToken(), body.getEmail(), body.getRole(), userId);
                    if (userId != null) {
                        tokenManager.saveUserId(userId);
                    }

                    // Register FCM token after successful login
                    registerFcmToken();

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

    public void registerDriver(String email, String firstName, String lastName, String address, String phoneNumber,
                               String vehicleModel, String vehicleType, String licensePlate, int numberOfSeats,
                               boolean babyFriendly, boolean petFriendly,
                               RepoCallback<RegisterDriverResponse> callback) {
        String cleanPhone = phoneNumber != null ? phoneNumber.replaceAll("[^0-9+]", "") : "";
        MediaType text = MediaType.parse("text/plain");

        RequestBody emailBody = RequestBody.create(text, email != null ? email : "");
        RequestBody firstNameBody = RequestBody.create(text, firstName != null ? firstName : "");
        RequestBody lastNameBody = RequestBody.create(text, lastName != null ? lastName : "");
        RequestBody addressBody = RequestBody.create(text, address != null ? address : "");
        RequestBody phoneBody = RequestBody.create(text, cleanPhone);
        RequestBody vehicleModelBody = RequestBody.create(text, vehicleModel != null ? vehicleModel : "");
        RequestBody vehicleTypeBody = RequestBody.create(text, vehicleType != null ? vehicleType : "STANDARD");
        RequestBody licensePlateBody = RequestBody.create(text, licensePlate != null ? licensePlate : "");
        RequestBody numberOfSeatsBody = RequestBody.create(text, String.valueOf(numberOfSeats));
        RequestBody babyFriendlyBody = RequestBody.create(text, babyFriendly ? "true" : "false");
        RequestBody petFriendlyBody = RequestBody.create(text, petFriendly ? "true" : "false");

        api.registerDriver(emailBody, firstNameBody, lastNameBody, addressBody, phoneBody,
                vehicleModelBody, vehicleTypeBody, licensePlateBody, numberOfSeatsBody,
                babyFriendlyBody, petFriendlyBody)
                .enqueue(new Callback<RegisterDriverResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RegisterDriverResponse> call, @NonNull Response<RegisterDriverResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            String message = "Failed to register driver";
                            if (response.code() == 409) {
                                try {
                                    if (response.errorBody() != null) {
                                        String err = response.errorBody().string();
                                        if (err != null && (err.toLowerCase().contains("license") || err.contains("plate"))) {
                                            message = "Vehicle with this license plate already registered";
                                        } else {
                                            message = "Email already exists";
                                        }
                                    } else {
                                        message = "Email or license plate already registered";
                                    }
                                } catch (Exception e) {
                                    message = "Email or license plate already registered";
                                }
                            } else if (response.code() == 400) {
                                message = "Invalid data. Check all fields.";
                            } else if (response.code() == 403) {
                                message = "Access denied. Admin only.";
                            }
                            callback.onError(message);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RegisterDriverResponse> call, @NonNull Throwable t) {
                        callback.onError("Network error. Please check your internet connection.");
                    }
                });
    }

    public void logout(RepoCallback<Void> callback) {
        // If user is admin, unsubscribe from admins topic before logout
        if (tokenManager.isAdmin()) {
            String fcmToken = tokenManager.getFcmToken();
            if (fcmToken != null && !fcmToken.isEmpty()) {
                notificationRepository.unsubscribeAdmin(fcmToken, new NotificationRepository.RegistrationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "Unsubscribed from admins topic");
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.w(TAG, "Failed to unsubscribe from admins topic: " + error);
                    }
                });
            }
        }

        api.logout().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                // Clear stored token
                tokenManager.clearToken();
                // Even if we get an error from the server, we consider the user locally logged out
                SessionManager.clear();
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // Clear stored token even on network error
                tokenManager.clearToken();
                // Same for network errors
                SessionManager.clear();
                callback.onSuccess(null);
            }
        });
    }

    /**
     * Register FCM token with backend for push notifications.
     * Backend subscribes to user topic, and admins topic if user is admin.
     */
    private void registerFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM token failed", task.getException());
                        return;
                    }

                    String fcmToken = task.getResult();
                    if (fcmToken == null || fcmToken.isEmpty()) {
                        Log.w(TAG, "FCM token is null or empty");
                        return;
                    }

                    // Save token locally
                    tokenManager.saveFcmToken(fcmToken);

                    // Register with backend
                    notificationRepository.registerToken(fcmToken, new NotificationRepository.RegistrationCallback() {
                        @Override
                        public void onSuccess() {
                            Log.i(TAG, "FCM token registered successfully");
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e(TAG, "Failed to register FCM token: " + error);
                        }
                    });
                });
    }
}
