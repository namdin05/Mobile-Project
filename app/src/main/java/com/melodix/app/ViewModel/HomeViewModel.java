package com.melodix.app.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.melodix.app.Model.Banner;
import com.melodix.app.Model.Genre;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.BannerRepository;
import com.melodix.app.Repository.GenreRepository;
import com.melodix.app.Repository.SongRepository;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {
    private SongRepository songRepository;
    private GenreRepository genreRepository;
    private BannerRepository bannerRepository;

    private LiveData<List<Song>> newReleases;
    private LiveData<List<Song>> trendingSongs;
    private LiveData<List<Genre>> genres;
    private LiveData<List<Banner>> banners;
    

    public HomeViewModel(@NonNull Application application) {
        super(application);

        
        songRepository = new SongRepository(application);
        genreRepository = new GenreRepository(application);
        bannerRepository = new BannerRepository(application);
    }


    public LiveData<List<Song>> getNewReleases() {
        if (newReleases == null) {
            newReleases = songRepository.fetchNewReleaseSongs();
        }
        return newReleases;
    }

    
    public LiveData<List<Song>> getSongsByGenre(int genreId) {
        
        
        return songRepository.fetchSongsByGenre(genreId);
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
