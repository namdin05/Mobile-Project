package com.melodix.app.Service;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;
import com.melodix.app.Model.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
public final class PlayerManager {

    private static final String TAG = "PlayerManager";

    private static volatile PlayerManager instance;

    private final Context appContext;
    private final Executor mainExecutor;
    private final Object controllerLock = new Object();

    private final List<ControllerTask> pendingTasks = new ArrayList<>();

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;



    private final MutableLiveData<Boolean> isPlayingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> currentSongIdLiveData = new MutableLiveData<>("");

    public LiveData<Boolean> getIsPlayingLiveData() { return isPlayingLiveData; }
    public LiveData<String> getCurrentSongIdLiveData() { return currentSongIdLiveData; }

    private PlayerManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.mainExecutor = ContextCompat.getMainExecutor(appContext);
    }

    public static PlayerManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PlayerManager.class) {
                if (instance == null) {
                    instance = new PlayerManager(context);
                }
            }
        }
        return instance;
    }

    public void playSongsFromStart(List<Song> songs) {
        playSongs(songs, 0);
    }

    public void playSongs(List<Song> songs, int requestedStartIndex) {
        final PlaylistPayload payload = buildPlaylistPayload(songs, requestedStartIndex);

        if (payload.mediaItems.isEmpty()) {
            Log.w(TAG, "playSongs: no playable items");
            return;
        }

        runWhenControllerReady(new ControllerTask() {
            @Override
            public void run(MediaController controller) {
                controller.setShuffleModeEnabled(false);
                controller.setRepeatMode(Player.REPEAT_MODE_OFF);
                controller.setMediaItems(payload.mediaItems, payload.startIndex, 0L);
                controller.prepare();
                controller.play();
            }
        });
    }
    public void togglePlayPause() {
        runWhenControllerReady(controller -> {
            if (controller.isPlaying()) {
                controller.pause();
            } else {
                controller.play();
            }
        });
    }

    public void clearPlaylist() {
        runWhenControllerReady(new ControllerTask() {
            @Override
            public void run(MediaController controller) {
                controller.pause();
                controller.clearMediaItems();
            }
        });
    }

    public void release() {
        ListenableFuture<MediaController> futureToRelease = null;

        synchronized (controllerLock) {
            if (controllerFuture != null) {
                futureToRelease = controllerFuture;
                controllerFuture = null;
            }
            mediaController = null;
            pendingTasks.clear();
        }

        if (futureToRelease != null) {
            MediaController.releaseFuture(futureToRelease);
        }
    }

    private void runWhenControllerReady(ControllerTask task) {
        MediaController readyController;

        synchronized (controllerLock) {
            readyController = mediaController;

            if (readyController == null) {
                pendingTasks.add(task);

                if (controllerFuture == null) {
                    buildControllerAsync();
                }
                return;
            }
        }

        task.run(readyController);
    }

    private void buildControllerAsync() {
        SessionToken sessionToken =
                new SessionToken(appContext, new ComponentName(appContext, MusicService.class));

        controllerFuture = new MediaController.Builder(appContext, sessionToken)
                .setListener(new MediaController.Listener() {
                    @Override
                    public void onDisconnected(MediaController controller) {
                        synchronized (controllerLock) {
                            if (mediaController == controller) {
                                mediaController = null;
                            }
                            controllerFuture = null;
                            pendingTasks.clear();
                        }
                    }
                })
                .buildAsync();

        final ListenableFuture<MediaController> localFuture = controllerFuture;
        localFuture.addListener(new Runnable() {
            @Override
            public void run() {
                onControllerReady(localFuture);
            }
        }, mainExecutor);
    }

    private void onControllerReady(ListenableFuture<MediaController> future) {
        MediaController controller = null;
        List<ControllerTask> tasksToRun = new ArrayList<>();

        try {
            controller = future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Controller connection interrupted", e);
        } catch (ExecutionException e) {
            Log.e(TAG, "Controller connection failed", e);
        }

        synchronized (controllerLock) {
            if (future != controllerFuture) {
                return;
            }

            if (controller == null) {
                pendingTasks.clear();
                controllerFuture = null;
                mediaController = null;
                return;
            }

            mediaController = controller;
            controller.addListener(new Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    isPlayingLiveData.postValue(isPlaying);
                }

                @Override
                public void onMediaItemTransition(@androidx.annotation.Nullable MediaItem mediaItem, int reason) {
                    if (mediaItem != null) {
                        currentSongIdLiveData.postValue(mediaItem.mediaId);
                    } else {
                        currentSongIdLiveData.postValue("");
                    }
                }
            });

            // Cập nhật trạng thái ngay lần đầu kết nối:
            isPlayingLiveData.postValue(controller.isPlaying());
            if (controller.getCurrentMediaItem() != null) {
                currentSongIdLiveData.postValue(controller.getCurrentMediaItem().mediaId);
            }
            tasksToRun.addAll(pendingTasks);
            pendingTasks.clear();
        }

        for (ControllerTask task : tasksToRun) {
            task.run(controller);
        }
    }

    private PlaylistPayload buildPlaylistPayload(List<Song> songs, int requestedStartIndex) {
        ArrayList<MediaItem> mediaItems = new ArrayList<>();

        if (songs == null || songs.isEmpty()) {
            return new PlaylistPayload(mediaItems, 0);
        }

        int safeRequestedIndex = Math.max(0, requestedStartIndex);
        int resolvedStartIndex = -1;

        for (int originalIndex = 0; originalIndex < songs.size(); originalIndex++) {
            Song song = songs.get(originalIndex);

            if (!isPlayable(song)) {
                continue;
            }

            if (originalIndex == safeRequestedIndex) {
                resolvedStartIndex = mediaItems.size();
            }

            mediaItems.add(buildMediaItem(song));
        }

        if (mediaItems.isEmpty()) {
            return new PlaylistPayload(mediaItems, 0);
        }

        if (resolvedStartIndex < 0 || resolvedStartIndex >= mediaItems.size()) {
            resolvedStartIndex = 0;
        }

        return new PlaylistPayload(mediaItems, resolvedStartIndex);
    }

    private boolean isPlayable(Song song) {
        return song != null && !TextUtils.isEmpty(song.getAudioUrl());
    }

    private MediaItem buildMediaItem(Song song) {
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();

        if (!TextUtils.isEmpty(song.getTitle())) {
            metadataBuilder.setTitle(song.getTitle());
        }

        if (!TextUtils.isEmpty(song.getArtistName())) {
            metadataBuilder.setArtist(song.getArtistName());
        }

        if (!TextUtils.isEmpty(song.getAlbumTitle())) {
            metadataBuilder.setAlbumTitle(song.getAlbumTitle());
        }

        Uri artworkUri = tryParseUri(song.getCoverUrl());
        if (artworkUri != null) {
            metadataBuilder.setArtworkUri(artworkUri);
        }

        return new MediaItem.Builder()
                .setMediaId(resolveMediaId(song))
                .setUri(Uri.parse(song.getAudioUrl()))
                .setMediaMetadata(metadataBuilder.build())
                .build();
    }

    private String resolveMediaId(Song song) {
        if (!TextUtils.isEmpty(song.getId())) {
            return song.getId();
        }
        return safe(song.getAudioUrl());
    }

    private Uri tryParseUri(String rawUri) {
        if (TextUtils.isEmpty(rawUri)) {
            return null;
        }
        return Uri.parse(rawUri);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private interface ControllerTask {
        void run(MediaController controller);
    }

    private static final class PlaylistPayload {
        private final ArrayList<MediaItem> mediaItems;
        private final int startIndex;

        private PlaylistPayload(ArrayList<MediaItem> mediaItems, int startIndex) {
            this.mediaItems = mediaItems;
            this.startIndex = startIndex;
        }
    }

    public void skipToNext() {
        runWhenControllerReady(controller -> {
            if (controller.hasNextMediaItem()) {
                controller.seekToNext();
            }
        });
    }

    public void skipToPrevious() {
        runWhenControllerReady(controller -> {
            if (controller.hasPreviousMediaItem()) {
                controller.seekToPrevious();
            }
        });
    }

    public void seekTo(long positionMs) {
        runWhenControllerReady(controller -> controller.seekTo(positionMs));
    }

    public long getCurrentPosition() {
        return mediaController != null ? mediaController.getCurrentPosition() : 0;
    }

    public long getDuration() {
        return mediaController != null ? mediaController.getDuration() : 0;
    }
}