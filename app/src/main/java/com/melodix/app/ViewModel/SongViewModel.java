package com.melodix.app.ViewModel;

import static android.widget.Toast.LENGTH_SHORT;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.melodix.app.Model.Banner;
import com.melodix.app.Model.Genre;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.BannerRepository;
import com.melodix.app.Repository.GenreRepository;
import com.melodix.app.Repository.SongRepository;
import com.melodix.app.Repository.auth.AuthRepository;
import com.melodix.app.Utils.PlaybackUtils;

import java.util.ArrayList;
import java.util.List;

public class SongViewModel extends ViewModel {
    private SongRepository songRepository = new SongRepository();
    private GenreRepository genreRepository = new GenreRepository();
    private BannerRepository bannerRepository = new BannerRepository();

    private LiveData<List<Song>> newReleases;
    private LiveData<List<Song>> trendingSongs;
    private LiveData<List<Genre>> genres;
    private LiveData<List<Banner>> banners;

    private LiveData<List<Song>> allSong;

    public LiveData<List<Song>> getAllSong() {
        if (allSong == null) {
            allSong = songRepository.fetchAllSongs();
        }
        return allSong;
    }

    public LiveData<List<Song>> getNewReleases() {
        if (newReleases == null) {
            newReleases = songRepository.fetchNewReleaseSongs();
        }
        return newReleases;
    }

    public LiveData<List<Song>> getTrendingSongs() {
        if (trendingSongs == null) {
            trendingSongs = songRepository.fetchTrendingSongs();
        }
        return trendingSongs;
    }

    public LiveData<List<Genre>> getGenres() {
        if (genres == null) {
            genres = genreRepository.fetchGenres();
        }
        return genres;
    }

    public LiveData<List<Banner>> getBanners() {
        if (banners == null) {
            banners = bannerRepository.fetchBanners();
        }
        return banners;
    }


}
