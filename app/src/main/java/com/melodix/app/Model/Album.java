package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;

public class Album implements Serializable {
    @SerializedName("id")
    public String id;

    @SerializedName("title")
    public String title;

    @SerializedName("artist_id")
    public String artistId;

    @SerializedName("cover_url")
    public String coverRes;

    @SerializedName("release_year")
    public int year;

    // Giữ nguyên để không lỗi code cũ
    public String artistName;
    public String genre;
    public String description;
    public ArrayList<String> songIds = new ArrayList<>();

    public Album() {}

    public Album(String id, String title, String artistId, String artistName,
                 String coverRes, String genre, String description, int year) {
        this.id = id;
        this.title = title;
        this.artistId = artistId;
        this.artistName = artistName;
        this.coverRes = coverRes;
        this.genre = genre;
        this.description = description;
        this.year = year;
    }
}