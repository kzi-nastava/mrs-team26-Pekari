package com.example.blackcar.data.session;

public final class SessionManager {

    private static String token;
    private static String email;
    private static String role;
    private static String userId;
    private static boolean blocked;

    private SessionManager() {
    }

    public static void setSession(String tokenValue, String emailValue, String roleValue, String userIdValue, boolean blockedValue) {
        token = tokenValue;
        email = emailValue;
        role = roleValue != null ? roleValue.toLowerCase() : null;
        userId = userIdValue;
        blocked = blockedValue;
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

    public static boolean isBlocked() {
        return blocked;
    }

    public static void setBlocked(boolean blockedValue) {
        blocked = blockedValue;
    }

    public static void clear() {
        token = null;
        email = null;
        role = null;
        userId = null;
        blocked = false;
    }
}
