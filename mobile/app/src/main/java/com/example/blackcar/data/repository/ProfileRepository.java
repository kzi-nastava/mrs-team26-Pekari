package com.example.blackcar.data.repository;

import androidx.annotation.NonNull;

import com.example.blackcar.data.api.ApiClient;
import com.example.blackcar.data.api.model.ChangePasswordRequest;
import com.example.blackcar.data.api.model.DriverProfileResponse;
import com.example.blackcar.data.api.model.MessageResponse;
import com.example.blackcar.data.api.model.PassengerProfileResponse;
import com.example.blackcar.data.api.model.UpdateProfileRequest;
import com.example.blackcar.data.api.service.ProfileApiService;
import com.example.blackcar.data.session.SessionManager;
import com.example.blackcar.presentation.profile.model.DriverInfoUIModel;
import com.example.blackcar.presentation.profile.model.ProfileUIModel;
import com.example.blackcar.presentation.profile.model.VehicleUIModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileRepository {

    public interface RepoCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public static class ProfilePayload {
        public final ProfileUIModel profile;
        public final DriverInfoUIModel driverInfo;

        public ProfilePayload(ProfileUIModel profile, DriverInfoUIModel driverInfo) {
            this.profile = profile;
            this.driverInfo = driverInfo;
        }
    }

    private final ProfileApiService api = ApiClient.getProfileService();

    public void getProfile(RepoCallback<ProfilePayload> callback) {
        String role = SessionManager.getRole();
        if ("driver".equalsIgnoreCase(role)) {
            api.getDriverProfile().enqueue(new Callback<DriverProfileResponse>() {
                @Override
                public void onResponse(@NonNull Call<DriverProfileResponse> call, @NonNull Response<DriverProfileResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        DriverProfileResponse body = response.body();
                        ProfileUIModel profile = new ProfileUIModel(
                                body.getId(),
                                body.getEmail(),
                                body.getUsername(),
                                body.getFirstName(),
                                body.getLastName(),
                                body.getPhoneNumber(),
                                body.getAddress(),
                                "driver",
                                body.getProfilePicture()
                        );
                        DriverInfoUIModel driverInfo = mapDriverInfo(body);
                        callback.onSuccess(new ProfilePayload(profile, driverInfo));
                    } else {
                        callback.onError(mapProfileError(response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<DriverProfileResponse> call, @NonNull Throwable t) {
                    callback.onError("Network error. Please check your internet connection.");
                }
            });
        } else if ("admin".equalsIgnoreCase(role)) {
            api.getAdminProfile().enqueue(new Callback<PassengerProfileResponse>() {
                @Override
                public void onResponse(@NonNull Call<PassengerProfileResponse> call, @NonNull Response<PassengerProfileResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        PassengerProfileResponse body = response.body();
                        ProfileUIModel profile = new ProfileUIModel(
                                body.getId(),
                                body.getEmail(),
                                body.getUsername(),
                                body.getFirstName(),
                                body.getLastName(),
                                body.getPhoneNumber(),
                                body.getAddress(),
                                "admin",
                                body.getProfilePicture()
                        );
                        callback.onSuccess(new ProfilePayload(profile, null));
                    } else {
                        callback.onError(mapProfileError(response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<PassengerProfileResponse> call, @NonNull Throwable t) {
                    callback.onError("Network error. Please check your internet connection.");
                }
            });
        } else {
            api.getPassengerProfile().enqueue(new Callback<PassengerProfileResponse>() {
                @Override
                public void onResponse(@NonNull Call<PassengerProfileResponse> call, @NonNull Response<PassengerProfileResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        PassengerProfileResponse body = response.body();
                        ProfileUIModel profile = new ProfileUIModel(
                                body.getId(),
                                body.getEmail(),
                                body.getUsername(),
                                body.getFirstName(),
                                body.getLastName(),
                                body.getPhoneNumber(),
                                body.getAddress(),
                                "passenger",
                                body.getProfilePicture()
                        );
                        callback.onSuccess(new ProfilePayload(profile, null));
                    } else {
                        callback.onError(mapProfileError(response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<PassengerProfileResponse> call, @NonNull Throwable t) {
                    callback.onError("Network error. Please check your internet connection.");
                }
            });
        }
    }

    public void updateProfile(ProfileUIModel profile, RepoCallback<String> callback) {
        String role = SessionManager.getRole();
        String picture = sanitizeProfilePicture(profile.profilePictureUri);
        UpdateProfileRequest request = new UpdateProfileRequest(
                profile.firstName,
                profile.lastName,
                profile.phoneNumber,
                profile.address,
                picture
        );

        Call<MessageResponse> call;
        if ("driver".equalsIgnoreCase(role)) {
            call = api.updateDriverProfile(request);
        } else if ("admin".equalsIgnoreCase(role)) {
            call = api.updateAdminProfile(request);
        } else {
            call = api.updatePassengerProfile(request);
        }

        call.enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                if (response.isSuccessful()) {
                    String message = response.body() != null ? response.body().getMessage() : "Profile updated";
                    callback.onSuccess(message);
                } else {
                    callback.onError(mapUpdateError(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                callback.onError("Network error. Please check your internet connection.");
            }
        });
    }

    public void changePassword(String currentPassword, String newPassword, String confirmPassword, RepoCallback<String> callback) {
        ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword, confirmPassword);
        api.changePassword(request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                if (response.isSuccessful()) {
                    String message = response.body() != null ? response.body().getMessage() : "Password changed";
                    callback.onSuccess(message);
                } else {
                    callback.onError(mapPasswordError(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                callback.onError("Network error. Please check your internet connection.");
            }
        });
    }

    private static DriverInfoUIModel mapDriverInfo(DriverProfileResponse response) {
        VehicleUIModel vehicle = new VehicleUIModel(
                response.getId(),
                safeString(response.getVehicleType()),
                safeString(response.getVehicleModel()),
                0,
                safeString(response.getLicensePlate()),
                safeString(response.getVehicleRegistration())
        );
        return new DriverInfoUIModel(0, vehicle);
    }

    private static String safeString(String value) {
        return value != null ? value : "";
    }

    private static String sanitizeProfilePicture(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= 500 ? trimmed : null;
    }

    private static String mapProfileError(int code) {
        if (code == 401 || code == 403) {
            return "Session expired. Please log in again.";
        }
        return "Failed to load profile.";
    }

    private static String mapUpdateError(int code) {
        if (code == 400) {
            return "Invalid profile data.";
        }
        if (code == 401 || code == 403) {
            return "Session expired. Please log in again.";
        }
        return "Failed to update profile.";
    }

    private static String mapPasswordError(int code) {
        if (code == 400) {
            return "Failed to change password. Please check your current password.";
        }
        if (code == 401 || code == 403) {
            return "Session expired. Please log in again.";
        }
        return "Failed to change password.";
    }
}
