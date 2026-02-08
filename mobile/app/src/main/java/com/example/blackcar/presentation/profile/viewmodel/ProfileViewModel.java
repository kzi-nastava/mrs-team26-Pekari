package com.example.blackcar.presentation.profile.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.repository.AuthRepository;
import com.example.blackcar.presentation.profile.data.MockProfileStore;
import com.example.blackcar.presentation.profile.model.ApprovalRequestUIModel;
import com.example.blackcar.presentation.profile.model.DriverInfoUIModel;
import com.example.blackcar.presentation.profile.model.ProfileUIModel;
import com.example.blackcar.presentation.profile.viewstate.ProfileViewState;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {

    private final MutableLiveData<ProfileViewState> state = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutSuccess = new MutableLiveData<>(false);
    private final MockProfileStore store = MockProfileStore.getInstance();
    private final AuthRepository authRepository = new AuthRepository();

    public ProfileViewModel() {
        load();
    }

    public LiveData<ProfileViewState> getState() {
        return state;
    }

    public LiveData<Boolean> getLogoutSuccess() {
        return logoutSuccess;
    }

    public void load() {
        ProfileUIModel profile = store.getCurrentProfile();
        DriverInfoUIModel driverInfo = store.getDriverInfo();
        List<ApprovalRequestUIModel> requests = store.getApprovalRequests();

        String banner = null;
        if ("driver".equalsIgnoreCase(profile.role) && store.hasPendingRequestForCurrentUser()) {
            banner = "Changes sent for admin approval. Current profile remains unchanged.";
        }

        state.setValue(new ProfileViewState(
                false,
                false,
                null,
                false,
                banner,
                profile,
                driverInfo,
                requests
        ));
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
        ProfileUIModel currentProfile = store.getCurrentProfile();

        if ("driver".equalsIgnoreCase(currentProfile.role)) {
            store.createDriverProfileChangeRequest(proposed);
            load();
            setEditing(false);
            return;
        }

        // admin/passenger: apply immediately
        store.setCurrentProfile(proposed);
        load();
        setEditing(false);
    }

    public boolean approveRequest(String requestId) {
        boolean ok = store.approveRequest(requestId);
        load();
        return ok;
    }

    public boolean rejectRequest(String requestId, String reason) {
        boolean ok = store.rejectRequest(requestId, reason);
        load();
        return ok;
    }

    public List<ApprovalRequestUIModel> getPendingApprovalRequestsOnly() {
        List<ApprovalRequestUIModel> all = store.getApprovalRequests();
        List<ApprovalRequestUIModel> pending = new ArrayList<>();
        for (ApprovalRequestUIModel r : all) {
            if (r != null && "pending".equalsIgnoreCase(r.status)) {
                pending.add(r);
            }
        }
        return pending;
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
}
