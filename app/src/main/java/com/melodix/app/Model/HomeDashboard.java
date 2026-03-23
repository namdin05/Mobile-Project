package com.melodix.app.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HomeDashboard implements Serializable {

    private List<HomeBanner> banners;
    private List<Song> trendingSongs;
    private List<Genre> genres;
    private List<Album> newAlbums;

    public HomeDashboard() {
        banners = new ArrayList<>();
        trendingSongs = new ArrayList<>();
        genres = new ArrayList<>();
        newAlbums = new ArrayList<>();
    }

    public boolean isEmpty() {
        return banners.isEmpty()
                && trendingSongs.isEmpty()
                && genres.isEmpty()
                && newAlbums.isEmpty();
    }

    public List<HomeBanner> getBanners() {
        return banners == null ? new ArrayList<>() : banners;
    }

    public void setBanners(List<HomeBanner> banners) {
        this.banners = banners == null ? new ArrayList<>() : banners;
    }

    public List<Song> getTrendingSongs() {
        return trendingSongs == null ? new ArrayList<>() : trendingSongs;
    }

    public void setTrendingSongs(List<Song> trendingSongs) {
        this.trendingSongs = trendingSongs == null ? new ArrayList<>() : trendingSongs;
    }

    public List<Genre> getGenres() {
        return genres == null ? new ArrayList<>() : genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres == null ? new ArrayList<>() : genres;
    }

    public List<Album> getNewAlbums() {
        return newAlbums == null ? new ArrayList<>() : newAlbums;
    }

    public void setNewAlbums(List<Album> newAlbums) {
        this.newAlbums = newAlbums == null ? new ArrayList<>() : newAlbums;
    }
}