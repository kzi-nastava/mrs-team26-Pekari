package com.example.blackcar.presentation.admin.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.UserListItemResponse;
import com.example.blackcar.data.repository.AdminRepository;
import com.example.blackcar.presentation.admin.viewstate.UserManagementViewState;

import java.util.ArrayList;
import java.util.List;

public class UserManagementViewModel extends ViewModel {

    private final MutableLiveData<UserManagementViewState> state = new MutableLiveData<>();
    private final AdminRepository repository = new AdminRepository();

    public LiveData<UserManagementViewState> getState() {
        return state;
    }

    public void loadAll() {
        state.setValue(new UserManagementViewState(true, false, null, null, new ArrayList<>(), new ArrayList<>()));

        repository.getDrivers(new AdminRepository.RepoCallback<List<UserListItemResponse>>() {
            @Override
            public void onSuccess(List<UserListItemResponse> drivers) {
                repository.getPassengers(new AdminRepository.RepoCallback<List<UserListItemResponse>>() {
                    @Override
                    public void onSuccess(List<UserListItemResponse> passengers) {
                        state.postValue(new UserManagementViewState(
                                false, false, null, null, drivers, passengers));
                    }

                    @Override
                    public void onError(String message) {
                        state.postValue(new UserManagementViewState(
                                false, true, message, null, drivers, new ArrayList<>()));
                    }
                });
            }

            @Override
            public void onError(String message) {
                repository.getPassengers(new AdminRepository.RepoCallback<List<UserListItemResponse>>() {
                    @Override
                    public void onSuccess(List<UserListItemResponse> passengers) {
                        state.postValue(new UserManagementViewState(
                                false, true, message, null, new ArrayList<>(), passengers));
                    }

                    @Override
                    public void onError(String msg) {
                        state.postValue(new UserManagementViewState(
                                false, true, message != null ? message : msg, null,
                                new ArrayList<>(), new ArrayList<>()));
                    }
                });
            }
        });
    }

    public void blockUser(UserListItemResponse user, String note) {
        String noteTrimmed = note != null ? note.trim() : "";
        String finalNote = noteTrimmed.isEmpty() ? null : noteTrimmed;

        repository.setUserBlock(user.getId(), true, finalNote, new AdminRepository.RepoCallback<String>() {
            @Override
            public void onSuccess(String msg) {
                loadAllWithSuccess(msg != null ? msg : "User blocked.");
            }

            @Override
            public void onError(String message) {
                UserManagementViewState current = state.getValue();
                state.postValue(new UserManagementViewState(
                        false, true, message, null,
                        current != null ? current.drivers : new ArrayList<>(),
                        current != null ? current.passengers : new ArrayList<>()
                ));
            }
        });
    }

    public void unblockUser(UserListItemResponse user) {
        repository.setUserBlock(user.getId(), false, null, new AdminRepository.RepoCallback<String>() {
            @Override
            public void onSuccess(String msg) {
                loadAllWithSuccess(msg != null ? msg : "User unblocked.");
            }

            @Override
            public void onError(String message) {
                UserManagementViewState current = state.getValue();
                state.postValue(new UserManagementViewState(
                        false, true, message, null,
                        current != null ? current.drivers : new ArrayList<>(),
                        current != null ? current.passengers : new ArrayList<>()
                ));
            }
        });
    }

    private void loadAllWithSuccess(String successMessage) {
        state.setValue(new UserManagementViewState(true, false, null, null, new ArrayList<>(), new ArrayList<>()));

        repository.getDrivers(new AdminRepository.RepoCallback<List<UserListItemResponse>>() {
            @Override
            public void onSuccess(List<UserListItemResponse> drivers) {
                repository.getPassengers(new AdminRepository.RepoCallback<List<UserListItemResponse>>() {
                    @Override
                    public void onSuccess(List<UserListItemResponse> passengers) {
                        state.postValue(new UserManagementViewState(
                                false, false, null, successMessage, drivers, passengers));
                    }

                    @Override
                    public void onError(String message) {
                        state.postValue(new UserManagementViewState(
                                false, true, message, null, drivers, new ArrayList<>()));
                    }
                });
            }

            @Override
            public void onError(String message) {
                repository.getPassengers(new AdminRepository.RepoCallback<List<UserListItemResponse>>() {
                    @Override
                    public void onSuccess(List<UserListItemResponse> passengers) {
                        state.postValue(new UserManagementViewState(
                                false, true, message, null, new ArrayList<>(), passengers));
                    }

                    @Override
                    public void onError(String msg) {
                        state.postValue(new UserManagementViewState(
                                false, true, message != null ? message : msg, null,
                                new ArrayList<>(), new ArrayList<>()));
                    }
                });
            }
        });
    }

    public void clearMessages() {
        UserManagementViewState current = state.getValue();
        if (current != null) {
            state.setValue(new UserManagementViewState(
                    current.loading, current.error, null, null,
                    current.drivers, current.passengers));
        }
    }

    public static String fullName(UserListItemResponse user) {
        if (user == null) return "";
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        if (!first.isEmpty() || !last.isEmpty()) {
            return (first + " " + last).trim();
        }
        return user.getEmail() != null ? user.getEmail() : "";
    }

    public static String notePreview(String note, int maxLen) {
        if (note == null || note.trim().isEmpty()) return "—";
        String trimmed = note.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen) + "…";
    }
}
