package com.melodix.app.Model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.Serializable;

public class Song implements Serializable {

    private String id;
    private String title;
    private String artistName;
    private String albumId;
    private String albumTitle;
    private String coverUrl;
    private String audioUrl;
    private String genre;
    private long playCount;
    private double trendingScore;
    private Timestamp releaseTimestamp;
    private boolean active;

    public Song() {
    }

    public static Song fromDocument(DocumentSnapshot document) {
        Song song = new Song();

        if (document == null || !document.exists()) {
            return song;
        }

        try {
            song.setId(document.getId());
            song.setTitle(getSafeString(document, "title"));
            song.setArtistName(getSafeString(document, "artistName"));
            song.setAlbumId(getSafeString(document, "albumId"));
            song.setAlbumTitle(getSafeString(document, "albumTitle"));
            song.setCoverUrl(getSafeString(document, "coverUrl"));
            song.setAudioUrl(getSafeString(document, "audioUrl"));
            song.setGenre(getSafeString(document, "genre"));
            song.setPlayCount(getSafeLong(document.get("playCount")));
            song.setTrendingScore(getSafeDouble(document.get("trendingScore")));
            song.setReleaseTimestamp(document.getTimestamp("releaseTimestamp"));

            Boolean activeValue = document.getBoolean("active");
            song.setActive(activeValue == null || activeValue);
        } catch (Exception ignored) {
        }

        return song;
    }

    private static String getSafeString(DocumentSnapshot document, String key) {
        try {
            String value = document.getString(key);
            return value == null ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }

    private static long getSafeLong(Object value) {
        try {
            if (value == null) {
                return 0L;
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static double getSafeDouble(Object value) {
        try {
            if (value == null) {
                return 0D;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0D;
        }
    }

    public String getDisplayTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }
        return "Untitled Song";
    }

    public String getDisplaySubtitle() {
        if (artistName != null && !artistName.trim().isEmpty()
                && genre != null && !genre.trim().isEmpty()) {
            return artistName + " • " + genre;
        }

        if (artistName != null && !artistName.trim().isEmpty()) {
            return artistName;
        }

        if (albumTitle != null && !albumTitle.trim().isEmpty()) {
            return albumTitle;
        }

        return "Unknown Artist";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName == null ? "" : artistName;
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId == null ? "" : albumId;
    }

    public String getAlbumTitle() {
        return albumTitle;
    }

    public void setAlbumTitle(String albumTitle) {
        this.albumTitle = albumTitle == null ? "" : albumTitle;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl == null ? "" : coverUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl == null ? "" : audioUrl;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre == null ? "" : genre;
    }

    public long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(long playCount) {
        this.playCount = Math.max(playCount, 0L);
    }

    public double getTrendingScore() {
        return trendingScore;
    }

    public void setTrendingScore(double trendingScore) {
        this.trendingScore = Math.max(trendingScore, 0D);
    }

    public Timestamp getReleaseTimestamp() {
        return releaseTimestamp;
    }

    public void setReleaseTimestamp(Timestamp releaseTimestamp) {
        this.releaseTimestamp = releaseTimestamp;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}