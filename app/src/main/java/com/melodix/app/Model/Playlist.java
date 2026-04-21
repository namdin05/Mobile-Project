package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;

public class Playlist implements Serializable {
    @SerializedName("id")
    public String id;

    @SerializedName("user_id")
    public String ownerUserId;

    @SerializedName("name")
    public String name;

    @SerializedName("cover_url")
    public String coverRes;
    @SerializedName("is_public")
    public boolean isPublic = true;
    public boolean pinned;
    public int songCount = 0;
    public ArrayList<String> songIds = new ArrayList<>();

    public Playlist() {}

    public Playlist(String id, String ownerUserId, String name, String coverRes, boolean pinned) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.coverRes = coverRes;
        this.pinned = pinned;
    }
}