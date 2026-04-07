package com.melodix.app.Service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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

import java.io.File;

public class AudioPlayerService extends Service {
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
        if (instance != null && instance.mediaPlayer != null) { // Thêm điều kiện isPlaying
            try { return instance.mediaPlayer.getCurrentPosition(); }
            catch (Exception ignored) {}
        }
        return 0;
    }

    public static int getDuration() {
        if (instance != null && instance.mediaPlayer != null) { // Thêm điều kiện isPlaying
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
            @Override
            public void onPlay() {
                togglePlayPause(); // Gọi hàm play/pause của bạn
            }

            @Override
            public void onPause() {
                togglePlayPause(); // Gọi hàm play/pause của bạn
            }

            @Override
            public void onSkipToNext() {
                playNext(); // Gọi hàm chuyển bài tới
            }

            @Override
            public void onSkipToPrevious() {
                playPrevious(); // Gọi hàm lùi bài
            }

            @Override
            public void onSeekTo(long pos) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo((int) pos); // Xử lý khi người dùng tua trên thanh thời gian
                    updateNotification(repository.getSongById(currentSongId), isPlaying);
                }
            }
        });

        tickRunnable = new Runnable() {
            @Override
            public void run() {
                broadcastState();
                // Tắt refresh liên tục vì Notification xịn không cần tự redraw mỗi giây,
                // chỉ cần gọi khi đổi trạng thái Play/Pause hoặc chuyển bài.
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
            android.util.Log.e("TEST_MUSIC", "LỖI: Không tìm thấy bài hát trong Queue! Bị thoát sớm.");
            return;
        }

        persistLastListen();
        releasePlayer();
        currentSongId = song.getId();

        try {
            mediaPlayer = new MediaPlayer();

            // Khai báo cho Android biết đây là stream nhạc
            mediaPlayer.setAudioAttributes(new android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .build());

            // Truyền link URL từ API vào (ở đây bạn dùng thuộc tính audioRes để chứa URL)
            Log.d("TEST_MUSIC", "Đang tải link: " + song.getAudioUrl());
            mediaPlayer.setDataSource(song.getAudioUrl());

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("TEST_MUSIC", "Lỗi phát nhạc! Mã lỗi: " + what + " - " + extra);
                isPlaying = false;
                broadcastState(); // Cập nhật giao diện thành Pause
                return true; // QUAN TRỌNG: Trả về true để Android KHÔNG gọi OnCompletionListener nữa
            });

            // Quan trọng: Bắt sự kiện khi load mạng xong mới bắt đầu phát
            mediaPlayer.setOnPreparedListener(mp -> startPlayback());

            // Dùng prepareAsync thay vì prepare để không làm đơ giao diện khi tải mạng
            mediaPlayer.prepareAsync();

            // Code xử lý khi hát xong (Next/Loop) giữ nguyên
            mediaPlayer.setOnCompletionListener(mp -> {
                int listenedSec = 0;
                try {
                    if (mp != null && isPlaying) {
                        listenedSec = mp.getDuration() / 1000;
                    }
                } catch (Exception e) {
                    android.util.Log.e("TEST_MUSIC", "Lỗi lấy Duration khi hết bài");
                }

                repository.recordPlay(currentSongId, listenedSec);

                if (isLoopingOne) {
                    try {
                        mp.seekTo(0);
                        mp.start();
                        broadcastState();
                    } catch (Exception e) {}
                } else {
                    playNext(); // Đây là thủ phạm gây nhảy bài liên tục nếu không bị chặn đúng cách
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

        // --- GỌI UPDATE NOTIFICATION KHI BẮT ĐẦU PHÁT ---
        updateNotification(playbackRepo.getCurrentSong(), true);

        broadcastState();
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) {
            Song current = repository.getCurrentQueueSong();
            if (current != null) playSong(current.getId());
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            persistLastListen();
            // --- GỌI UPDATE KHI TẠM DỪNG ---
            updateNotification(playbackRepo.getCurrentSong(), false);
        } else {
            mediaPlayer.start();
            applyPlaybackSpeed();
            isPlaying = true;
            // --- GỌI UPDATE KHI PHÁT TIẾP ---
            updateNotification(playbackRepo.getCurrentSong(), true);
        }
    }

    private void playNext() {
        Song next = playbackRepo.moveNext();
        if (next == null || next.getId().equals(currentSongId)) {
            stopPlayback();
            stopForeground(true);
        } else {
            playSong(next.getId());
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

    // huy phat nhac
    private void stopPlayback() {
        persistLastListen();
        releasePlayer();
        currentSongId = null;
        isPlaying = false;
        broadcastState();
    }

    private void setSleepTimer(int minutes) {
        if (sleepRunnable != null) handler.removeCallbacks(sleepRunnable);
        if (minutes <= 0) return;
        sleepRunnable = () -> {
            stopPlayback();
            stopForeground(true);
            stopSelf();
        };
        handler.postDelayed(sleepRunnable, minutes * 60L * 1000L);
    }

    // luu lai luot nghe: dem so giay nghe bai hat nao, cong luot stream cho bai hat...
    private void persistLastListen() {
        if (mediaPlayer != null && currentSongId != null) {
            int listenedSec = mediaPlayer.getCurrentPosition() / 1000;
            repository.recordPlay(currentSongId, listenedSec);
        }
    }

    // --- HÀM TẠO NOTIFICATION KIỂU MỚI (MÀN HÌNH KHÓA XỊN SÒ) ---
    private void updateNotification(Song currentSong, boolean isPlaying) {
        if (currentSong == null) return;

        PendingIntent playPauseIntent = createActionIntent(ACTION_TOGGLE_PLAY, 101);
        PendingIntent prevIntent = createActionIntent(ACTION_PREVIOUS, 100);
        PendingIntent nextIntent = createActionIntent(ACTION_NEXT, 102);

        // Lấy thời lượng và vị trí hiện tại
        long duration = mediaPlayer != null ? mediaPlayer.getDuration() : 0;
        long position = mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;

        Intent openIntent = new Intent(this, PlayerActivity.class);
        openIntent.putExtra(PlayerActivity.EXTRA_SONG_ID, currentSong.getId());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 11, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 1. Lấy ảnh Cover
        Bitmap coverBitmap = null;
        int coverResId = ResourceUtils.anyDrawable(this, currentSong.getCoverUrl());
        if (coverResId != 0) {
            coverBitmap = BitmapFactory.decodeResource(getResources(), coverResId);
        }

        // 2. Bơm trạng thái Play/Pause và VỊ TRÍ + QUYỀN TUA
        android.support.v4.media.session.PlaybackStateCompat.Builder stateBuilder = new android.support.v4.media.session.PlaybackStateCompat.Builder()
                .setActions(android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY |
                        android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE |
                        android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO) // Thêm quyền kéo tua
                .setState(isPlaying ? android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING : android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED,
                        position, playbackSpeed); // Cập nhật vị trí và tốc độ thực tế
        mediaSession.setPlaybackState(stateBuilder.build());

        // 3. Bơm thông tin bài hát và THỜI LƯỢNG (DURATION)
        android.support.v4.media.MediaMetadataCompat.Builder metadataBuilder = new android.support.v4.media.MediaMetadataCompat.Builder()
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.getTitle())
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.getArtistName())
                .putLong(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION, duration); // Thêm tổng thời gian để vẽ thanh progress

        if (coverBitmap != null) {
            metadataBuilder.putBitmap(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART, coverBitmap);
        }
        mediaSession.setMetadata(metadataBuilder.build());

        // 4. Tạo Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.app_logo)
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

        if (coverBitmap != null) {
            builder.setLargeIcon(coverBitmap);
        }

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private PendingIntent createActionIntent(String action, int requestCode) {
        Intent intent = new Intent(this, AudioPlayerService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void broadcastState() {
        Intent stateIntent = new Intent(ACTION_STATE_CHANGED); // implicit intent, ai cung nhan duoc
        stateIntent.setPackage(getPackageName()); // intent chi phat noi bo app
        stateIntent.putExtra(EXTRA_SONG_ID, currentSongId);
        stateIntent.putExtra("playing", isPlaying);
        stateIntent.putExtra("position", mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0);
        stateIntent.putExtra("duration", mediaPlayer != null ? mediaPlayer.getDuration() : 0);
        stateIntent.putExtra(EXTRA_SPEED, playbackSpeed);
        sendBroadcast(stateIntent); // gui
    }

    // giai phong player
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
        persistLastListen();
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
