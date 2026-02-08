package com.example.blackcar.presentation;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.blackcar.presentation.auth.viewmodel.LoginViewModel;
import com.example.blackcar.presentation.history.viewmodel.DriverHistoryViewModel;

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
        } else if (modelClass.isAssignableFrom(DriverHistoryViewModel.class)) {
            return (T) new DriverHistoryViewModel();
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
