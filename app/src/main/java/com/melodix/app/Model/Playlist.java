package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class Playlist {
    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("name")
    private String name;

    @SerializedName("cover_url")
    private String coverUrl;

    @SerializedName("is_public")
    private boolean isPublic;

    @SerializedName("created_at")
    private String createdAt;

    // Getters & Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public String getUserId() { return userId; }
}