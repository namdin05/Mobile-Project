package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class PlaylistSong {
    @SerializedName("playlist_id")
    public String playlistId;

    @SerializedName("song_id")
    public String songId;

    @SerializedName("order_index")
    public int orderIndex;

    @SerializedName("songs")
    public Song song;

    @SerializedName("artistname")
    public String artistname;
}