package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class Song {

    @SerializedName("id")
    private String id;
    @SerializedName("title")
    private String title;

    public String getTitle() {
        return title;
    }
}
