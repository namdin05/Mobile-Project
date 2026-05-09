package com.melodix.app.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Model.Song;
import com.melodix.app.Repository.SongRepository;

import java.util.List;

public class SongViewModel extends AndroidViewModel {
    private SongRepository songRepository;

    private final MutableLiveData<Boolean> actionSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> actionMessage = new MutableLiveData<>();

    private LiveData<List<Song>> newReleases;
    private LiveData<List<Song>> trendingSongs;
    private LiveData<List<Song>> allSong;

    public SongViewModel(@NonNull Application application) {
        super(application);

        // Truyền thẳng cái 'application' (Context) xuống cho Repository
        songRepository = new SongRepository(application);
    }

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

    public LiveData<Boolean> getActionSuccess() { return actionSuccess; }
    public LiveData<String> getActionMessage() { return actionMessage; }

    // Hàm này sẽ được PlayerActivity gọi
    public void updateSongStatus(String songId, String newStatus) {
        songRepository.updateSongStatus(songId, newStatus, actionSuccess, actionMessage);
    }
}
