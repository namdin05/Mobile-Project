package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class AddToPlaylistRequest {
    @SerializedName("playlist_id")
    private String playlistId;

    @SerializedName("song_id")
    private String songId;

    @SerializedName("order_index")
    private int orderIndex;

    public AddToPlaylistRequest(String playlistId, String songId, int orderIndex) {
        this.playlistId = playlistId;
        this.songId = songId;
        this.orderIndex = orderIndex;
    }
}