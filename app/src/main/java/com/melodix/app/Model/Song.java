package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class Song {

    @SerializedName("id")
    private String id;
    @SerializedName("title")
    private String title;
    @SerializedName("cover_url")
    private String cover_url;
    @SerializedName("artistName")
    private String artistName;

    public String getTitle() {
        return title;
    }

    public String getCover_url() {
        return cover_url;
    }

    public String getArtistName() {
        return artistName;
    }
}
