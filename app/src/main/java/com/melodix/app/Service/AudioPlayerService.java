package com.melodix.app.Service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.melodix.app.Model.Song;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.R;
import com.melodix.app.Repository.PlaybackRepository;
import com.melodix.app.Utils.ResourceUtils;
import com.melodix.app.PlayerActivity;

public class AudioPlayerService extends Service {
    private static boolean hasRecordedPlay = false; // Bấm nút chặn spam view
    private PlaybackRepository playbackRepo = PlaybackRepository.getInstance();
    public static final int NOTIFICATION_ID = 991;
    private static final String CHANNEL_ID = "melodix_playback_channel";
    public static final String ACTION_PLAY_SONG = "com.melodix.app.action.PLAY_SONG";
    public static final String ACTION_TOGGLE_PLAY = "com.melodix.app.action.TOGGLE_PLAY";
    public static final String ACTION_NEXT = "com.melodix.app.action.NEXT";
    public static final String ACTION_PREVIOUS = "com.melodix.app.action.PREVIOUS";
    public static final String ACTION_REWIND = "com.melodix.app.action.REWIND";
    public static final String ACTION_FORWARD = "com.melodix.app.action.FORWARD";
    public static final String ACTION_SEEK_TO = "com.melodix.app.action.SEEK_TO";
    public static final String ACTION_SET_SPEED = "com.melodix.app.action.SET_SPEED";
    public static final String ACTION_STOP = "com.melodix.app.action.STOP";
    public static final String ACTION_SET_SLEEP_TIMER = "com.melodix.app.action.SET_SLEEP_TIMER";
    public static final String ACTION_STATE_CHANGED = "com.melodix.app.action.STATE_CHANGED";

    public static final String ACTION_PAUSE = "com.melodix.app.ACTION_PAUSE";

    public static final String EXTRA_SONG_ID = "extra_song_id";
    public static final String EXTRA_SEEK = "extra_seek";
    public static final String EXTRA_SPEED = "extra_speed";
    public static final String EXTRA_TIMER_MINUTES = "extra_timer_minutes";

    private static AudioPlayerService instance;
    private static MediaPlayer mediaPlayer;
    private AppRepository repository;
    private final Handler handler = new Handler();
    private Runnable tickRunnable;
    private Runnable sleepRunnable;

    private static String currentSongId;
    private static boolean isPlaying;
    private static float playbackSpeed = 1f;
    private static boolean isLoopingOne = false;

    private MediaSessionCompat mediaSession;

    public static String getCurrentSongId() { return currentSongId; }
    public static boolean isPlaying() { return isPlaying; }
    public static float getPlaybackSpeed() { return playbackSpeed; }
    public static boolean isLoopMode() { return isLoopingOne; }

    public static int getCurrentPosition() {
        if (instance != null && instance.mediaPlayer != null) {
            try { return instance.mediaPlayer.getCurrentPosition(); }
            catch (Exception ignored) {}
        }
        return 0;
    }

