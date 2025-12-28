package com.example.blackcar.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.session.SessionManager;
import com.example.blackcar.domain.model.DriverInfo;
import com.example.blackcar.domain.model.PasswordChangeRequest;
import com.example.blackcar.domain.model.ProfileData;
import com.example.blackcar.domain.model.ProfileUpdateRequest;
import com.example.blackcar.domain.model.UserRole;
import com.example.blackcar.domain.repository.ProfileRepository;

public final class ProfileViewModel extends ViewModel {
    private final SessionManager sessionManager;
    private final ProfileRepository repository;

    private final MutableLiveData<ProfileViewState> state = new MutableLiveData<>();

    public ProfileViewModel(
            @NonNull SessionManager sessionManager,
            @NonNull ProfileRepository repository
    ) {
        this.sessionManager = sessionManager;
        this.repository = repository;
        state.setValue(ProfileViewState.initial(sessionManager.getRole()));
    }

    @NonNull
    public LiveData<ProfileViewState> getState() {
        return state;
    }

    public void refresh() {
        UserRole role = sessionManager.getRole();
        updateState(new ProfileViewState(true, getSafe().isEditing, getSafe().profile, getSafe().driverInfo, null, null, role));

        repository.getProfile(new ProfileRepository.Callback<ProfileData>() {
            @Override
            public void onSuccess(@NonNull ProfileData data) {
                ProfileViewState current = getSafe();
                updateState(new ProfileViewState(false, current.isEditing, data, current.driverInfo, current.successMessage, null, role));
                if (role == UserRole.DRIVER) {
                    loadDriverInfo();
                } else {
                    clearDriverInfo();
                }
            }

            @Override
            public void onError(@NonNull String message) {
                ProfileViewState current = getSafe();
                updateState(new ProfileViewState(false, current.isEditing, current.profile, current.driverInfo, null, message, role));
            }
        });
    }

    public void toggleEdit() {
        ProfileViewState current = getSafe();
        boolean next = !current.isEditing;
        updateState(new ProfileViewState(current.isLoading, next, current.profile, current.driverInfo, null, null, current.role));
    }

    public void submitProfileUpdate(
            @NonNull String firstName,
            @NonNull String lastName,
            @NonNull String phoneNumber,
            @NonNull String address,
            @Nullable String profilePicture
    ) {
        ProfileViewState current = getSafe();
        updateState(new ProfileViewState(true, current.isEditing, current.profile, current.driverInfo, null, null, current.role));

        ProfileUpdateRequest request = new ProfileUpdateRequest(firstName, lastName, phoneNumber, address, profilePicture);
        repository.updateProfile(request, new ProfileRepository.Callback<String>() {
            @Override
            public void onSuccess(@NonNull String message) {
                ProfileViewState now = getSafe();
                updateState(new ProfileViewState(false, false, now.profile, now.driverInfo, message, null, now.role));
                refresh();
            }

            @Override
            public void onError(@NonNull String message) {
                ProfileViewState now = getSafe();
                updateState(new ProfileViewState(false, now.isEditing, now.profile, now.driverInfo, null, message, now.role));
            }
        });
    }

    public void submitPasswordChange(
            @NonNull String currentPassword,
            @NonNull String newPassword,
            @NonNull String confirmPassword
    ) {
        ProfileViewState current = getSafe();
        updateState(new ProfileViewState(true, current.isEditing, current.profile, current.driverInfo, null, null, current.role));
        PasswordChangeRequest request = new PasswordChangeRequest(currentPassword, newPassword, confirmPassword);

        repository.changePassword(request, new ProfileRepository.Callback<String>() {
            @Override
            public void onSuccess(@NonNull String message) {
                ProfileViewState now = getSafe();
                updateState(new ProfileViewState(false, now.isEditing, now.profile, now.driverInfo, message, null, now.role));
            }

            @Override
            public void onError(@NonNull String message) {
                ProfileViewState now = getSafe();
                updateState(new ProfileViewState(false, now.isEditing, now.profile, now.driverInfo, null, message, now.role));
            }
        });
    }

    public void clearMessages() {
        ProfileViewState current = getSafe();
        updateState(new ProfileViewState(current.isLoading, current.isEditing, current.profile, current.driverInfo, null, null, current.role));
    }

    public void devSetRole(@NonNull UserRole role) {
        sessionManager.setRole(role);
        ProfileViewState current = getSafe();
        updateState(ProfileViewState.initial(role));
        refresh();
    }

    private void loadDriverInfo() {
        repository.getDriverInfo(new ProfileRepository.Callback<DriverInfo>() {
            @Override
            public void onSuccess(@NonNull DriverInfo data) {
                ProfileViewState current = getSafe();
                updateState(new ProfileViewState(current.isLoading, current.isEditing, current.profile, data, current.successMessage, current.errorMessage, current.role));
            }

            @Override
            public void onError(@NonNull String message) {
                // keep silent for now
            }
        });
    }

    private void clearDriverInfo() {
        ProfileViewState current = getSafe();
        updateState(new ProfileViewState(current.isLoading, current.isEditing, current.profile, null, current.successMessage, current.errorMessage, current.role));
    }

    @NonNull
    private ProfileViewState getSafe() {
        ProfileViewState current = state.getValue();
        if (current == null) {
            return ProfileViewState.initial(sessionManager.getRole());
        }
        return current;
    }

    private void updateState(@NonNull ProfileViewState newState) {
        state.setValue(newState);
    }
}
