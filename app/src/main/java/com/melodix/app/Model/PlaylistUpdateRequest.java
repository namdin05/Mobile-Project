package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class PlaylistUpdateRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("cover_url")
    private String coverUrl;

    @SerializedName("is_public")
    private Boolean isPublic;

    public PlaylistUpdateRequest(String name, String coverUrl) {
        this.name = name;
        this.coverUrl = coverUrl;
    }
}