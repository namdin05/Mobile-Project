package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class Comment {

    @SerializedName("id")
    public String id;

    @SerializedName("song_id")
    public String songId;

    @SerializedName("user_id")
    public String userId;

    @SerializedName("content")
    public String content;

    @SerializedName("created_at")
    public String createdAt;

    // Join từ bảng profiles
    @SerializedName("profiles")
    public Profile profiles;

    // Getter an toàn
    public String getDisplayName() {
        if (profiles != null && profiles.getDisplayName() != null) {
            return profiles.getDisplayName();
        }
        return "Người dùng";
    }

    public String getAvatarUrl() {
        return (profiles != null) ? profiles.getAvatarUrl() : null;
    }

    public String getUserId() {
        return userId;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}