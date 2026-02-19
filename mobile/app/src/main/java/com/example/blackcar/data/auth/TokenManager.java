package com.example.blackcar.data.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {

    private static final String PREFS_NAME = "blackcar_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    private static TokenManager instance;
    private final SharedPreferences prefs;

    private TokenManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context);
        }
        return instance;
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public void saveRole(String role) {
        prefs.edit().putString(KEY_ROLE, role).apply();
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public void clearToken() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_ROLE)
                .apply();
    }

    public boolean hasToken() {
        return getToken() != null;
    }

    /**
     * Save FCM token for push notifications.
     */
    public void saveFcmToken(String fcmToken) {
        prefs.edit().putString(KEY_FCM_TOKEN, fcmToken).apply();
    }

    /**
     * Get stored FCM token.
     */
    public String getFcmToken() {
        return prefs.getString(KEY_FCM_TOKEN, null);
    }

    /**
     * Check if user is an admin.
     */
    public boolean isAdmin() {
        String role = getRole();
        return role != null && role.equalsIgnoreCase("ADMIN");
    }
}
