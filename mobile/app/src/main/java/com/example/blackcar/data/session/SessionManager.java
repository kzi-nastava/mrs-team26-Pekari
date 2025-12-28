package com.example.blackcar.data.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.blackcar.domain.model.User;
import com.example.blackcar.domain.model.UserRole;

public final class SessionManager {
    private static final String PREFS_NAME = "blackcar_session";
    private static final String KEY_ROLE = "role";

    private final SharedPreferences prefs;

    public SessionManager(@NonNull Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public User getCurrentUser() {
        UserRole role = getRole();

        if (role == UserRole.ADMIN) {
            return new User("3", "admin@example.com", "adminuser", "Admin", "User", UserRole.ADMIN);
        }
        if (role == UserRole.DRIVER) {
            return new User("1", "driver@example.com", "driveruser", "John", "Doe", UserRole.DRIVER);
        }
        return new User("2", "passenger@example.com", "passengeruser", "Jane", "Smith", UserRole.PASSENGER);
    }

    @NonNull
    public UserRole getRole() {
        String stored = prefs.getString(KEY_ROLE, UserRole.DRIVER.name());
        try {
            return UserRole.valueOf(stored);
        } catch (Exception ignored) {
            return UserRole.DRIVER;
        }
    }

    public void setRole(@NonNull UserRole role) {
        prefs.edit().putString(KEY_ROLE, role.name()).apply();
    }
}
