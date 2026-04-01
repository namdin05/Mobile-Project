package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Song implements Serializable {
    @SerializedName("id")
    public String id;

    @SerializedName("title")
    public String title;

    @SerializedName("artist_id")
    public String artistId;

    @SerializedName("album_id")
    public String albumId;

    @SerializedName("cover_url")
    public String coverRes;

    @SerializedName("audio_url")
    public String audioRes;

    @SerializedName("duration_seconds")
    public int durationSec;

    @SerializedName("stream_count")
    public int plays;

    @SerializedName("is_published")
    public boolean approved = true;

    // Các trường dưới đây không có trực tiếp trong bảng songs (do dùng DB quan hệ)
    // Mình giữ nguyên để không làm lỗi app, nhưng dữ liệu trả về tạm thời sẽ là null/mặc định
    public String artistName;
    public String albumName;
    public String genre;
    public String description;
    public int likes;

    public Song() {}

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

    public String getTitle() {
        return title;
    }

    public String getCover_url() {
        return coverRes;
    }
    public String getArtistName(){
        return artistName;
    }
}