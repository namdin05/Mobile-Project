package com.melodix.app.ViewModel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.melodix.app.Repository.AlbumRepository;

public class AlbumViewModelFactory implements ViewModelProvider.Factory {

    private final AlbumRepository albumRepository;
    private final String albumId;

    public AlbumViewModelFactory(AlbumRepository albumRepository, String albumId) {
        this.albumRepository = albumRepository;
        this.albumId = albumId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AlbumViewModel.class)) {
            return (T) new AlbumViewModel(albumRepository, albumId);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}