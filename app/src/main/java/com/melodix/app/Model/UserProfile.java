package com.melodix.app.Model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.melodix.app.Utils.AppConstants;

import java.io.Serializable;

public class UserProfile implements Serializable {

    private String uid;
    private String fullName;
    private String email;
    private String photoUrl;
    private String role;
    private String provider;
    private boolean active;
    private boolean emailVerified;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp lastLoginAt;

    public UserProfile() {
    }

    public UserProfile(String uid, String fullName, String email, String photoUrl, String role,
                       String provider, boolean active, boolean emailVerified,
                       Timestamp createdAt, Timestamp updatedAt, Timestamp lastLoginAt) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.role = role;
        this.provider = provider;
        this.active = active;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLoginAt = lastLoginAt;
    }

    public static UserProfile fromDocument(DocumentSnapshot document) {
        UserProfile profile = new UserProfile();

        if (document == null || !document.exists()) {
            return profile;
        }

        try {
            profile.setUid(getSafeString(document, "uid"));
            profile.setFullName(getSafeString(document, "fullName"));
            profile.setEmail(getSafeString(document, "email"));
            profile.setPhotoUrl(getSafeString(document, "photoUrl"));
            profile.setRole(getSafeString(document, "role"));
            profile.setProvider(getSafeString(document, "provider"));

            Boolean activeValue = document.getBoolean("active");
            Boolean emailVerifiedValue = document.getBoolean("emailVerified");

            profile.setActive(activeValue == null || activeValue);
            profile.setEmailVerified(emailVerifiedValue != null && emailVerifiedValue);

            profile.setCreatedAt(document.getTimestamp("createdAt"));
            profile.setUpdatedAt(document.getTimestamp("updatedAt"));
            profile.setLastLoginAt(document.getTimestamp("lastLoginAt"));
        } catch (Exception ignored) {
        }

        return profile;
    }

    private static String getSafeString(DocumentSnapshot document, String key) {
        try {
            String value = document.getString(key);
            return value == null ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }

    public String getReadableProvider() {
        if (AppConstants.PROVIDER_GOOGLE.equals(provider)) {
            return "Google";
        }
        if (AppConstants.PROVIDER_FACEBOOK.equals(provider)) {
            return "Facebook";
        }
        if (AppConstants.PROVIDER_EMAIL.equals(provider)) {
            return "Email/Password";
        }
        return provider == null || provider.trim().isEmpty() ? "Không xác định" : provider;
    }

    public String getDisplayNameOrEmail() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        if (email != null && !email.trim().isEmpty()) {
            return email;
        }
        return "Melodix User";
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Timestamp getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Timestamp lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}