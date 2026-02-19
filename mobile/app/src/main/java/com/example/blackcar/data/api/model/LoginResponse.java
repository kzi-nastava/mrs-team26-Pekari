package com.example.blackcar.data.api.model;

public class LoginResponse {
    private String token;
    private String id;
    private String userId;
    private String email;
    private String role;
    private Boolean blocked;

    // Getters
    public String getToken() { return token; }
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Boolean getBlocked() { return blocked; }
}
