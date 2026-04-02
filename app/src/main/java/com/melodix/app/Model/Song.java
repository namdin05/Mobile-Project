package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Song {
    @SerializedName("id") private String id;
    @SerializedName("title") private String title;
    @SerializedName("audio_url") private String audioUrl;
    @SerializedName("cover_url") private String coverUrl;
    @SerializedName("profiles")
    private List<ArtistProfile> artistProfiles;
    @SerializedName("status")
    private String status;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAudioUrl() { return audioUrl; }
    public String getCoverUrl() { return coverUrl; }
    public String getStatus() { return status; }

    public String getArtistName() {
        if (artistProfiles != null && !artistProfiles.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < artistProfiles.size(); i++) {
                names.append(artistProfiles.get(i).getDisplayName());
                // Thêm dấu phẩy nếu chưa phải người cuối cùng
                if (i < artistProfiles.size() - 1) {
                    names.append(", ");
                }
            }
            return names.toString();
        }
        return "Unknown Artist";
    }

    // Mock dữ liệu cho UI
    public String getGenre() { return "Pop"; }
    public String getPlayCount() { return "1.2K"; }

    // ==========================================
    // CLASS CON ĐỂ HỨNG CỘT display_name TỪ PROFILES
    // ==========================================
    public static class ArtistProfile {
        @SerializedName("display_name")
        private String displayName;

        public String getDisplayName() { return displayName; }
    }
}