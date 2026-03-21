package com.melodix.app.Model;

public class Song {
    private String id;
    private String title;
    private String artistId;
    private String artistName;
    private String albumId;
    private String albumTitle;
    private String genre;
    private String coverUrl;
    private String audioUrl;
    private long durationMs;
    private int trackNumber;
    private boolean explicit;
    private boolean favorite;

    public Song() {
    }

    public Song(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public Song(String id,
                String title,
                String artistId,
                String artistName,
                String albumId,
                String albumTitle,
                String genre,
                String coverUrl,
                String audioUrl,
                long durationMs,
                int trackNumber,
                boolean explicit,
                boolean favorite) {
        this.id = id;
        this.title = title;
        this.artistId = artistId;
        this.artistName = artistName;
        this.albumId = albumId;
        this.albumTitle = albumTitle;
        this.genre = genre;
        this.coverUrl = coverUrl;
        this.audioUrl = audioUrl;
        this.durationMs = durationMs;
        this.trackNumber = trackNumber;
        this.explicit = explicit;
        this.favorite = favorite;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtistId() {
        return artistId;
    }

    public void setArtistId(String artistId) {
        this.artistId = artistId;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public String getAlbumTitle() {
        return albumTitle;
    }

    public void setAlbumTitle(String albumTitle) {
        this.albumTitle = albumTitle;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public boolean isExplicit() {
        return explicit;
    }

    public void setExplicit(boolean explicit) {
        this.explicit = explicit;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}