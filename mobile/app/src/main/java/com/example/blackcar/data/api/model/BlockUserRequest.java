package com.example.blackcar.data.api.model;

import com.google.gson.annotations.SerializedName;

public class BlockUserRequest {
    private Boolean blocked;
    @SerializedName("blockedNote")
    private String blockedNote;

    public BlockUserRequest(Boolean blocked, String blockedNote) {
        this.blocked = blocked;
        this.blockedNote = blockedNote;
    }

    public Boolean getBlocked() { return blocked; }
    public String getBlockedNote() { return blockedNote; }
}
