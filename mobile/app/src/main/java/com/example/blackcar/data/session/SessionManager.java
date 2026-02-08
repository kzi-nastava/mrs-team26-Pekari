package com.example.blackcar.data.session;

public final class SessionManager {

    private static String token;
    private static String email;
    private static String role;
    private static String userId;

    private SessionManager() {
    }

    public static void setSession(String tokenValue, String emailValue, String roleValue, String userIdValue) {
        token = tokenValue;
        email = emailValue;
        role = roleValue != null ? roleValue.toLowerCase() : null;
        userId = userIdValue;
    }

    public static String getToken() {
        return token;
    }

    public static String getEmail() {
        return email;
    }

    public static String getRole() {
        return role;
    }

    public static String getUserId() {
        return userId;
    }

    public static void clear() {
        token = null;
        email = null;
        role = null;
        userId = null;
    }
}
