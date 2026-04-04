package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class Genre {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("cover_url")
    private String coverUrl;

    @SerializedName("is_visible")
    private boolean isVisible;


    public String getId() { return id; }
    public String getName() { return name; }
    public String getCoverUrl() { return coverUrl; }

    public boolean isVisible() { return isVisible; }
}
