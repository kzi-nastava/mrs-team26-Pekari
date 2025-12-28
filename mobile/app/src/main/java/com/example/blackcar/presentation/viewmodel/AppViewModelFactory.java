package com.example.blackcar.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.blackcar.data.session.SessionManager;
import com.example.blackcar.domain.repository.ProfileRepository;

public final class AppViewModelFactory implements ViewModelProvider.Factory {
    private final SessionManager sessionManager;
    private final ProfileRepository profileRepository;

    public AppViewModelFactory(
            @NonNull SessionManager sessionManager,
            @NonNull ProfileRepository profileRepository
    ) {
        this.sessionManager = sessionManager;
        this.profileRepository = profileRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ProfileViewModel.class)) {
            //noinspection unchecked
            return (T) new ProfileViewModel(sessionManager, profileRepository);
        }
        if (modelClass.isAssignableFrom(ApprovalsViewModel.class)) {
            //noinspection unchecked
            return (T) new ApprovalsViewModel(sessionManager, profileRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
