package com.example.blackcar.presentation.profile.data;

import androidx.annotation.Nullable;

import com.example.blackcar.presentation.profile.model.ApprovalRequestUIModel;
import com.example.blackcar.presentation.profile.model.ProfileUIModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-memory store for admin approval requests.
 * This mirrors the web behavior until the backend supports approval endpoints.
 */
public final class ApprovalRequestStore {

    private static ApprovalRequestStore instance;

    public static ApprovalRequestStore getInstance() {
        if (instance == null) {
            instance = new ApprovalRequestStore();
        }
        return instance;
    }

    private final List<ApprovalRequestUIModel> approvalRequests = new ArrayList<>();

    private ApprovalRequestStore() {
    }

    public List<ApprovalRequestUIModel> getApprovalRequests() {
        return new ArrayList<>(approvalRequests);
    }

    public boolean hasPendingRequestForUser(String userId) {
        for (ApprovalRequestUIModel r : approvalRequests) {
            if (r.userId.equals(userId) && "pending".equalsIgnoreCase(r.status)) {
                return true;
            }
        }
        return false;
    }

    public ApprovalRequestUIModel getLatestApprovedRequestForUser(String userId) {
        for (ApprovalRequestUIModel r : approvalRequests) {
            if (r.userId.equals(userId) && "approved".equalsIgnoreCase(r.status)) {
                return r;
            }
        }
        return null;
    }

    public ApprovalRequestUIModel createDriverProfileChangeRequest(String userId, String userEmail, ProfileUIModel proposedChanges) {
        String id = UUID.randomUUID().toString();
        ApprovalRequestUIModel request = new ApprovalRequestUIModel(
                id,
                userId,
                userEmail,
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
