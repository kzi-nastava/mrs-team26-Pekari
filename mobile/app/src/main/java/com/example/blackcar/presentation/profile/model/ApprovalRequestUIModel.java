package com.example.blackcar.presentation.profile.model;

public class ApprovalRequestUIModel {
    public final String id;
    public final String userId;
    public final String userEmail;
    public final ProfileUIModel changes;
    public final String status; // pending | approved | rejected
    public final String rejectionReason;

    public ApprovalRequestUIModel(
            String id,
            String userId,
            String userEmail,
            ProfileUIModel changes,
            String status,
            String rejectionReason
    ) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.changes = changes;
        this.status = status;
        this.rejectionReason = rejectionReason;
    }

    public ApprovalRequestUIModel copyWithStatus(String status, String rejectionReason) {
        return new ApprovalRequestUIModel(id, userId, userEmail, changes, status, rejectionReason);
    }
}
