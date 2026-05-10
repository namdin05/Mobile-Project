package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SongRequestUpload {
    @SerializedName("p_title") private String songTitle;
    @SerializedName("p_cover_url") private String coverUrl;
    @SerializedName("p_audio_url") private String audioUrl;

    @SerializedName("p_duration_seconds") private int durationSeconds;
    @SerializedName("p_album_id") private String albumId; // Nếu không có, truyền null
    @SerializedName("p_lyrics_url") private String lyricsUrl; // Tạm truyền null

    @SerializedName("p_artist_ids") private List<String> artistIds;
    @SerializedName("p_genre_ids") private List<Integer> genreIds; // ID thể loại (VD: 1, 2)

    public SongRequestUpload(String songTitle, String coverUrl, String audioUrl, int durationSeconds, String albumId, String lyricsUrl, List<String> artistIds, List<Integer> genreIds) {
        this.songTitle = songTitle;
        this.coverUrl = coverUrl;
        this.audioUrl = audioUrl;
        this.durationSeconds = durationSeconds;
        this.albumId = albumId;
        this.lyricsUrl = lyricsUrl;
        this.artistIds = artistIds;
        this.genreIds = genreIds;
    }
}