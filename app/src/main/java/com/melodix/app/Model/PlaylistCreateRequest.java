package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class PlaylistCreateRequest {

    @SerializedName("name")
    private String name;

    @SerializedName("cover_url")
    private String coverUrl;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("is_public")
    private boolean isPublic = true;

    public PlaylistCreateRequest(String name, String coverUrl) {
        this.name = name;
        this.coverUrl = coverUrl;
    }

    // Constructor
    public PlaylistCreateRequest(String name, String coverUrl, String userId) {
        this.name = name;
        this.coverUrl = coverUrl;
        this.userId = userId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}