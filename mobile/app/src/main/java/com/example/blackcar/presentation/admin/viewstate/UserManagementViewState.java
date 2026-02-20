package com.example.blackcar.presentation.admin.viewstate;

import com.example.blackcar.data.api.model.UserListItemResponse;

import java.util.ArrayList;
import java.util.List;

public class UserManagementViewState {
    public final boolean loading;
    public final boolean error;
    public final String errorMessage;
    public final String successMessage;
    public final List<UserListItemResponse> drivers;
    public final List<UserListItemResponse> passengers;

    public UserManagementViewState(
            boolean loading,
            boolean error,
            String errorMessage,
            String successMessage,
            List<UserListItemResponse> drivers,
            List<UserListItemResponse> passengers
    ) {
        this.loading = loading;
        this.error = error;
        this.errorMessage = errorMessage;
        this.successMessage = successMessage;
        this.drivers = drivers != null ? drivers : new ArrayList<>();
        this.passengers = passengers != null ? passengers : new ArrayList<>();
    }
}
