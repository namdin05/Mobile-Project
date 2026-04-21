package com.melodix.app.ViewModel;

import static android.widget.Toast.LENGTH_SHORT;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
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

public class BannerViewModel extends AndroidViewModel {
    private BannerRepository bannerRepository;

    private LiveData<List<Song>> newReleases;
    private LiveData<List<Song>> trendingSongs;
    private LiveData<List<Genre>> genres;
    private LiveData<List<Banner>> banners;
    private LiveData<List<Song>> allSong;

    public BannerViewModel(@NonNull Application application) {
        super(application);

        // Truyền thẳng cái 'application' (Context) xuống cho Repository
        bannerRepository = new BannerRepository(application);
    }

    public LiveData<List<Banner>> getBanners() {
        if (banners == null) {
            banners = bannerRepository.fetchBanners();
        }
        return banners;
    }
}
