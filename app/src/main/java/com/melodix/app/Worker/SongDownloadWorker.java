package com.melodix.app.Worker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.melodix.app.Model.AppDatabase;
import com.melodix.app.Model.DownloadedSong;
import com.melodix.app.PlayerActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SongDownloadWorker extends Worker {

    private static final String TAG = "SongDownloadWorker";
    private static final String CHANNEL_ID = "melodix_download_channel";
    private static final int PROGRESS_NOTIFICATION_ID = 1000;
    private static final int FINISH_NOTIFICATION_ID = 1001;

    public static final String KEY_SONG_ID = "song_id";
    public static final String KEY_AUDIO_URL = "audio_url";
    public static final String KEY_TITLE = "title";
    public static final String KEY_ARTIST = "artist";
    public static final String KEY_COVER_URL = "cover_url";
    public static final String KEY_DURATION = "duration";

    public SongDownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String songId = getInputData().getString(KEY_SONG_ID);
        String audioUrl = getInputData().getString(KEY_AUDIO_URL);
        String title = getInputData().getString(KEY_TITLE);
        String artist = getInputData().getString(KEY_ARTIST);
        String coverUrl = getInputData().getString(KEY_COVER_URL);
        int duration = getInputData().getInt(KEY_DURATION, 0);

        if (audioUrl == null || songId == null) {
            Log.e(TAG, "Dữ liệu không hợp lệ");
            return Result.failure();
        }

        createNotificationChannel();

        // Hiển thị notification bắt đầu tải
        showProgressNotification(0, title);

        try {
            String localPath = downloadWithProgress(audioUrl, title, artist);

            if (localPath == null) {
                throw new Exception("Lưu file thất bại");
            }

            saveToDatabase(songId, title, artist, coverUrl, localPath, duration);

            // Xóa notification progress cũ
            cancelProgressNotification();

            // Hiển thị notification thành công với intent để mở bài hát
            showSuccessNotification(title, songId);

            Log.d(TAG, "Download thành công: " + title);

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Download thất bại: " + title, e);
            cancelProgressNotification();
            showFailedNotification(title);
            return Result.failure();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Tải nhạc Melodix",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Hiển thị tiến trình tải bài hát");
            channel.setSound(null, null);
            channel.setShowBadge(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});

            NotificationManager nm = (NotificationManager) getApplicationContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }

    private void showProgressNotification(int progress, String songTitle) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(getApplicationContext());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Đang tải: " + songTitle)
                .setProgress(100, progress, false)
                .setContentText(progress + "%")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        // Category chỉ hỗ trợ từ API 21+, an toàn cho Android 10
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(NotificationCompat.CATEGORY_PROGRESS);
        }

        nm.notify(PROGRESS_NOTIFICATION_ID, builder.build());
    }

    private void updateProgressNotification(int progress, String songTitle) {
        showProgressNotification(progress, songTitle);
    }

    private void cancelProgressNotification() {
        NotificationManagerCompat.from(getApplicationContext()).cancel(PROGRESS_NOTIFICATION_ID);
        Log.d(TAG, "Đã xóa notification progress");
    }

    private void showSuccessNotification(String title, String songId) {  // Thêm tham số songId
        NotificationManagerCompat nm = NotificationManagerCompat.from(getApplicationContext());

        // Tạo Intent để mở PlayerActivity với bài hát vừa tải
        Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_SONG_ID, songId);
        intent.putExtra(PlayerActivity.EXTRA_AUTO_PLAY, true);  // THÊM DÒNG NÀY
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Tạo PendingIntent
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    songId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    songId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Tải thành công")
                .setContentText("Nhấn để nghe: " + title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)  // THÊM DÒNG NÀY
                .setVibrate(new long[]{0, 500, 200, 500});

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText("Đã tải xong bài hát: " + title + "\nNhấn vào đây để nghe ngay"));
        }

        nm.notify(FINISH_NOTIFICATION_ID, builder.build());

        Log.d(TAG, "Đã hiển thị thông báo thành công cho: " + title);
    }

    private void showFailedNotification(String title) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(getApplicationContext());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_delete)
                .setContentTitle("Tải thất bại")
                .setContentText(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true);

        nm.notify(FINISH_NOTIFICATION_ID, builder.build());

        Log.d(TAG, "Đã hiển thị thông báo thất bại cho: " + title);
    }

    private String downloadWithProgress(String audioUrl, String title, String artist) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        Request request = new Request.Builder()
                .url(audioUrl)
                .addHeader("User-Agent", "MelodixApp/1.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new Exception("Tải file thất bại từ server, mã lỗi: " + response.code());
            }

            long contentLength = response.body().contentLength();
            InputStream is = response.body().byteStream();

            // Sử dụng phương thức lưu phù hợp với từng phiên bản Android
            String localPath = saveFileForAndroidVersion(title, artist, contentLength, is);
            is.close();
            return localPath;
        }
    }

    /**
     * Lưu file tương thích với Android 10 (API 29) và các phiên bản khác
     */
    private String saveFileForAndroidVersion(String title, String artist, long contentLength, InputStream is) throws Exception {
        // Android 10 trở lên (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return saveUsingMediaStore(title, artist, contentLength, is);
        } else {
            // Android 9 trở xuống, dùng file system truyền thống
            return saveUsingFileSystem(title, artist, is);
        }
    }

    /**
     * Lưu file bằng MediaStore (cho Android 10+)
     */
    private String saveUsingMediaStore(String title, String artist, long contentLength, InputStream is) throws Exception {
        ContentResolver resolver = getApplicationContext().getContentResolver();

        // Làm sạch tên file, loại bỏ ký tự đặc biệt nhưng giữ tiếng Việt
        String fileName = sanitizeFileName(title) + ".mp3";
        if (title == null || title.isEmpty()) {
            fileName = "unknown_song.mp3";
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Melodix");
        values.put(MediaStore.Audio.AudioColumns.TITLE, title != null ? title : "Unknown Title");
        values.put(MediaStore.Audio.AudioColumns.ARTIST, artist != null ? artist : "Unknown Artist");
        values.put(MediaStore.Audio.AudioColumns.IS_MUSIC, 1);

        Uri audioCollection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri uri = resolver.insert(audioCollection, values);

        if (uri == null) {
            throw new Exception("Không thể tạo URI trong MediaStore");
        }

        try (OutputStream os = resolver.openOutputStream(uri)) {
            if (os == null) throw new Exception("Không mở được OutputStream");

            byte[] buffer = new byte[8192];
            long totalBytesRead = 0;
            int bytesRead;
            int lastProgress = -1;

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                if (contentLength > 0) {
                    int progress = (int) ((totalBytesRead * 100) / contentLength);
                    if (progress > lastProgress + 3 || progress == 100) {
                        lastProgress = progress;
                        updateProgressNotification(progress, title);
                    }
                }
            }
        } catch (Exception e) {
            resolver.delete(uri, null, null);
            throw e;
        }

        return uri.toString();
    }

    /**
     * Lưu file bằng File System truyền thống (cho Android 9 trở xuống)
     */
    private String saveUsingFileSystem(String title, String artist, InputStream is) throws Exception {
        // Tạo thư mục Melodix trong Music
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File melodixDir = new File(musicDir, "Melodix");

        if (!melodixDir.exists()) {
            if (!melodixDir.mkdirs()) {
                throw new Exception("Không thể tạo thư mục: " + melodixDir.getAbsolutePath());
            }
        }

        String fileName = sanitizeFileName(title) + ".mp3";
        File outputFile = new File(melodixDir, fileName);

        // Nếu file đã tồn tại, thêm số đằng sau
        int counter = 1;
        while (outputFile.exists()) {
            fileName = sanitizeFileName(title) + "_" + counter + ".mp3";
            outputFile = new File(melodixDir, fileName);
            counter++;
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;
            int lastProgress = -1;

            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // Cập nhật progress cho Android 9 trở xuống (không có contentLength)
                int progress = (int) ((totalBytesRead * 100) / (totalBytesRead + 1024));
                if (progress > lastProgress + 5) {
                    lastProgress = progress;
                    updateProgressNotification(progress, title);
                }
            }
        }

        // Quét file vào MediaStore để hiển thị trong các app nghe nhạc khác
        scanFileToMediaStore(outputFile, title, artist);

        return outputFile.getAbsolutePath();
    }

    /**
     * Quét file vào MediaStore (cho Android 9 trở xuống)
     */
    private void scanFileToMediaStore(File file, String title, String artist) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
        values.put(MediaStore.MediaColumns.TITLE, title != null ? title : "Unknown Title");
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg");
        values.put(MediaStore.MediaColumns.SIZE, file.length());
        values.put(MediaStore.Audio.AudioColumns.ARTIST, artist != null ? artist : "Unknown Artist");
        values.put(MediaStore.Audio.AudioColumns.IS_MUSIC, 1);

        getApplicationContext().getContentResolver().insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                values
        );
    }

    /**
     * Làm sạch tên file, loại bỏ ký tự đặc biệt nhưng giữ tiếng Việt
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "unknown_song";
        // Chỉ thay thế các ký tự không hợp lệ trong tên file
        // Giữ nguyên tiếng Việt có dấu
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void saveToDatabase(String songId, String title, String artist,
                                String coverUrl, String localPath, int duration) {
        DownloadedSong downloadedSong = new DownloadedSong();
        downloadedSong.songId = songId;
        downloadedSong.title = title;
        downloadedSong.artistName = artist;
        downloadedSong.coverUrl = coverUrl;
        downloadedSong.localAudioPath = localPath;
        downloadedSong.durationSeconds = duration;
        downloadedSong.downloadedAt = System.currentTimeMillis();

        AppDatabase.getInstance(getApplicationContext())
                .downloadedSongDao()
                .insert(downloadedSong);

        Log.d(TAG, "Đã lưu vào database: " + title + ", path: " + localPath);
    }
}