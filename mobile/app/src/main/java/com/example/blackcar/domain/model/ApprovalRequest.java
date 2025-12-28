package com.example.blackcar.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ApprovalRequest {
    @NonNull
    private final String id;
    @NonNull
    private final String userId;
    @NonNull
    private final ProfileUpdateRequest changes;
    @NonNull
    private final ApprovalStatus status;
    private final long createdAtEpochMs;
    @Nullable
    private final Long reviewedAtEpochMs;
    @Nullable
    private final String reviewedBy;
    @Nullable
    private final String rejectionReason;

    public ApprovalRequest(
            @NonNull String id,
            @NonNull String userId,
            @NonNull ProfileUpdateRequest changes,
            @NonNull ApprovalStatus status,
            long createdAtEpochMs,
            @Nullable Long reviewedAtEpochMs,
            @Nullable String reviewedBy,
            @Nullable String rejectionReason
    ) {
        this.id = id;
        this.userId = userId;
        this.changes = changes;
        this.status = status;
        this.createdAtEpochMs = createdAtEpochMs;
        this.reviewedAtEpochMs = reviewedAtEpochMs;
        this.reviewedBy = reviewedBy;
        this.rejectionReason = rejectionReason;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    @NonNull
    public ProfileUpdateRequest getChanges() {
        return changes;
    }

    @NonNull
    public ApprovalStatus getStatus() {
        return status;
    }

    public long getCreatedAtEpochMs() {
        return createdAtEpochMs;
    }

    @Nullable
    public Long getReviewedAtEpochMs() {
        return reviewedAtEpochMs;
    }

    @Nullable
    public String getReviewedBy() {
        return reviewedBy;
    }

    @Nullable
    public String getRejectionReason() {
        return rejectionReason;
    }

    @NonNull
    public ApprovalRequest withStatus(
            @NonNull ApprovalStatus status,
            @Nullable Long reviewedAtEpochMs,
            @Nullable String reviewedBy,
            @Nullable String rejectionReason
    ) {
        return new ApprovalRequest(id, userId, changes, status, createdAtEpochMs, reviewedAtEpochMs, reviewedBy, rejectionReason);
    }
}
