package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class PlaylistSong {
    @SerializedName("playlist_id")
    private String playlistId;

    @SerializedName("song_id")
    private String songId;

    @SerializedName("order_index")
    private int orderIndex;

    @SerializedName("added_at")
    private String addedAt;

    // Join với bảng songs
    @SerializedName("songs")
    private Song song;

    public int getOrderIndex() { return orderIndex; }
    public Song getSong() { return song; }
    public String getSongId() { return songId; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}