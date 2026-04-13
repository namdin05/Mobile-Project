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

    // Supabase trả về cover_url -> nhét vào biến coverRes
    @SerializedName("cover_url")
    public String coverRes;

    @SerializedName("release_year")
    public int year;

    // THÊM DÒNG NÀY VÀO ĐỂ LẤY ĐƯỢC TÊN CA SĨ
    @SerializedName("artistName")
    public String artistName;

    // Các biến dưới đây Database không trả về, cứ để kệ nó
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

    public String getId () {
        return id;
    }

    public String getTitle () {
        return title;
    }

    public String getArtistId () {
        return artistId;
    }

    public String getCoverUrl () {
        return coverRes;
    }

    public int getReleaseYear () {
        return year;
    }
}