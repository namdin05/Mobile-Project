package com.melodix.app.Model;

public class Album {
    private String id;
    private String title;
    private String artistId;
    private String artistName;
    private String coverUrl;
    private String headerImageUrl;
    private String genre;
    private String releaseDate;
    private int totalTracks;
    private long totalDurationMs;
    private String description;
    private boolean single;

    public Album() {
    }

    public Album(String id,
                 String title,
                 String artistId,
                 String artistName,
                 String coverUrl,
                 String headerImageUrl,
                 String genre,
                 String releaseDate,
                 int totalTracks,
                 long totalDurationMs,
                 String description,
                 boolean single) {
        this.id = id;
        this.title = title;
        this.artistId = artistId;
        this.artistName = artistName;
        this.coverUrl = coverUrl;
        this.headerImageUrl = headerImageUrl;
        this.genre = genre;
        this.releaseDate = releaseDate;
        this.totalTracks = totalTracks;
        this.totalDurationMs = totalDurationMs;
        this.description = description;
        this.single = single;
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

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getHeaderImageUrl() {
        return headerImageUrl;
    }

    public void setHeaderImageUrl(String headerImageUrl) {
        this.headerImageUrl = headerImageUrl;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public int getTotalTracks() {
        return totalTracks;
    }

    public void setTotalTracks(int totalTracks) {
        this.totalTracks = totalTracks;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSingle() {
        return single;
    }

    public void setSingle(boolean single) {
        this.single = single;
    }
}