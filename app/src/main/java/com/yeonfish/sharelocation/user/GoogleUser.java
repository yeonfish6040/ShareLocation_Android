package com.yeonfish.sharelocation.user;

public class GoogleUser {
    private String id;
    private String email;
    private String displayName;
    private String profilePicture;

    public void setId(String id) {
        this.id = id;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getId() {
        return this.id;
    }
    public String getEmail() {
        return this.email;
    }
    public String getDisplayName() {
        return this.displayName;
    }
    public String getProfilePicture() {
        return this.profilePicture;
    }
}
