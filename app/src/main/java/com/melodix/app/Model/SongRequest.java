package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class SongRequest {
    @SerializedName("id")
    private String id;

    @SerializedName("song_title")
    private String songTitle;

    @SerializedName("audio_url")
    private String audioUrl; // Thêm link nhạc để phát

    @SerializedName("cover_url")
    private String coverUrl;

    @SerializedName("status")
    private String status;

    // Supabase sẽ nhét thông tin của bảng profiles vào biến này
    @SerializedName("profiles")
    private Profile artistProfile;

    public String getId() { return id; }
    public String getSongTitle() { return songTitle; }
    public String getAudioUrl() { return audioUrl; }
    public String getCoverUrl() { return coverUrl; }
    public String getStatus() { return status; }

    // Hàm phụ trợ: Tự động móc tên ca sĩ từ Profile ra
    public String getArtistName() {
        if (artistProfile != null && artistProfile.getDisplayName() != null) {
            return artistProfile.getDisplayName();
        }
        return "Unknown Artist";
    }
}