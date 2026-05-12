package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class ListenHistoryItem {

    @SerializedName("id")
    private long id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("song_id")
    private String songId;

    @SerializedName("listened_at")
    private String listenedAt;

    // Song fields từ VIEW
    @SerializedName("title")
    private String title;

    @SerializedName("cover_url")
    private String coverUrl;

    @SerializedName("audio_url")
    private String audioUrl;

    @SerializedName("lyrics_lrc_url")
    private String lyricsUrl;

    @SerializedName("duration_seconds")
    private int durationSeconds;

    @SerializedName("stream_count")
    private int streamCount;

    @SerializedName("status")
    private String status;

    @SerializedName("artist_name")
    private String artistName;

    @SerializedName("like_count")
    private int likeCount;

    public ListenHistoryItem() {}

    // Chuyển đổi sang Song object
    public Song getSong() {
        Song song = new Song(
                songId,           // id
                title,            // title
                null,             // artistId
                artistName,       // artistName
                null,             // albumId
                null,             // albumName
                coverUrl,         // coverRes
                audioUrl,         // audioRes
                null,             // genre
                null,             // description
                durationSeconds,  // durationSec
                streamCount,      // plays
                likeCount         // likes
        );
        song.setStatus(status);

        try {
            java.lang.reflect.Field field = Song.class.getDeclaredField("lyrics_url");
            field.setAccessible(true);
            field.set(song, lyricsUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return song;
    }

    public String getListenedAt() {
        return listenedAt;
    }
}