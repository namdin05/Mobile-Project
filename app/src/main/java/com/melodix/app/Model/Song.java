package com.melodix.app.Model;

import java.io.Serializable;
import java.util.ArrayList;

public class Song implements Serializable {
    public String id;
    public String title;
    public String artistId;
    public String artistName;
    public String albumId;
    public String albumName;
    public String coverRes;
    public String audioRes;
    public String genre;
    public String description;
    public int durationSec;
    public int plays;
    public int likes;
    public boolean approved = true;


    public Song() {
    }

    public Song(String id, String title, String artistId, String artistName, String albumId,
                String albumName, String coverRes, String audioRes, String genre,
                String description, int durationSec, int plays, int likes) {
        this.id = id;
        this.title = title;
        this.artistId = artistId;
        this.artistName = artistName;
        this.albumId = albumId;
        this.albumName = albumName;
        this.coverRes = coverRes;
        this.audioRes = audioRes;
        this.genre = genre;
        this.description = description;
        this.durationSec = durationSec;
        this.plays = plays;
        this.likes = likes;
    }
}
