package com.melodix.app.Model;

public class Artist {
    private String id;
    private String name;
    private String imageUrl;
    private String headerImageUrl;
    private String genre;
    private String bio;
    private long monthlyListeners;
    private long followers;
    private int totalAlbums;
    private int totalTracks;
    private boolean verified;

    public Artist() {
    }

    public Artist(String id,
                  String name,
                  String imageUrl,
                  String headerImageUrl,
                  String genre,
                  String bio,
                  long monthlyListeners,
                  long followers,
                  int totalAlbums,
                  int totalTracks,
                  boolean verified) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.headerImageUrl = headerImageUrl;
        this.genre = genre;
        this.bio = bio;
        this.monthlyListeners = monthlyListeners;
        this.followers = followers;
        this.totalAlbums = totalAlbums;
        this.totalTracks = totalTracks;
        this.verified = verified;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public long getMonthlyListeners() {
        return monthlyListeners;
    }

    public void setMonthlyListeners(long monthlyListeners) {
        this.monthlyListeners = monthlyListeners;
    }

    public long getFollowers() {
        return followers;
    }

    public void setFollowers(long followers) {
        this.followers = followers;
    }

    public int getTotalAlbums() {
        return totalAlbums;
    }

    public void setTotalAlbums(int totalAlbums) {
        this.totalAlbums = totalAlbums;
    }

    public int getTotalTracks() {
        return totalTracks;
    }

    public void setTotalTracks(int totalTracks) {
        this.totalTracks = totalTracks;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}