package com.melodix.app.Model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloaded_songs")
public class DownloadedSong {

    @PrimaryKey
    @NonNull
    public String songId;

    public String title;
    public String artistName;
    public String coverUrl;
    public String localCoverPath;
    public String localAudioPath;
    public int durationSeconds;
    public long downloadedAt;
    public long fileSizeBytes;

    public DownloadedSong() {
    }

    public DownloadedSong(@NonNull String songId, String title, String artistName,
                          String coverUrl, String localAudioPath, int durationSeconds) {
        this.songId = songId;
        this.title = title;
        this.artistName = artistName;
        this.coverUrl = coverUrl;
        this.localAudioPath = localAudioPath;
        this.durationSeconds = durationSeconds;
        this.downloadedAt = System.currentTimeMillis();
    }
}