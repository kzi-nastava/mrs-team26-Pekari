package com.example.blackcar.presentation.profile.viewstate;

import com.example.blackcar.presentation.profile.model.ApprovalRequestUIModel;
import com.example.blackcar.presentation.profile.model.DriverInfoUIModel;
import com.example.blackcar.presentation.profile.model.ProfileUIModel;

import java.util.List;

public class ProfileViewState {
    public final boolean loading;
    public final boolean error;
    public final String errorMessage;

    public final boolean isEditing;
    public final String bannerMessage;
    /** When true, style banner as danger (e.g. for blocked users). */
    public final boolean bannerDanger;

    public final ProfileUIModel profile;
    public final DriverInfoUIModel driverInfo;
    public final List<ApprovalRequestUIModel> approvalRequests;

    public ProfileViewState(
            boolean loading,
            boolean error,
            String errorMessage,
            boolean isEditing,
            String bannerMessage,
            boolean bannerDanger,
            ProfileUIModel profile,
            DriverInfoUIModel driverInfo,
            List<ApprovalRequestUIModel> approvalRequests
    ) {
        this.loading = loading;
        this.error = error;
        this.errorMessage = errorMessage;
        this.isEditing = isEditing;
        this.bannerMessage = bannerMessage;
        this.bannerDanger = bannerDanger;
        this.profile = profile;
        this.driverInfo = driverInfo;
        this.approvalRequests = approvalRequests;
    }
}
