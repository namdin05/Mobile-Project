package com.melodix.app.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.melodix.app.Model.Banner;
import com.melodix.app.Model.Genre;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.auth.AuthRepository;

import java.util.List;

public class HomeViewModel extends ViewModel {
    private AuthRepository repository = new AuthRepository();

    private LiveData<List<Song>> newReleases;
    private LiveData<List<Song>> trendingSongs;
    private LiveData<List<Genre>> genres;
    private LiveData<List<Banner>> banners;

    public LiveData<List<Song>> getNewReleases() {
        if (newReleases == null) {
            newReleases = repository.fetchNewReleaseSongs();
        }
        return newReleases;
    }

    public LiveData<List<Song>> getTrendingSongs() {
        if (trendingSongs == null) {
            trendingSongs = repository.fetchTrendingSongs();
        }
        return trendingSongs;
    }

    public LiveData<List<Genre>> getGenres() {
        if (genres == null) {
            genres = repository.fetchGenres();
        }
        return genres;
    }

    public LiveData<List<Banner>> getBanners() {
        if (banners == null) {
            banners = repository.fetchBanners();
        }
        return banners;
    }

}
