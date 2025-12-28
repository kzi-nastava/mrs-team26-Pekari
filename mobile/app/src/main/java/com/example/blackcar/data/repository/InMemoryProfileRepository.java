package com.example.blackcar.data.repository;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.blackcar.data.session.SessionManager;
import com.example.blackcar.domain.model.ApprovalRequest;
import com.example.blackcar.domain.model.ApprovalStatus;
import com.example.blackcar.domain.model.DriverInfo;
import com.example.blackcar.domain.model.PasswordChangeRequest;
import com.example.blackcar.domain.model.ProfileData;
import com.example.blackcar.domain.model.ProfileUpdateRequest;
import com.example.blackcar.domain.model.User;
import com.example.blackcar.domain.model.UserRole;
import com.example.blackcar.domain.model.VehicleInfo;
import com.example.blackcar.domain.repository.ProfileRepository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class InMemoryProfileRepository implements ProfileRepository {
    private final SessionManager sessionManager;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ProfileData driverProfile;
    private ProfileData passengerProfile;
    private ProfileData adminProfile;
    private final DriverInfo driverInfo;

    private final List<ApprovalRequest> approvalRequests = new ArrayList<>();

    public InMemoryProfileRepository(@NonNull SessionManager sessionManager) {
        this.sessionManager = sessionManager;

        driverProfile = new ProfileData(
                "1",
                "driver@example.com",
                "driveruser",
                "John",
                "Doe",
                "+381 64 000 000",
                "123 Main Street, City",
                UserRole.DRIVER,
                null
        );

        passengerProfile = new ProfileData(
                "2",
                "passenger@example.com",
                "passengeruser",
                "Jane",
                "Smith",
                "+381 64 111 111",
                "45 Side Street, City",
                UserRole.PASSENGER,
                null
        );

        adminProfile = new ProfileData(
                "3",
                "admin@example.com",
                "adminuser",
                "Admin",
                "User",
                "+381 64 222 222",
                "1 Admin Blvd, City",
                UserRole.ADMIN,
                null
        );

        driverInfo = new DriverInfo(
                5.33,
                new VehicleInfo("v1", "Tesla", "Model 3", 2023, "TS-123-AB", "12345678901234567")
        );
    }

    @Override
    public void getProfile(@NonNull Callback<ProfileData> callback) {
        ioExecutor.execute(() -> {
            sleep(350);
            User user = sessionManager.getCurrentUser();
            ProfileData profile = getProfileForUser(user);
            mainHandler.post(() -> callback.onSuccess(profile));
        });
    }

    @Override
    public void getDriverInfo(@NonNull Callback<DriverInfo> callback) {
        ioExecutor.execute(() -> {
            sleep(250);
            if (sessionManager.getRole() != UserRole.DRIVER) {
                mainHandler.post(() -> callback.onError("Driver information is available only for drivers."));
                return;
            }
            mainHandler.post(() -> callback.onSuccess(driverInfo));
        });
    }

    @Override
    public void updateProfile(@NonNull ProfileUpdateRequest request, @NonNull Callback<String> callback) {
        ioExecutor.execute(() -> {
            sleep(500);
            UserRole role = sessionManager.getRole();
            User user = sessionManager.getCurrentUser();

            if (role == UserRole.DRIVER) {
                ApprovalRequest approvalRequest = new ApprovalRequest(
                        UUID.randomUUID().toString(),
                        user.getId(),
                        request,
                        ApprovalStatus.PENDING,
                        System.currentTimeMillis(),
                        null,
                        null,
                        null
                );
                synchronized (approvalRequests) {
                    approvalRequests.add(0, approvalRequest);
                }
                mainHandler.post(() -> callback.onSuccess("Profile update request sent for admin approval."));
                return;
            }

            applyChangesImmediately(user.getId(), request);
            mainHandler.post(() -> callback.onSuccess("Profile updated successfully."));
        });
    }

    @Override
    public void changePassword(@NonNull PasswordChangeRequest request, @NonNull Callback<String> callback) {
        ioExecutor.execute(() -> {
            sleep(450);

            if (request.getCurrentPassword().trim().length() < 6) {
                mainHandler.post(() -> callback.onError("Current password is invalid."));
                return;
            }
            if (request.getNewPassword().trim().length() < 6) {
                mainHandler.post(() -> callback.onError("New password must be at least 6 characters."));
                return;
            }
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                mainHandler.post(() -> callback.onError("Passwords do not match."));
                return;
            }
            mainHandler.post(() -> callback.onSuccess("Password changed successfully."));
        });
    }

    @Override
    public void getPendingApprovalRequests(@NonNull Callback<List<ApprovalRequest>> callback) {
        ioExecutor.execute(() -> {
            sleep(300);
            if (sessionManager.getRole() != UserRole.ADMIN) {
                mainHandler.post(() -> callback.onError("Only admins can review approval requests."));
                return;
            }
            List<ApprovalRequest> pending;
            synchronized (approvalRequests) {
                pending = new ArrayList<>();
                for (ApprovalRequest request : approvalRequests) {
                    if (request.getStatus() == ApprovalStatus.PENDING) {
                        pending.add(request);
                    }
                }
            }
            mainHandler.post(() -> callback.onSuccess(pending));
        });
    }

    @Override
    public void approveRequest(@NonNull String requestId, @NonNull Callback<String> callback) {
        ioExecutor.execute(() -> {
            sleep(450);
            if (sessionManager.getRole() != UserRole.ADMIN) {
                mainHandler.post(() -> callback.onError("Only admins can approve requests."));
                return;
            }
            ApprovalRequest found = null;
            synchronized (approvalRequests) {
                for (ApprovalRequest request : approvalRequests) {
                    if (request.getId().equals(requestId)) {
                        found = request;
                        break;
                    }
                }
            }
            if (found == null) {
                mainHandler.post(() -> callback.onError("Request not found."));
                return;
            }

            applyChangesImmediately(found.getUserId(), found.getChanges());
            markRequest(requestId, ApprovalStatus.APPROVED, null);
            mainHandler.post(() -> callback.onSuccess("Request approved."));
        });
    }

    @Override
    public void rejectRequest(@NonNull String requestId, @Nullable String reason, @NonNull Callback<String> callback) {
        ioExecutor.execute(() -> {
            sleep(450);
            if (sessionManager.getRole() != UserRole.ADMIN) {
                mainHandler.post(() -> callback.onError("Only admins can reject requests."));
                return;
            }
            boolean exists = false;
            synchronized (approvalRequests) {
                for (ApprovalRequest request : approvalRequests) {
                    if (request.getId().equals(requestId)) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) {
                mainHandler.post(() -> callback.onError("Request not found."));
                return;
            }
            markRequest(requestId, ApprovalStatus.REJECTED, reason);
            mainHandler.post(() -> callback.onSuccess("Request rejected."));
        });
    }

    @NonNull
    private ProfileData getProfileForUser(@NonNull User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return adminProfile;
        }
        if (user.getRole() == UserRole.DRIVER) {
            return driverProfile;
        }
        return passengerProfile;
    }

    private void applyChangesImmediately(@NonNull String userId, @NonNull ProfileUpdateRequest changes) {
        if ("1".equals(userId)) {
            driverProfile = driverProfile.withUpdated(
                    changes.getFirstName(),
                    changes.getLastName(),
                    changes.getPhoneNumber(),
                    changes.getAddress(),
                    changes.getProfilePicture()
            );
        } else if ("2".equals(userId)) {
            passengerProfile = passengerProfile.withUpdated(
                    changes.getFirstName(),
                    changes.getLastName(),
                    changes.getPhoneNumber(),
                    changes.getAddress(),
                    changes.getProfilePicture()
            );
        } else if ("3".equals(userId)) {
            adminProfile = adminProfile.withUpdated(
                    changes.getFirstName(),
                    changes.getLastName(),
                    changes.getPhoneNumber(),
                    changes.getAddress(),
                    changes.getProfilePicture()
            );
        }
    }

    private void markRequest(@NonNull String requestId, @NonNull ApprovalStatus newStatus, @Nullable String reason) {
        User admin = sessionManager.getCurrentUser();
        synchronized (approvalRequests) {
            List<ApprovalRequest> updated = new ArrayList<>(approvalRequests.size());
            for (ApprovalRequest request : approvalRequests) {
                if (request.getId().equals(requestId)) {
                    updated.add(request.withStatus(
                            newStatus,
                            System.currentTimeMillis(),
                            admin.getUsername(),
                            reason
                    ));
                } else {
                    updated.add(request);
                }
            }
            approvalRequests.clear();
            approvalRequests.addAll(updated);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
