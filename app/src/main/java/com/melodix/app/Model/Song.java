package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class Song {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("album_id")
    private String album_id;

    @SerializedName("cover_url")
    private String cover_url;

    @SerializedName("audio_url")
    private String audio_url;

    @SerializedName("duration_seconds")
    private int duration_seconds;

    @SerializedName("stream_count")
    private int plays;

    @SerializedName("status")
    private String status;

    // ĐÃ THÊM SERIALIZED NAME Ở ĐÂY ĐỂ NHẬN CHUỖI GỘP NHIỀU NGHỆ SĨ
    @SerializedName("artistName")
    private String artistName;
    private String artistId;
    private String albumName;
    private String genre;
    private String description;
    private int likes;

    public Song() {}

    public Song(String id, String title, String artistId, String artistName, String albumId,
                String albumName, String coverRes, String audioRes, String genre,
                String description, int durationSec, int plays, int likes) {
        this.id = id;
        this.title = title;
        this.artistId = artistId;
        this.artistName = artistName;
        this.album_id = albumId;
        this.albumName = albumName;
        this.cover_url = coverRes;
        this.audio_url = audioRes;
        this.genre = genre;
        this.description = description;
        this.duration_seconds = durationSec;
        this.plays = plays;
        this.likes = likes;
    }

    public String getStatus() { return status; }


    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getAudioUrl() {
        return audio_url;
    }

    public String getCoverUrl() {
        return cover_url;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public int getDurationSeconds() {
        return duration_seconds;
    }

    public String getGenre() {
        return genre;
    }

    public int getPlays() {
        return plays;
    }


    public void setStatus(String status) { this.status = status; }
}