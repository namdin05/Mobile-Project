package com.melodix.core.model;

import com.google.gson.annotations.SerializedName;

public class PlaylistSong {
    @SerializedName("id")
    public String id;

    @SerializedName("playlist_id")
    public String playlistId;

    @SerializedName("song_id")
    public String songId;

    @SerializedName("songs")
    public Song song;
}