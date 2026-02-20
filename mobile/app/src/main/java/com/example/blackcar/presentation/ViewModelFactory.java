package com.example.blackcar.presentation;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.blackcar.presentation.admin.viewmodel.AddDriverViewModel;
import com.example.blackcar.presentation.auth.viewmodel.LoginViewModel;
import com.example.blackcar.presentation.auth.viewmodel.RegisterViewModel;
import com.example.blackcar.presentation.home.viewmodel.DriverHomeViewModel;
import com.example.blackcar.presentation.home.viewmodel.PassengerHomeViewModel;
import com.example.blackcar.presentation.history.viewmodel.AdminHistoryViewModel;
import com.example.blackcar.presentation.history.viewmodel.AdminRideDetailViewModel;
import com.example.blackcar.presentation.history.viewmodel.PassengerHistoryViewModel;
import com.example.blackcar.presentation.chat.ChatViewModel;
import com.example.blackcar.presentation.home.viewmodel.RideTrackingViewModel;
import com.example.blackcar.presentation.profile.viewmodel.ProfileViewModel;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private final Context context;

    public ViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(context);
        } else if (modelClass.isAssignableFrom(RegisterViewModel.class)) {
            return (T) new RegisterViewModel(context);
        } else if (modelClass.isAssignableFrom(ProfileViewModel.class)) {
            return (T) new ProfileViewModel(context);
        } else if (modelClass.isAssignableFrom(PassengerHistoryViewModel.class)) {
            return (T) new PassengerHistoryViewModel();
        } else if (modelClass.isAssignableFrom(PassengerHomeViewModel.class)) {
            return (T) new PassengerHomeViewModel((Application) context.getApplicationContext());
        } else if (modelClass.isAssignableFrom(DriverHomeViewModel.class)) {
            return (T) new DriverHomeViewModel();
        } else if (modelClass.isAssignableFrom(AddDriverViewModel.class)) {
            return (T) new AddDriverViewModel(context);
        } else if (modelClass.isAssignableFrom(ChatViewModel.class)) {
            return (T) new ChatViewModel((Application) context.getApplicationContext());
        } else if (modelClass.isAssignableFrom(RideTrackingViewModel.class)) {
            return (T) new RideTrackingViewModel((Application) context.getApplicationContext());
        } else if (modelClass.isAssignableFrom(AdminRideDetailViewModel.class)) {
            return (T) new AdminRideDetailViewModel((Application) context.getApplicationContext());
        } else if (modelClass.isAssignableFrom(AdminHistoryViewModel.class)) {
            return (T) new AdminHistoryViewModel();
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
