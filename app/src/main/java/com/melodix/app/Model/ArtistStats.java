package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class ArtistStats {
    @SerializedName("artist_id")
    public String artistId;

    @SerializedName("total_songs")
    public int totalSongs;

    @SerializedName("total_streams")
    public int totalStreams;

    @SerializedName("total_likes")
    public int totalLikes;

    @SerializedName("total_listeners")
    public int totalListeners;
}