    public static int getDuration() {
        if (instance != null && instance.mediaPlayer != null) {
            try { return instance.mediaPlayer.getDuration(); }
            catch (Exception ignored) {}
        }
        return 0;
    }
    public static void toggleLoop() {
        isLoopingOne = !isLoopingOne;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        repository = AppRepository.getInstance(this);

        createNotificationChannel();
        mediaSession = new MediaSessionCompat(this, "AudioPlayerService");
        mediaSession.setActive(true);
        mediaSession.setCallback(new android.support.v4.media.session.MediaSessionCompat.Callback() {
            @Override public void onPlay() { togglePlayPause(); }
            @Override public void onPause() { togglePlayPause(); }
            @Override public void onSkipToNext() { playNext(); }
            @Override public void onSkipToPrevious() { playPrevious(); }
            @Override public void onSeekTo(long pos) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo((int) pos);
                    updateNotification(playbackRepo.getCurrentSong(), isPlaying);
                }
            }
        });

        tickRunnable = new Runnable() {
            @Override
            public void run() {
                broadcastState();

                // ĐỒNG HỒ ĐẾM GIÂY VÀ CỘNG VIEW (Đã tích hợp test log)
                if (mediaPlayer != null && isPlaying) {
                    int listenedSec = mediaPlayer.getCurrentPosition() / 1000;

                    Log.d("TEST_TIME", "⏳ Đang nghe: " + listenedSec + "s - Đã cộng view chưa: " + hasRecordedPlay);

                    if (listenedSec >= 15 && !hasRecordedPlay) {
                        Log.d("TEST_TIME", "🚀 Đã đủ 15s! Bắt đầu gọi API cộng view...");
                        repository.recordPlay(currentSongId); // Gọi hàm 1 tham số
                        hasRecordedPlay = true;
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(tickRunnable);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Media Playback", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Music playback controls");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) return START_STICKY;

        switch (intent.getAction()) {
            case ACTION_PLAY_SONG:
                playSong(intent.getStringExtra(EXTRA_SONG_ID));
                break;
            case ACTION_TOGGLE_PLAY:
                togglePlayPause();
                break;
            case ACTION_NEXT:
                playNext();
                break;
            case ACTION_PREVIOUS:
                playPrevious();
                break;
            case ACTION_REWIND:
                if (mediaPlayer != null) mediaPlayer.seekTo(Math.max(0, mediaPlayer.getCurrentPosition() - 15000));
                break;
            case ACTION_FORWARD:
                if (mediaPlayer != null) mediaPlayer.seekTo(Math.min(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition() + 30000));
                break;
            case ACTION_SEEK_TO:
                if (mediaPlayer != null) mediaPlayer.seekTo(intent.getIntExtra(EXTRA_SEEK, 0));
                break;
            case ACTION_SET_SPEED:
                setSpeed(intent.getFloatExtra(EXTRA_SPEED, 1f));
                break;
            case ACTION_SET_SLEEP_TIMER:
                setSleepTimer(intent.getIntExtra(EXTRA_TIMER_MINUTES, 0));
                break;
            case ACTION_STOP:
                stopPlayback();
                stopForeground(true);
                stopSelf();
                break;
        }
        broadcastState();
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopPlayback();
        stopForeground(true);
        stopSelf();
    }

    private void playSong(String songId) {
        Song song = playbackRepo.getCurrentSong();
        if (song == null) {
            Log.e("TEST_MUSIC", "LỖI: Không tìm thấy bài hát trong Queue! Bị thoát sớm.");
            return;
        }

        releasePlayer();
        currentSongId = song.getId();
        hasRecordedPlay = false; // Reset cờ cho bài hát mới

        try {
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setAudioAttributes(new android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .build());

            String audioSource = song.getAudioUrl();

            Log.d("TEST_MUSIC", "Đang phát từ: " + audioSource);

            // === PHÂN BIỆT LOCAL FILE VÀ URL MẠNG ===
            if (audioSource != null &&
                    (audioSource.startsWith("/") ||
                            audioSource.startsWith("content://") ||
                            audioSource.startsWith("file://"))) {

                // Đây là file local (offline)
                mediaPlayer.setDataSource(audioSource);
                Log.d("TEST_MUSIC", "→ Phát từ file local");
            } else {
                // Đây là URL từ Supabase (online)
                mediaPlayer.setDataSource(audioSource);
                Log.d("TEST_MUSIC", "→ Phát từ URL mạng");
            }

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("TEST_MUSIC", "Lỗi phát nhạc! Mã lỗi: " + what + " - " + extra);
                isPlaying = false;
                broadcastState();
                return true;
            });

            mediaPlayer.setOnPreparedListener(mp -> startPlayback());
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnCompletionListener(mp -> {
                // ĐÃ XÓA KHÚC GỌI API BỊ LỖI Ở ĐÂY
                if (isLoopingOne) {
                    try {
                        mp.seekTo(0);
                        mp.start();
                        hasRecordedPlay = false; // Nếu bài hát tự lặp lại thì cho tính thêm view nữa
                        broadcastState();
                    } catch (Exception e) {}
                } else {
                    playNext();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(this, "Lỗi phát nhạc từ link", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void startPlayback() {
        if (mediaPlayer == null) return;
        applyPlaybackSpeed();
        mediaPlayer.start();
        isPlaying = true;
        updateNotification(playbackRepo.getCurrentSong(), true);
        broadcastState();
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) {
            Song current = playbackRepo.getCurrentSong();
            if (current != null) playSong(current.getId());
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            updateNotification(playbackRepo.getCurrentSong(), false);
        } else {
            mediaPlayer.start();
            applyPlaybackSpeed();
            isPlaying = true;
            updateNotification(playbackRepo.getCurrentSong(), true);
        }
    }

    // Trong AudioPlayerService.java
    private void playNext() {
        Song next = playbackRepo.moveNext();
        if (next != null) {
            // Nếu bài tiếp theo khác bài hiện tại -> Phát bài mới
            if (!next.getId().equals(currentSongId)) {
                playSong(next.getId());
            } else {
                // Nếu chỉ có 1 bài (ID trùng nhau) thì lặp lại bài đó
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
                broadcastState();
            }
        }
    }

    private void playPrevious() {
        Song previous = playbackRepo.movePrevious();
        if (previous != null) {
            playSong(previous.getId());
        }
    }

    private void setSpeed(float speed) {
        playbackSpeed = speed;
        applyPlaybackSpeed();
    }

    private void applyPlaybackSpeed() {
        if (mediaPlayer == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PlaybackParams params = mediaPlayer.getPlaybackParams();
                params.setSpeed(playbackSpeed);
                mediaPlayer.setPlaybackParams(params);
            } catch (Exception ignored) {}
        }
    }

    private void stopPlayback() {
        releasePlayer();
        isPlaying = false;
        broadcastState();
    }

    private void setSleepTimer(int minutes) {
        if (sleepRunnable != null) handler.removeCallbacks(sleepRunnable);
        if (minutes <= 0) return;
        sleepRunnable = () -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPlaying = false;
                updateNotification(playbackRepo.getCurrentSong(), false);
                broadcastState();
            }
            stopForeground(false);
        };
        handler.postDelayed(sleepRunnable, minutes * 1000L);
    }

    private void updateNotification(Song currentSong, boolean isPlaying) {
        if (currentSong == null) return;

        PendingIntent playPauseIntent = createActionIntent(ACTION_TOGGLE_PLAY, 101);
        PendingIntent prevIntent = createActionIntent(ACTION_PREVIOUS, 100);
        PendingIntent nextIntent = createActionIntent(ACTION_NEXT, 102);

        long duration = mediaPlayer != null ? mediaPlayer.getDuration() : 0;
        long position = mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;

        Intent openIntent = new Intent(this, PlayerActivity.class);
        openIntent.putExtra(PlayerActivity.EXTRA_SONG_ID, currentSong.getId());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 11, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 1. TẠO THÔNG BÁO CƠ BẢN VÀ HIỆN LÊN TRƯỚC (Để tránh lỗi Android ANR)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo) // Bạn có thể thay bằng R.drawable.ic_music_note nếu muốn
                .setContentTitle(currentSong.getTitle())
                .setContentText(currentSong.getArtistName())
                .setContentIntent(contentIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setOngoing(isPlaying)
                .addAction(R.drawable.ic_prev, "Previous", prevIntent)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, isPlaying ? "Pause" : "Play", playPauseIntent)
                .addAction(R.drawable.ic_next, "Next", nextIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.getSessionToken()));

        // Gắn luôn logo mặc định trong lúc chờ tải ảnh thật
        Bitmap placeholder = BitmapFactory.decodeResource(getResources(), R.drawable.ic_logo);
        if (placeholder != null) {
            builder.setLargeIcon(placeholder);
        }

        // Hiện thông báo lên liền
        startForeground(NOTIFICATION_ID, builder.build());

        // Cập nhật trạng thái Play/Pause cho MediaSession
        android.support.v4.media.session.PlaybackStateCompat.Builder stateBuilder = new android.support.v4.media.session.PlaybackStateCompat.Builder()
                .setActions(android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY |
                        android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE |
                        android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO)
                .setState(isPlaying ? android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING : android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED,
                        position, playbackSpeed);
        mediaSession.setPlaybackState(stateBuilder.build());

        // 2. TẢI ẢNH QUA MẠNG BẰNG GLIDE VÀ CẬP NHẬT LẠI KHI TẢI XONG
        if (currentSong.getCoverUrl() != null && !currentSong.getCoverUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .asBitmap()
                    .load(currentSong.getCoverUrl())
                    .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@androidx.annotation.NonNull Bitmap resource, @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                            // Ảnh đã tải xong! Bơm ảnh vào Thông báo
                            builder.setLargeIcon(resource);

                            // Bơm ảnh vào màn hình khóa (Lockscreen)
                            android.support.v4.media.MediaMetadataCompat.Builder metadataBuilder = new android.support.v4.media.MediaMetadataCompat.Builder()
                                    .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.getTitle())
                                    .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.getArtistName())
                                    .putLong(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                                    .putBitmap(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART, resource); // Gắn ảnh bìa
                            mediaSession.setMetadata(metadataBuilder.build());

                            // Yêu cầu Android vẽ lại thông báo với ảnh mới
                            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            if (manager != null) {
                                manager.notify(NOTIFICATION_ID, builder.build());
                            }
                        }

                        @Override
                        public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {
                        }
                    });
        } else {
            // Nếu bài hát không có link ảnh, chỉ update Text
            android.support.v4.media.MediaMetadataCompat.Builder metadataBuilder = new android.support.v4.media.MediaMetadataCompat.Builder()
                    .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.getTitle())
                    .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.getArtistName())
                    .putLong(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION, duration);
            mediaSession.setMetadata(metadataBuilder.build());
        }
    }

    private PendingIntent createActionIntent(String action, int requestCode) {
        Intent intent = new Intent(this, AudioPlayerService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void broadcastState() {
        Intent stateIntent = new Intent(ACTION_STATE_CHANGED);
        stateIntent.setPackage(getPackageName());
        stateIntent.putExtra(EXTRA_SONG_ID, currentSongId);
        stateIntent.putExtra("playing", isPlaying);
        stateIntent.putExtra("position", mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0);
        stateIntent.putExtra("duration", mediaPlayer != null ? mediaPlayer.getDuration() : 0);
        stateIntent.putExtra(EXTRA_SPEED, playbackSpeed);
        sendBroadcast(stateIntent);
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            try { mediaPlayer.reset(); mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        isPlaying = false;
    }

    @Override
    public void onDestroy() {
        instance = null;
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        if (mediaSession != null) {
            mediaSession.release();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}