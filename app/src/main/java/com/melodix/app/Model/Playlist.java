package com.melodix.app.Model;

import java.io.Serializable;
import java.util.ArrayList;

public class Playlist implements Serializable {
    public String id;
    public String ownerUserId;
    public String name;
    public String coverRes;
    public boolean pinned;
    public ArrayList<String> songIds = new ArrayList<>();

    public Playlist() {
    }

    public Playlist(String id, String ownerUserId, String name, String coverRes, boolean pinned) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.coverRes = coverRes;
        this.pinned = pinned;
    }
}
