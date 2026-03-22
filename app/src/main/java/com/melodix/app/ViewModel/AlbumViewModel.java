package com.melodix.app.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.melodix.app.Model.Album;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.AlbumRepository;

import java.util.ArrayList;
import java.util.List;

public class AlbumViewModel extends ViewModel {

    private final AlbumRepository albumRepository;
    private final String albumId;

    private final MutableLiveData<Album> albumLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> albumSongsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> notFoundLiveData = new MutableLiveData<>(false);

    public AlbumViewModel(AlbumRepository albumRepository, String albumId) {
        this.albumRepository = albumRepository;
        this.albumId = albumId == null ? "" : albumId.trim();

        albumSongsLiveData.setValue(new ArrayList<Song>());
        loadAlbumDetail();
    }

    public LiveData<Album> getAlbumLiveData() {
        return albumLiveData;
    }

    public LiveData<List<Song>> getAlbumSongsLiveData() {
        return albumSongsLiveData;
    }

    public LiveData<Boolean> getNotFoundLiveData() {
        return notFoundLiveData;
    }

    public void refresh() {
        loadAlbumDetail();
    }

    private void loadAlbumDetail() {
        if (albumId.isEmpty()) {
            albumLiveData.setValue(null);
            albumSongsLiveData.setValue(new ArrayList<Song>());
            notFoundLiveData.setValue(true);
            return;
        }

        Album album = albumRepository.getAlbumById(albumId);
        List<Song> songs = albumRepository.getSongsByAlbumId(albumId);

        albumLiveData.setValue(album);
        albumSongsLiveData.setValue(songs == null ? new ArrayList<Song>() : songs);
        notFoundLiveData.setValue(album == null);
    }
}