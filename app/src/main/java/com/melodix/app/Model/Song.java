package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class Song {
    @SerializedName("id") private String id;
    @SerializedName("title") private String title;
    @SerializedName("audio_url") private String audioUrl;
    @SerializedName("cover_url") private String coverUrl;
    @SerializedName("duration_seconds") private Integer durationSeconds;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAudioUrl() { return audioUrl; }
    public String getCoverUrl() { return coverUrl; }
    public Integer getDurationSeconds() { return durationSeconds; }
}