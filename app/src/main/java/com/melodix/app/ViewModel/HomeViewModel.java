package com.melodix.app.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.melodix.app.Model.Banner;
import com.melodix.app.Model.Genre;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.BannerRepository;
import com.melodix.app.Repository.GenreRepository;
import com.melodix.app.Repository.SongRepository;
import com.melodix.app.Repository.auth.AuthRepository;

import java.util.List;

public class HomeViewModel extends ViewModel {
    private SongRepository songRepository = new SongRepository();
    private GenreRepository genreRepository = new GenreRepository();
    private BannerRepository bannerRepository = new BannerRepository();

    private LiveData<List<Song>> newReleases;
    private LiveData<List<Song>> trendingSongs;
    private LiveData<List<Genre>> genres;
    private LiveData<List<Banner>> banners;
    // view model chi goi API 1 lan va giu cac data du Activity, fragment bi destroy hay ko
    public LiveData<List<Song>> getNewReleases() {
        if (newReleases == null) {
            newReleases = songRepository.fetchNewReleaseSongs();
        }
        return newReleases;
    }

    // Bạn ném hàm này vào trong class HomeViewModel nhé
    public LiveData<List<Song>> getSongsByGenre(int genreId) {
        // Khác với list Trending hay New Release (chỉ load 1 lần),
        // list nhạc theo thể loại sẽ phụ thuộc vào ID người dùng bấm, nên ta gọi thẳng từ Repository.
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
