package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class Banner {
    @SerializedName("id")
    private String id;
    @SerializedName("image_url")
    private String cover_url;
    @SerializedName("title")
    private String title;

    public String getCover_url() {
        return cover_url;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
