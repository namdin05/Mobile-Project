package com.melodix.app.Service;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;
import com.melodix.app.Model.PlayerUiState;
import com.melodix.app.Model.Song;
import com.melodix.app.Utils.AppConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MelodixPlayerManager {

    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface PlayerStateListener {
        void onPlayerStateChanged(PlayerUiState state);
    }

    private static final String TAG = "MelodixPlayerManager";
    private static MelodixPlayerManager instance;

    private final Context appContext;
    private final Handler progressHandler;
    private final Set<PlayerStateListener> listeners;
    private final List<ActionCallback> pendingConnectionCallbacks;

    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private boolean isConnecting;

    private final Player.Listener internalPlayerListener = new Player.Listener() {
        @Override
        public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
            notifyStateChanged();
        }
    };

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                notifyStateChanged();
            } catch (Exception e) {
                Log.e(TAG, "progressRunnable notify failed.", e);
            }

            try {
                if (mediaController != null && !listeners.isEmpty()) {
                    progressHandler.postDelayed(this, 500L);
                }
            } catch (Exception e) {
                Log.e(TAG, "progressRunnable scheduling failed.", e);
            }
        }
    };

    private MelodixPlayerManager(Context context) {
        appContext = context.getApplicationContext();
        progressHandler = new Handler(Looper.getMainLooper());
        listeners = new CopyOnWriteArraySet<>();
        pendingConnectionCallbacks = new ArrayList<>();
        isConnecting = false;
    }

    public static synchronized MelodixPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new MelodixPlayerManager(context);
        }
        return instance;
    }

    public void addListener(@Nullable PlayerStateListener listener) {
        try {
            if (listener == null) {
                return;
            }

            listeners.add(listener);
            listener.onPlayerStateChanged(buildCurrentState());

            if (!listeners.isEmpty()) {
                startProgressUpdates();
            }
        } catch (Exception e) {
            Log.e(TAG, "addListener failed.", e);
        }
    }

    public void removeListener(@Nullable PlayerStateListener listener) {
        try {
            if (listener != null) {
                listeners.remove(listener);
            }

            if (listeners.isEmpty()) {
                stopProgressUpdates();
            }
        } catch (Exception e) {
            Log.e(TAG, "removeListener failed.", e);
        }
    }

    public void connect(@Nullable ActionCallback callback) {
        try {
            if (mediaController != null) {
                dispatchActionSuccess(callback);
                notifyStateChanged();
                return;
            }

            if (callback != null) {
                pendingConnectionCallbacks.add(callback);
            }

            if (isConnecting) {
                return;
            }

            isConnecting = true;

            SessionToken sessionToken = new SessionToken(
                    appContext,
                    new ComponentName(appContext, MelodixPlaybackService.class)
            );

            controllerFuture = new MediaController.Builder(appContext, sessionToken).buildAsync();
            controllerFuture.addListener(() -> {
                try {
                    mediaController = controllerFuture.get();

                    if (mediaController != null) {
                        mediaController.addListener(internalPlayerListener);
                    }

                    isConnecting = false;
                    startProgressUpdates();
                    notifyStateChanged();
                    flushPendingConnectionSuccess();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to connect MediaController.", e);
                    isConnecting = false;
                    mediaController = null;
                    flushPendingConnectionError("Không thể kết nối tới Music Service.");
                }
            }, ContextCompat.getMainExecutor(appContext));
        } catch (Exception e) {
            Log.e(TAG, "connect failed.", e);
            isConnecting = false;
            flushPendingConnectionError("Không thể kết nối tới Music Service.");
            dispatchActionError(callback, "Không thể kết nối tới Music Service.");
        }
    }

    public void playSongs(@Nullable List<Song> songs, int requestedStartIndex, @Nullable ActionCallback callback) {
        try {
            PreparedQueue preparedQueue = prepareQueue(songs, requestedStartIndex);

            if (preparedQueue.mediaItems.isEmpty()) {
                dispatchActionError(callback, "Danh sách phát không hợp lệ hoặc chưa có audioUrl streaming.");
                return;
            }

            connect(new ActionCallback() {
                @Override
                public void onSuccess() {
                    try {
                        if (mediaController == null) {
                            dispatchActionError(callback, "Player chưa sẵn sàng.");
                            return;
                        }

                        mediaController.setMediaItems(preparedQueue.mediaItems, preparedQueue.startIndex, 0L);
                        mediaController.prepare();
                        mediaController.play();
                        notifyStateChanged();
                        dispatchActionSuccess(callback);
                    } catch (Exception e) {
                        Log.e(TAG, "playSongs action failed.", e);
                        dispatchActionError(callback, "Không thể bắt đầu phát nhạc.");
                    }
                }

                @Override
                public void onError(String message) {
                    dispatchActionError(callback, message);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "playSongs failed.", e);
            dispatchActionError(callback, "Không thể bắt đầu phát nhạc.");
        }
    }

    public void togglePlayPause(@Nullable ActionCallback callback) {
        connect(new ActionCallback() {
            @Override
            public void onSuccess() {
                try {
                    if (mediaController == null || mediaController.getCurrentMediaItem() == null) {
                        dispatchActionError(callback, "Chưa có bài hát nào đang được chọn.");
                        return;
                    }

                    if (mediaController.isPlaying()) {
                        mediaController.pause();
                    } else {
                        mediaController.play();
                    }

                    notifyStateChanged();
                    dispatchActionSuccess(callback);
                } catch (Exception e) {
                    Log.e(TAG, "togglePlayPause failed.", e);
                    dispatchActionError(callback, "Không thể thay đổi trạng thái phát nhạc.");
                }
            }

            @Override
            public void onError(String message) {
                dispatchActionError(callback, message);
            }
        });
    }

    public void skipToNext(@Nullable ActionCallback callback) {
        connect(new ActionCallback() {
            @Override
            public void onSuccess() {
                try {
                    if (mediaController == null || mediaController.getCurrentMediaItem() == null) {
                        dispatchActionError(callback, "Chưa có bài hát nào đang được chọn.");
                        return;
                    }

                    if (!mediaController.hasNextMediaItem()) {
                        dispatchActionError(callback, "Bạn đang ở cuối hàng đợi.");
                        return;
                    }

                    mediaController.seekToNextMediaItem();
                    mediaController.play();
                    notifyStateChanged();
                    dispatchActionSuccess(callback);
                } catch (Exception e) {
                    Log.e(TAG, "skipToNext failed.", e);
                    dispatchActionError(callback, "Không thể chuyển bài tiếp theo.");
                }
            }

            @Override
            public void onError(String message) {
                dispatchActionError(callback, message);
            }
        });
    }

    public void skipToPrevious(@Nullable ActionCallback callback) {
        connect(new ActionCallback() {
            @Override
            public void onSuccess() {
                try {
                    if (mediaController == null || mediaController.getCurrentMediaItem() == null) {
                        dispatchActionError(callback, "Chưa có bài hát nào đang được chọn.");
                        return;
                    }

                    if (mediaController.hasPreviousMediaItem()) {
                        mediaController.seekToPreviousMediaItem();
                        mediaController.play();
                    } else {
                        mediaController.seekTo(0L);
                    }

                    notifyStateChanged();
                    dispatchActionSuccess(callback);
                } catch (Exception e) {
                    Log.e(TAG, "skipToPrevious failed.", e);
                    dispatchActionError(callback, "Không thể quay lại bài trước.");
                }
            }

            @Override
            public void onError(String message) {
                dispatchActionError(callback, message);
            }
        });
    }

    public void seekTo(long positionMs, @Nullable ActionCallback callback) {
        connect(new ActionCallback() {
            @Override
            public void onSuccess() {
                try {
                    if (mediaController == null || mediaController.getCurrentMediaItem() == null) {
                        dispatchActionError(callback, "Chưa có bài hát nào đang được chọn.");
                        return;
                    }

                    long duration = sanitizeDuration(mediaController.getDuration());
                    long targetPosition = Math.max(positionMs, 0L);

                    if (duration > 0L) {
                        targetPosition = Math.min(targetPosition, duration);
                    }

                    mediaController.seekTo(targetPosition);
                    notifyStateChanged();
                    dispatchActionSuccess(callback);
                } catch (Exception e) {
                    Log.e(TAG, "seekTo failed.", e);
                    dispatchActionError(callback, "Không thể tua tới vị trí đã chọn.");
                }
            }

            @Override
            public void onError(String message) {
                dispatchActionError(callback, message);
            }
        });
    }

    public void seekForward(@Nullable ActionCallback callback) {
        seekBy(AppConstants.PLAYER_SEEK_INTERVAL_MS, callback);
    }

    public void seekBackward(@Nullable ActionCallback callback) {
        seekBy(-AppConstants.PLAYER_SEEK_INTERVAL_MS, callback);
    }

    public void setPlaybackSpeed(float speed, @Nullable ActionCallback callback) {
        connect(new ActionCallback() {
            @Override
            public void onSuccess() {
                try {
                    if (mediaController == null) {
                        dispatchActionError(callback, "Player chưa sẵn sàng.");
                        return;
                    }

                    float safeSpeed = sanitizePlaybackSpeed(speed);
                    mediaController.setPlaybackParameters(new PlaybackParameters(safeSpeed, 1f));
                    notifyStateChanged();
                    dispatchActionSuccess(callback);
                } catch (Exception e) {
                    Log.e(TAG, "setPlaybackSpeed failed.", e);
                    dispatchActionError(callback, "Không thể thay đổi tốc độ phát.");
                }
            }

            @Override
            public void onError(String message) {
                dispatchActionError(callback, message);
            }
        });
    }

    public void stopAndClear(@Nullable ActionCallback callback) {
        try {
            if (mediaController == null) {
                dispatchActionSuccess(callback);
                notifyStateChanged();
                return;
            }

            mediaController.pause();
            mediaController.clearMediaItems();
            notifyStateChanged();
            dispatchActionSuccess(callback);
        } catch (Exception e) {
            Log.e(TAG, "stopAndClear failed.", e);
            dispatchActionError(callback, "Không thể dừng player.");
        }
    }

    public PlayerUiState getCurrentState() {
        return buildCurrentState();
    }

    private void seekBy(long deltaMs, @Nullable ActionCallback callback) {
        connect(new ActionCallback() {
            @Override
            public void onSuccess() {
                try {
                    if (mediaController == null || mediaController.getCurrentMediaItem() == null) {
                        dispatchActionError(callback, "Chưa có bài hát nào đang được chọn.");
                        return;
                    }

                    long currentPosition = sanitizePosition(mediaController.getCurrentPosition());
                    long duration = sanitizeDuration(mediaController.getDuration());

                    long targetPosition = Math.max(0L, currentPosition + deltaMs);
                    if (duration > 0L) {
                        targetPosition = Math.min(targetPosition, duration);
                    }

                    mediaController.seekTo(targetPosition);
                    notifyStateChanged();
                    dispatchActionSuccess(callback);
                } catch (Exception e) {
                    Log.e(TAG, "seekBy failed.", e);
                    dispatchActionError(callback, "Không thể tua bài hát.");
                }
            }

            @Override
            public void onError(String message) {
                dispatchActionError(callback, message);
            }
        });
    }

    private float sanitizePlaybackSpeed(float speed) {
        try {
            if (speed < AppConstants.PLAYER_MIN_SPEED) {
                return AppConstants.PLAYER_MIN_SPEED;
            }

            if (speed > AppConstants.PLAYER_MAX_SPEED) {
                return AppConstants.PLAYER_MAX_SPEED;
            }

            return speed;
        } catch (Exception e) {
            return AppConstants.PLAYER_DEFAULT_SPEED;
        }
    }

    private PreparedQueue prepareQueue(List<Song> songs, int requestedStartIndex) {
        PreparedQueue preparedQueue = new PreparedQueue();

        try {
            if (songs == null || songs.isEmpty()) {
                return preparedQueue;
            }

            int safeRequestedIndex = Math.max(0, Math.min(requestedStartIndex, songs.size() - 1));
            int playableIndexCounter = 0;

            for (int i = 0; i < songs.size(); i++) {
                Song song = songs.get(i);
                MediaItem mediaItem = buildMediaItem(song);

                if (mediaItem == null) {
                    continue;
                }

                if (i == safeRequestedIndex) {
                    preparedQueue.startIndex = playableIndexCounter;
                }

                preparedQueue.mediaItems.add(mediaItem);
                playableIndexCounter++;
            }

            if (preparedQueue.startIndex >= preparedQueue.mediaItems.size()) {
                preparedQueue.startIndex = 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "prepareQueue failed.", e);
        }

        return preparedQueue;
    }

    @Nullable
    private MediaItem buildMediaItem(@Nullable Song song) {
        try {
            if (song == null) {
                return null;
            }

            String audioUrl = song.getAudioUrl();
            if (TextUtils.isEmpty(audioUrl)) {
                return null;
            }

            MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                    .setTitle(song.getDisplayTitle())
                    .setArtist(song.getArtistName())
                    .setAlbumTitle(song.getAlbumTitle());

            if (!TextUtils.isEmpty(song.getCoverUrl())) {
                try {
                    metadataBuilder.setArtworkUri(Uri.parse(song.getCoverUrl()));
                } catch (Exception ignored) {
                }
            }

            return new MediaItem.Builder()
                    .setMediaId(TextUtils.isEmpty(song.getId()) ? song.getDisplayTitle() : song.getId())
                    .setUri(Uri.parse(audioUrl))
                    .setMediaMetadata(metadataBuilder.build())
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "buildMediaItem failed.", e);
            return null;
        }
    }

    private PlayerUiState buildCurrentState() {
        PlayerUiState state = PlayerUiState.idle();

        try {
            state.setConnected(mediaController != null);

            if (mediaController == null) {
                return state;
            }

            state.setPlaying(mediaController.isPlaying());
            state.setLoading(mediaController.getPlaybackState() == Player.STATE_BUFFERING);
            state.setQueueSize(mediaController.getMediaItemCount());

            int currentIndex = mediaController.getCurrentMediaItemIndex();
            state.setCurrentIndex(Math.max(currentIndex, 0));

            try {
                PlaybackParameters playbackParameters = mediaController.getPlaybackParameters();
                if (playbackParameters != null) {
                    state.setPlaybackSpeed(playbackParameters.speed);
                } else {
                    state.setPlaybackSpeed(AppConstants.PLAYER_DEFAULT_SPEED);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to read playback parameters.", e);
                state.setPlaybackSpeed(AppConstants.PLAYER_DEFAULT_SPEED);
            }

            MediaItem currentMediaItem = mediaController.getCurrentMediaItem();
            if (currentMediaItem == null) {
                state.setVisible(false);
                return state;
            }

            state.setVisible(true);
            state.setMediaId(safeString(currentMediaItem.mediaId));

            MediaMetadata metadata = currentMediaItem.mediaMetadata;
            if (metadata != null) {
                state.setTitle(safeCharSequence(metadata.title));
                state.setSubtitle(safeCharSequence(metadata.artist));

                if (metadata.artworkUri != null) {
                    state.setCoverUrl(metadata.artworkUri.toString());
                }
            }

            long currentPosition = sanitizePosition(mediaController.getCurrentPosition());
            long duration = sanitizeDuration(mediaController.getDuration());

            state.setCurrentPosition(currentPosition);
            state.setDuration(duration);
            state.setHasPrevious(mediaController.hasPreviousMediaItem() || currentPosition > 0L);
            state.setHasNext(mediaController.hasNextMediaItem());
        } catch (Exception e) {
            Log.e(TAG, "buildCurrentState failed.", e);
        }

        return state;
    }

    private long sanitizePosition(long positionMs) {
        try {
            if (positionMs == C.TIME_UNSET || positionMs < 0L) {
                return 0L;
            }
            return positionMs;
        } catch (Exception e) {
            return 0L;
        }
    }

    private long sanitizeDuration(long durationMs) {
        try {
            if (durationMs == C.TIME_UNSET || durationMs < 0L) {
                return 0L;
            }
            return durationMs;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String safeCharSequence(CharSequence value) {
        try {
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private void notifyStateChanged() {
        try {
            PlayerUiState state = buildCurrentState();

            for (PlayerStateListener listener : listeners) {
                try {
                    if (listener != null) {
                        listener.onPlayerStateChanged(state);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Listener notification failed.", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "notifyStateChanged failed.", e);
        }
    }

    private void startProgressUpdates() {
        try {
            progressHandler.removeCallbacks(progressRunnable);

            if (mediaController != null && !listeners.isEmpty()) {
                progressHandler.post(progressRunnable);
            }
        } catch (Exception e) {
            Log.e(TAG, "startProgressUpdates failed.", e);
        }
    }

    private void stopProgressUpdates() {
        try {
            progressHandler.removeCallbacks(progressRunnable);
        } catch (Exception e) {
            Log.e(TAG, "stopProgressUpdates failed.", e);
        }
    }

    private void flushPendingConnectionSuccess() {
        try {
            List<ActionCallback> callbacks = new ArrayList<>(pendingConnectionCallbacks);
            pendingConnectionCallbacks.clear();

            for (ActionCallback callback : callbacks) {
                dispatchActionSuccess(callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "flushPendingConnectionSuccess failed.", e);
        }
    }

    private void flushPendingConnectionError(String message) {
        try {
            List<ActionCallback> callbacks = new ArrayList<>(pendingConnectionCallbacks);
            pendingConnectionCallbacks.clear();

            for (ActionCallback callback : callbacks) {
                dispatchActionError(callback, message);
            }
        } catch (Exception e) {
            Log.e(TAG, "flushPendingConnectionError failed.", e);
        }
    }

    private void dispatchActionSuccess(@Nullable ActionCallback callback) {
        try {
            if (callback != null) {
                callback.onSuccess();
            }
        } catch (Exception e) {
            Log.e(TAG, "dispatchActionSuccess failed.", e);
        }
    }

    private void dispatchActionError(@Nullable ActionCallback callback, String message) {
        try {
            if (callback != null) {
                callback.onError(message == null ? "Đã xảy ra lỗi không xác định." : message);
            }
        } catch (Exception e) {
            Log.e(TAG, "dispatchActionError failed.", e);
        }
    }

    private static class PreparedQueue {
        private final List<MediaItem> mediaItems = new ArrayList<>();
        private int startIndex = 0;
    }
}