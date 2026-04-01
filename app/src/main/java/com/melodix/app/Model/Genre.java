package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class Genre {
    @SerializedName("id")
    private int id;
    @SerializedName("name")
    private String name;
    @SerializedName("cover_url")
    private String cover_url;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCover_url() {
        return cover_url;
    }
}
