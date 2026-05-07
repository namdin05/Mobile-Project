package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class Profile {

    // Ánh xạ chính xác tên cột trong database Supabase
    @SerializedName("id")
    private String id;

    @SerializedName("display_name")
    private String displayName;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("role")
    private String role;

    @SerializedName("show_playlists")
    private boolean showPlaylists = true;

    @SerializedName("show_recent_artists")
    private boolean showRecentArtists = true;

    // Constructor rỗng (Bắt buộc phải có cho Firebase/Supabase/Gson)
    public Profile() {
    }

    // Constructor có tham số
    public Profile(String id, String displayName, String avatarUrl, String role) {
        this.id = id;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.role = role;
    }

    // --- CÁC HÀM GETTER & SETTER ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
    public boolean isShowPlaylists() { return showPlaylists; }
    public void setShowPlaylists(boolean showPlaylists) { this.showPlaylists = showPlaylists; }

    public boolean isShowRecentArtists() { return showRecentArtists; }
    public void setShowRecentArtists(boolean showRecentArtists) { this.showRecentArtists = showRecentArtists; }
}
