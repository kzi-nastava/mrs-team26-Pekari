package com.example.blackcar;

import android.app.Application;

import androidx.annotation.NonNull;

import com.example.blackcar.data.repository.InMemoryProfileRepository;
import com.example.blackcar.data.session.SessionManager;
import com.example.blackcar.domain.repository.ProfileRepository;

public final class BlackCarApp extends Application {
    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
    }

    @NonNull
    public AppContainer getAppContainer() {
        return appContainer;
    }

    public static final class AppContainer {
        private final SessionManager sessionManager;
        private final ProfileRepository profileRepository;

        public AppContainer(@NonNull Application application) {
            sessionManager = new SessionManager(application);
            profileRepository = new InMemoryProfileRepository(sessionManager);
        }

        @NonNull
        public SessionManager getSessionManager() {
            return sessionManager;
        }

        @NonNull
        public ProfileRepository getProfileRepository() {
            return profileRepository;
        }
    }
}
