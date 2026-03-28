package com.melodix.app.Model;

import java.io.Serializable;
import java.util.ArrayList;

public class Album implements Serializable {
    public String id;
    public String title;
    public String artistId;
    public String artistName;
    public String coverRes;
    public String genre;
    public String description;
    public int year;
    public ArrayList<String> songIds = new ArrayList<>();

    public Album() {
    }

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
