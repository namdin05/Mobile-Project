package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class PlaylistCreateRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("cover_url")
    private String coverUrl;

    @SerializedName("is_public")
    private boolean isPublic = true;

    public PlaylistCreateRequest(String name, String coverUrl) {
        this.name = name;
        this.coverUrl = coverUrl;
    }

    public PlaylistCreateRequest(String name, String coverUrl, boolean isPublic) {
        this.name = name;
        this.coverUrl = coverUrl;
        this.isPublic = isPublic;
    }
}