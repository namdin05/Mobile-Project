package com.melodix.app.Model;

import com.melodix.app.Utils.AppConstants;

import java.io.Serializable;
import java.util.Locale;

public class PlayerUiState implements Serializable {

    private boolean connected;
    private boolean visible;
    private boolean playing;
    private boolean loading;
    private boolean hasPrevious;
    private boolean hasNext;
    private long currentPosition;
    private long duration;
    private int currentIndex;
    private int queueSize;
    private String mediaId;
    private String title;
    private String subtitle;
    private String coverUrl;
    private float playbackSpeed;

    public PlayerUiState() {
        connected = false;
        visible = false;
        playing = false;
        loading = false;
        hasPrevious = false;
        hasNext = false;
        currentPosition = 0L;
        duration = 0L;
        currentIndex = 0;
        queueSize = 0;
        mediaId = "";
        title = "";
        subtitle = "";
        coverUrl = "";
        playbackSpeed = AppConstants.PLAYER_DEFAULT_SPEED;
    }

    public static PlayerUiState idle() {
        return new PlayerUiState();
    }

    public String getDisplayTitle() {
        return title == null || title.trim().isEmpty() ? "No song playing" : title;
    }

    public String getDisplaySubtitle() {
        return subtitle == null || subtitle.trim().isEmpty() ? "Melodix" : subtitle;
    }

    public String getPlaybackSpeedLabel() {
        try {
            float speed = playbackSpeed <= 0f ? AppConstants.PLAYER_DEFAULT_SPEED : playbackSpeed;

            if (Math.abs(speed - Math.round(speed)) < 0.001f) {
                return String.format(Locale.getDefault(), "%.1fx", speed);
            }

            if (Math.abs((speed * 10f) - Math.round(speed * 10f)) < 0.001f) {
                return String.format(Locale.getDefault(), "%.1fx", speed);
            }

            return String.format(Locale.getDefault(), "%.2fx", speed);
        } catch (Exception e) {
            return "1.0x";
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public long getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(long currentPosition) {
        this.currentPosition = Math.max(currentPosition, 0L);
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = Math.max(duration, 0L);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = Math.max(currentIndex, 0);
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = Math.max(queueSize, 0);
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId == null ? "" : mediaId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle == null ? "" : subtitle;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl == null ? "" : coverUrl;
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(float playbackSpeed) {
        if (playbackSpeed <= 0f) {
            this.playbackSpeed = AppConstants.PLAYER_DEFAULT_SPEED;
            return;
        }
        this.playbackSpeed = playbackSpeed;
    }
}