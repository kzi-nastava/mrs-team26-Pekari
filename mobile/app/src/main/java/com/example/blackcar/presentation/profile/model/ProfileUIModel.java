package com.example.blackcar.presentation.profile.model;

public class ProfileUIModel {
    public final String id;
    public final String email;
    public final String username;
    public final String firstName;
    public final String lastName;
    public final String phoneNumber;
    public final String address;
    /** Expected: admin | passenger | driver */
    public final String role;

    /** Local-only URI string, or null. */
    public final String profilePictureUri;

    /** True if user has been blocked by admin. */
    public final Boolean blocked;

    /** Admin note explaining why user was blocked. */
    public final String blockedNote;

    public ProfileUIModel(
            String id,
            String email,
            String username,
            String firstName,
            String lastName,
            String phoneNumber,
            String address,
            String role,
            String profilePictureUri,
            Boolean blocked,
            String blockedNote
    ) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.role = role;
        this.profilePictureUri = profilePictureUri;
        this.blocked = blocked;
        this.blockedNote = blockedNote;
    }

    public ProfileUIModel copyWith(
            String firstName,
            String lastName,
            String phoneNumber,
            String address,
            String profilePictureUri
    ) {
        return new ProfileUIModel(
                id,
                email,
                username,
                firstName,
                lastName,
                phoneNumber,
                address,
                role,
                profilePictureUri,
                blocked,
                blockedNote
        );
    }
}
