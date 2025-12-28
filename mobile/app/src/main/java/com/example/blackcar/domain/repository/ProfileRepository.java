package com.example.blackcar.domain.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.blackcar.domain.model.ApprovalRequest;
import com.example.blackcar.domain.model.DriverInfo;
import com.example.blackcar.domain.model.PasswordChangeRequest;
import com.example.blackcar.domain.model.ProfileData;
import com.example.blackcar.domain.model.ProfileUpdateRequest;

import java.util.List;

public interface ProfileRepository {

    interface Callback<T> {
        void onSuccess(@NonNull T data);

        void onError(@NonNull String message);
    }

    void getProfile(@NonNull Callback<ProfileData> callback);

    void getDriverInfo(@NonNull Callback<DriverInfo> callback);

    void updateProfile(@NonNull ProfileUpdateRequest request, @NonNull Callback<String> callback);

    void changePassword(@NonNull PasswordChangeRequest request, @NonNull Callback<String> callback);

    void getPendingApprovalRequests(@NonNull Callback<List<ApprovalRequest>> callback);

    void approveRequest(@NonNull String requestId, @NonNull Callback<String> callback);

    void rejectRequest(@NonNull String requestId, @Nullable String reason, @NonNull Callback<String> callback);
}
