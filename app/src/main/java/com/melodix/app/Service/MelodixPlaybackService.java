package com.melodix.app.Service;

import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.melodix.app.MainActivity;
import com.melodix.app.R;
import com.melodix.app.Utils.AppConstants;
import com.melodix.app.View.player.PlayerActivity;

@UnstableApi
public class MelodixPlaybackService extends MediaSessionService {

    private static final String TAG = "MelodixPlaybackService";

    private ExoPlayer exoPlayer;
    private MediaSession mediaSession;
    private DefaultMediaNotificationProvider notificationProvider;

    @Override
    public void onCreate() {
        super.onCreate();
        initializePlayerSafely();
    }

    private void initializePlayerSafely() {
        try {
            if (exoPlayer == null) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build();

                exoPlayer = new ExoPlayer.Builder(this).build();
                exoPlayer.setAudioAttributes(audioAttributes, true);
                exoPlayer.setHandleAudioBecomingNoisy(true);
                exoPlayer.setWakeMode(C.WAKE_MODE_NETWORK);
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
            }

            if (notificationProvider == null) {
                notificationProvider = new DefaultMediaNotificationProvider.Builder(this)
                        .setNotificationId(AppConstants.PLAYER_NOTIFICATION_ID)
                        .setChannelId(AppConstants.PLAYER_NOTIFICATION_CHANNEL_ID)
                        .setChannelName(R.string.media_notification_channel_name)
                        .build();

                try {
                    notificationProvider.setSmallIcon(R.drawable.ic_melodix_notification);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set custom notification small icon.", e);
                }

                try {
                    setMediaNotificationProvider(notificationProvider);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set media notification provider.", e);
                }
            }

            if (mediaSession == null && exoPlayer != null) {
                PendingIntent sessionActivity = buildSessionActivityPendingIntent();

                mediaSession = new MediaSession.Builder(this, exoPlayer)
                        .setSessionActivity(sessionActivity)
                        .build();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ExoPlayer / MediaSession.", e);
        }
    }

    @NonNull
    private PendingIntent buildSessionActivityPendingIntent() {
        try {
            Intent playerIntent = new Intent(this, PlayerActivity.class);
            playerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            return PendingIntent.getActivity(
                    this,
                    1001,
                    playerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to create PlayerActivity pending intent. Falling back to MainActivity.", e);

            Intent fallbackIntent = new Intent(this, MainActivity.class);
            fallbackIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            return PendingIntent.getActivity(
                    this,
                    1002,
                    fallbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        }
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        try {
            return mediaSession;
        } catch (Exception e) {
            Log.e(TAG, "onGetSession failed.", e);
            return null;
        }
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        try {
            if (!isPlaybackOngoing()) {
                stopSelf();
            }
        } catch (Exception e) {
            Log.e(TAG, "onTaskRemoved failed.", e);
            stopSelf();
        }

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        try {
            if (mediaSession != null) {
                mediaSession.release();
                mediaSession = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to release mediaSession.", e);
        }

        try {
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to release exoPlayer.", e);
        }

        notificationProvider = null;
        super.onDestroy();
    }
}