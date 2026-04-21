package com.melodix.app.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.melodix.app.Model.Album;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.AlbumRepository;

import java.util.List;

public class AlbumViewModel extends AndroidViewModel {

    private AlbumRepository albumRepository;

    public AlbumViewModel(@NonNull Application application) {
        super(application);
        albumRepository = new AlbumRepository(application);
    }

    public LiveData<List<Album>> getAllAlbums() {
        return albumRepository.getAllAlbums();
    }

    public LiveData<List<Song>> getSongsByAlbumId(String id) {
        return albumRepository.getSongsByAlbumId(id);
    }

}
