package com.melodix.app.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.melodix.app.Model.Banner;
import com.melodix.app.Model.Genre;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.BannerRepository;

import java.util.List;

public class BannerViewModel extends AndroidViewModel {
    private BannerRepository bannerRepository;

    private LiveData<List<Song>> newReleases;
    private LiveData<List<Song>> trendingSongs;
    private LiveData<List<Genre>> genres;
    private LiveData<List<Banner>> banners;
    private LiveData<List<Song>> allSong;

    public BannerViewModel(@NonNull Application application) {
        super(application);

        
        bannerRepository = new BannerRepository(application);
    }

    public LiveData<List<Banner>> getBanners() {
        if (banners == null) {
            banners = bannerRepository.fetchBanners();
        }
        return banners;
    }
}
