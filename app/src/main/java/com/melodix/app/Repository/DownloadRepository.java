package com.melodix.app.Repository;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.BackoffPolicy;

import com.melodix.app.Model.Song;
import com.melodix.app.Worker.SongDownloadWorker;
import com.melodix.app.Model.AppDatabase;

import java.util.concurrent.TimeUnit;

public class DownloadRepository {

    private final Context context;

    public DownloadRepository(Context context) {
        this.context = context;
    }

    public void enqueueDownload(Song song) {
        if (song == null || song.getId() == null || song.getAudioUrl() == null) {
            Log.e("DownloadRepo", "Song data không hợp lệ");
            return;
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Cần cấp quyền lưu trữ để tải nhạc", Toast.LENGTH_LONG).show()
                );
                return;
            }
        }

        new Thread(() -> {
            try {
                boolean alreadyDownloaded = AppDatabase.getInstance(context)
                        .downloadedSongDao()
                        .isDownloaded(song.getId());

                if (alreadyDownloaded) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "Bài hát đã được tải xuống", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String downloadUrl = song.getAudioUrl();

                Data inputData = new Data.Builder()
                        .putString(SongDownloadWorker.KEY_SONG_ID, song.getId())
                        .putString(SongDownloadWorker.KEY_AUDIO_URL, downloadUrl)
                        .putString(SongDownloadWorker.KEY_TITLE, song.getTitle() != null ? song.getTitle() : "Unknown")
                        .putString(SongDownloadWorker.KEY_ARTIST, song.getArtistName() != null ? song.getArtistName() : "")
                        .putString(SongDownloadWorker.KEY_COVER_URL, song.getCoverUrl())
                        .putInt(SongDownloadWorker.KEY_DURATION, song.getDurationSeconds())
                        .build();

                // QUAN TRỌNG: Thêm constraints và backoff policy
                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SongDownloadWorker.class)
                        .setInputData(inputData)
                        .setConstraints(new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build())
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                        .build();

                WorkManager.getInstance(context).enqueue(workRequest);

                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Đang tải: " + song.getTitle(), Toast.LENGTH_LONG).show()
                );

            } catch (Exception e) {
                Log.e("DownloadRepo", "Lỗi khi enqueue download", e);
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Lỗi tải xuống: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}