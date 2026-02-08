package com.example.blackcar.presentation.profile.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.repository.AuthRepository;
import com.example.blackcar.data.repository.ProfileRepository;
import com.example.blackcar.presentation.profile.data.ApprovalRequestStore;
import com.example.blackcar.presentation.profile.model.ApprovalRequestUIModel;
import com.example.blackcar.presentation.profile.model.DriverInfoUIModel;
import com.example.blackcar.presentation.profile.model.ProfileUIModel;
import com.example.blackcar.presentation.profile.viewstate.ProfileViewState;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {

    private final MutableLiveData<ProfileViewState> state = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> logoutSuccess = new MutableLiveData<>(false);
    private final ApprovalRequestStore approvalStore = ApprovalRequestStore.getInstance();
    private final AuthRepository authRepository;
    private final ProfileRepository profileRepository = new ProfileRepository();

    private ProfileUIModel cachedProfile;
    private DriverInfoUIModel cachedDriverInfo;

    public ProfileViewModel(Context context) {
        this.authRepository = new AuthRepository(context);
        load();
    }

    public LiveData<ProfileViewState> getState() {
        return state;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<Boolean> getLogoutSuccess() {
        return logoutSuccess;
    }

    public void load() {
        state.setValue(new ProfileViewState(
                true,
                false,
                null,
                false,
                null,
                cachedProfile,
                cachedDriverInfo,
                approvalStore.getApprovalRequests()
        ));

        profileRepository.getProfile(new ProfileRepository.RepoCallback<ProfileRepository.ProfilePayload>() {
            @Override
            public void onSuccess(ProfileRepository.ProfilePayload data) {
                cachedProfile = data.profile;
                cachedDriverInfo = data.driverInfo;
                String banner = null;
                if (cachedProfile != null && "driver".equalsIgnoreCase(cachedProfile.role)) {
                    ApprovalRequestUIModel approved = approvalStore.getLatestApprovedRequestForUser(cachedProfile.id);
                    if (approved != null && approved.changes != null) {
                        cachedProfile = approved.changes;
                    }
                    if (approvalStore.hasPendingRequestForUser(cachedProfile.id)) {
                        banner = "Changes sent for admin approval. Current profile remains unchanged.";
                    }
                }

                state.postValue(new ProfileViewState(
                        false,
                        false,
                        null,
                        false,
                        banner,
                        cachedProfile,
                        cachedDriverInfo,
                        approvalStore.getApprovalRequests()
                ));
            }

            @Override
            public void onError(String message) {
                state.postValue(new ProfileViewState(
                        false,
                        true,
                        message,
                        false,
                        null,
                        cachedProfile,
                        cachedDriverInfo,
                        approvalStore.getApprovalRequests()
                ));
            }
        });
    }

    public void setEditing(boolean editing) {
        ProfileViewState current = state.getValue();
        if (current == null) {
            load();
            return;
        }

        state.setValue(new ProfileViewState(
                current.loading,
                current.error,
                current.errorMessage,
                editing,
                current.bannerMessage,
                current.profile,
                current.driverInfo,
                current.approvalRequests
        ));
    }

    public void updateLocalProfilePicture(String uriString) {
        ProfileViewState current = state.getValue();
        if (current == null || current.profile == null) {
            return;
        }

        ProfileUIModel updated = current.profile.copyWith(
                current.profile.firstName,
                current.profile.lastName,
                current.profile.phoneNumber,
                current.profile.address,
                uriString
        );

        // For UI: reflect immediately in edit mode preview
        cachedProfile = updated;
        state.setValue(new ProfileViewState(
                current.loading,
                current.error,
                current.errorMessage,
                current.isEditing,
                current.bannerMessage,
                updated,
                current.driverInfo,
                current.approvalRequests
        ));
    }

    public void submitChanges(ProfileUIModel proposed) {
        ProfileViewState current = state.getValue();
        if (current == null || current.profile == null) {
            toastMessage.postValue("Profile not loaded");
            return;
        }

        if ("driver".equalsIgnoreCase(current.profile.role)) {
            approvalStore.createDriverProfileChangeRequest(
                    current.profile.id,
                    current.profile.email,
                    proposed
            );

            String banner = "Changes sent for admin approval. Current profile remains unchanged.";
            cachedProfile = current.profile;
            state.postValue(new ProfileViewState(
                    false,
                    false,
                    null,
                    false,
                    banner,
                    current.profile,
                    current.driverInfo,
                    approvalStore.getApprovalRequests()
            ));
            toastMessage.postValue("Request sent for admin approval");
            return;
        }

        profileRepository.updateProfile(proposed, new ProfileRepository.RepoCallback<String>() {
            @Override
            public void onSuccess(String message) {
                cachedProfile = proposed;
                state.postValue(new ProfileViewState(
                        false,
                        false,
                        null,
                        false,
                        null,
                        proposed,
                        current.driverInfo,
                        approvalStore.getApprovalRequests()
                ));
                toastMessage.postValue(message != null ? message : "Profile updated");
            }

            @Override
            public void onError(String message) {
                toastMessage.postValue(message != null ? message : "Failed to update profile");
            }
        });
    }

    public boolean approveRequest(String requestId) {
        boolean ok = approvalStore.approveRequest(requestId);
        refreshApprovalsOnly();
        return ok;
    }

    public boolean rejectRequest(String requestId, String reason) {
        boolean ok = approvalStore.rejectRequest(requestId, reason);
        refreshApprovalsOnly();
        return ok;
    }

    public List<ApprovalRequestUIModel> getPendingApprovalRequestsOnly() {
        List<ApprovalRequestUIModel> all = approvalStore.getApprovalRequests();
        List<ApprovalRequestUIModel> pending = new ArrayList<>();
        for (ApprovalRequestUIModel r : all) {
            if (r != null && "pending".equalsIgnoreCase(r.status)) {
                pending.add(r);
            }
        }
        return pending;
    }

    public void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        profileRepository.changePassword(currentPassword, newPassword, confirmPassword,
                new ProfileRepository.RepoCallback<String>() {
                    @Override
                    public void onSuccess(String message) {
                        toastMessage.postValue(message != null ? message : "Password changed");
                    }

                    @Override
                    public void onError(String message) {
                        toastMessage.postValue(message != null ? message : "Failed to change password");
                    }
                });
    }

    public void clearToastMessage() {
        toastMessage.setValue(null);
    }

    public void logout() {
        authRepository.logout(new AuthRepository.RepoCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                logoutSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                // We consider logout successful locally regardless of server error
                logoutSuccess.postValue(true);
            }
        });
    }

    private void refreshApprovalsOnly() {
        ProfileViewState current = state.getValue();
        if (current == null) {
            return;
        }
        state.postValue(new ProfileViewState(
                current.loading,
                current.error,
                current.errorMessage,
                current.isEditing,
                current.bannerMessage,
                current.profile,
                current.driverInfo,
                approvalStore.getApprovalRequests()
        ));
    }
}
