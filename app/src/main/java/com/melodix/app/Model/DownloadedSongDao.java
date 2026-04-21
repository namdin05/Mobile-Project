package com.melodix.app.Model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DownloadedSongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DownloadedSong song);

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    LiveData<List<DownloadedSong>> getAllDownloaded();

    @Query("SELECT * FROM downloaded_songs WHERE songId = :songId LIMIT 1")
    DownloadedSong getById(String songId);

    @Query("DELETE FROM downloaded_songs WHERE songId = :songId")
    void deleteById(String songId);

    @Query("DELETE FROM downloaded_songs WHERE localAudioPath IS NULL OR localAudioPath = ''")
    void deleteInvalidEntries();

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE songId = :songId)")
    boolean isDownloaded(String songId);

    @Query("SELECT * FROM downloaded_songs WHERE localAudioPath = :path LIMIT 1")
    DownloadedSong getByLocalPath(String path);

    @Query("SELECT * FROM downloaded_songs")
    List<DownloadedSong> getAllDownloadedSync();
}