package com.example.blackcar.data.api.model;

import com.google.gson.annotations.SerializedName;

public class UserListItemResponse {
    private String id;
    private String email;
    @SerializedName("firstName")
    private String firstName;
    @SerializedName("lastName")
    private String lastName;
    private String role;
    private Boolean blocked;
    @SerializedName("blockedNote")
    private String blockedNote;

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getRole() { return role; }
    public Boolean getBlocked() { return blocked; }
    public String getBlockedNote() { return blockedNote; }
}
