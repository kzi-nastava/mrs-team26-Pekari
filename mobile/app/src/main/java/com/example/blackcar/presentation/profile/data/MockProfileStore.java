package com.example.blackcar.presentation.profile.data;

import androidx.annotation.Nullable;

import com.example.blackcar.presentation.profile.model.ApprovalRequestUIModel;
import com.example.blackcar.presentation.profile.model.DriverInfoUIModel;
import com.example.blackcar.presentation.profile.model.ProfileUIModel;
import com.example.blackcar.presentation.profile.model.VehicleUIModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * UI-only mock store.
 * Keeps in-memory state for the current profile, driver info, and pending approval requests.
 */
public final class MockProfileStore {

    private static MockProfileStore instance;

    public static MockProfileStore getInstance() {
        if (instance == null) {
            instance = new MockProfileStore();
        }
        return instance;
    }

    private ProfileUIModel currentProfile;
    private DriverInfoUIModel driverInfo;
    private final List<ApprovalRequestUIModel> approvalRequests = new ArrayList<>();

    private MockProfileStore() {
        // Default user role for UI demo: driver
        currentProfile = new ProfileUIModel(
                "1",
                "john@example.com",
                "johndoe",
                "John",
                "Doe",
                "+381 64 000 000",
                "123 Main Street, City",
                "driver",
                null
        );

        driverInfo = new DriverInfoUIModel(
                5.33,
                new VehicleUIModel(
                        "v1",
                        "Tesla",
                        "Model 3",
                        2023,
                        "TS-123-AB",
                        "12345678901234567"
                )
        );
    }

    public ProfileUIModel getCurrentProfile() {
        return currentProfile;
    }

    public void setCurrentProfile(ProfileUIModel profile) {
        this.currentProfile = profile;
    }

    public DriverInfoUIModel getDriverInfo() {
        return driverInfo;
    }

    public void setDriverInfo(DriverInfoUIModel driverInfo) {
        this.driverInfo = driverInfo;
    }

    public List<ApprovalRequestUIModel> getApprovalRequests() {
        return new ArrayList<>(approvalRequests);
    }

    public boolean hasPendingRequestForCurrentUser() {
        for (ApprovalRequestUIModel r : approvalRequests) {
            if (r.userId.equals(currentProfile.id) && "pending".equalsIgnoreCase(r.status)) {
                return true;
            }
        }
        return false;
    }

    public ApprovalRequestUIModel createDriverProfileChangeRequest(ProfileUIModel proposedChanges) {
        String id = UUID.randomUUID().toString();
        ApprovalRequestUIModel request = new ApprovalRequestUIModel(
                id,
                currentProfile.id,
                currentProfile.email,
                proposedChanges,
                "pending",
                null
        );
        approvalRequests.add(0, request);
        return request;
    }

    public boolean approveRequest(String requestId) {
        for (int i = 0; i < approvalRequests.size(); i++) {
            ApprovalRequestUIModel r = approvalRequests.get(i);
            if (r.id.equals(requestId) && "pending".equalsIgnoreCase(r.status)) {
                // Apply changes to the user's profile
                currentProfile = new ProfileUIModel(
                        currentProfile.id,
                        currentProfile.email,
                        currentProfile.username,
                        r.changes.firstName,
                        r.changes.lastName,
                        r.changes.phoneNumber,
                        r.changes.address,
                        currentProfile.role,
                        r.changes.profilePictureUri
                );
                approvalRequests.set(i, r.copyWithStatus("approved", null));
                return true;
            }
        }
        return false;
    }

    public boolean rejectRequest(String requestId, @Nullable String reason) {
        for (int i = 0; i < approvalRequests.size(); i++) {
            ApprovalRequestUIModel r = approvalRequests.get(i);
            if (r.id.equals(requestId) && "pending".equalsIgnoreCase(r.status)) {
                approvalRequests.set(i, r.copyWithStatus("rejected", reason));
                return true;
            }
        }
        return false;
    }
}
