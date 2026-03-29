package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;

public class Artist implements Serializable {
    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("avatar_url")
    public String avatarRes;

    @SerializedName("biography")
    public String bio;

    // Giữ nguyên để không lỗi code cũ
    public String userId;
    public String heroCoverRes;
    public ArrayList<String> albumIds = new ArrayList<>();
    public ArrayList<String> topSongIds = new ArrayList<>();
    public ArrayList<String> similarArtistIds = new ArrayList<>();
    public boolean darkMode;
    public boolean offlineMode;

    public Artist() {}

    public Artist(String id, String userId, String name, String avatarRes, String bio, String heroCoverRes) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.avatarRes = avatarRes;
        this.bio = bio;
        this.heroCoverRes = heroCoverRes;
    }
}