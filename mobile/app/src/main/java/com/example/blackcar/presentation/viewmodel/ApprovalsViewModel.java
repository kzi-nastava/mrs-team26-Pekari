package com.example.blackcar.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.session.SessionManager;
import com.example.blackcar.domain.model.ApprovalRequest;
import com.example.blackcar.domain.model.UserRole;
import com.example.blackcar.domain.repository.ProfileRepository;

import java.util.Collections;
import java.util.List;

public final class ApprovalsViewModel extends ViewModel {
    public static final class State {
        public final boolean isLoading;
        @NonNull
        public final List<ApprovalRequest> requests;
        @Nullable
        public final String successMessage;
        @Nullable
        public final String errorMessage;

        public State(boolean isLoading, @NonNull List<ApprovalRequest> requests, @Nullable String successMessage, @Nullable String errorMessage) {
            this.isLoading = isLoading;
            this.requests = requests;
            this.successMessage = successMessage;
            this.errorMessage = errorMessage;
        }

        @NonNull
        public static State initial() {
            return new State(false, Collections.emptyList(), null, null);
        }
    }

    private final SessionManager sessionManager;
    private final ProfileRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(State.initial());

    public ApprovalsViewModel(@NonNull SessionManager sessionManager, @NonNull ProfileRepository repository) {
        this.sessionManager = sessionManager;
        this.repository = repository;
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    public void refresh() {
        if (sessionManager.getRole() != UserRole.ADMIN) {
            state.setValue(new State(false, Collections.emptyList(), null, "Only admins can view approvals."));
            return;
        }
        State current = getSafe();
        state.setValue(new State(true, current.requests, null, null));
        repository.getPendingApprovalRequests(new ProfileRepository.Callback<List<ApprovalRequest>>() {
            @Override
            public void onSuccess(@NonNull List<ApprovalRequest> data) {
                state.setValue(new State(false, data, null, null));
            }

            @Override
            public void onError(@NonNull String message) {
                state.setValue(new State(false, Collections.emptyList(), null, message));
            }
        });
    }

    public void approve(@NonNull String requestId) {
        State current = getSafe();
        state.setValue(new State(true, current.requests, null, null));
        repository.approveRequest(requestId, new ProfileRepository.Callback<String>() {
            @Override
            public void onSuccess(@NonNull String message) {
                state.setValue(new State(false, getSafe().requests, message, null));
                refresh();
            }

            @Override
            public void onError(@NonNull String message) {
                state.setValue(new State(false, getSafe().requests, null, message));
            }
        });
    }

    public void reject(@NonNull String requestId, @Nullable String reason) {
        State current = getSafe();
        state.setValue(new State(true, current.requests, null, null));
        repository.rejectRequest(requestId, reason, new ProfileRepository.Callback<String>() {
            @Override
            public void onSuccess(@NonNull String message) {
                state.setValue(new State(false, getSafe().requests, message, null));
                refresh();
            }

            @Override
            public void onError(@NonNull String message) {
                state.setValue(new State(false, getSafe().requests, null, message));
            }
        });
    }

    public void clearMessages() {
        State current = getSafe();
        state.setValue(new State(current.isLoading, current.requests, null, null));
    }

    @NonNull
    private State getSafe() {
        State current = state.getValue();
        return current != null ? current : State.initial();
    }
}
