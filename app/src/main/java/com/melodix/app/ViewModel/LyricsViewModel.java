package com.melodix.app.ViewModel;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Data.Resource;
import com.melodix.app.Model.SongLyrics;
import com.melodix.app.Repository.LyricsRepository;
import com.melodix.app.Repository.RepositoryCallback;

public class LyricsViewModel extends AndroidViewModel {

    private final LyricsRepository lyricsRepository;
    private final MutableLiveData<Resource<SongLyrics>> lyricsState =
            new MutableLiveData<>(Resource.<SongLyrics>idle());

    private String lastLoadedSongId = "";

    public LyricsViewModel(@NonNull Application application) {
        super(application);
        lyricsRepository = new LyricsRepository();
    }

    public LiveData<Resource<SongLyrics>> getLyricsState() {
        return lyricsState;
    }

    public void loadLyrics(String songId, boolean forceRefresh) {
        try {
            String safeSongId = songId == null ? "" : songId.trim();

            if (TextUtils.isEmpty(safeSongId)) {
                lastLoadedSongId = "";
                lyricsState.postValue(Resource.success(new SongLyrics()));
                return;
            }

            if (!forceRefresh && safeSongId.equals(lastLoadedSongId)) {
                return;
            }

            lastLoadedSongId = safeSongId;
            lyricsState.postValue(Resource.<SongLyrics>loading());

            lyricsRepository.loadLyrics(safeSongId, new RepositoryCallback<SongLyrics>() {
                @Override
                public void onSuccess(SongLyrics data) {
                    lyricsState.postValue(Resource.success(data == null ? new SongLyrics() : data));
                }

                @Override
                public void onError(String message) {
                    lyricsState.postValue(Resource.error(message));
                }
            });
        } catch (Exception e) {
            lyricsState.postValue(Resource.error("Không thể tải lời bài hát."));
        }
    }

    public void clearLyrics() {
        lastLoadedSongId = "";
        lyricsState.postValue(Resource.success(new SongLyrics()));
    }
}