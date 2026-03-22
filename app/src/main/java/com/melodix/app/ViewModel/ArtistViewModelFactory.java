package com.melodix.app.ViewModel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.melodix.app.Repository.ArtistRepository;

public class ArtistViewModelFactory implements ViewModelProvider.Factory {

    private final ArtistRepository artistRepository;
    private final String artistId;

    public ArtistViewModelFactory(ArtistRepository artistRepository, String artistId) {
        this.artistRepository = artistRepository;
        this.artistId = artistId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ArtistViewModel.class)) {
            return (T) new ArtistViewModel(artistRepository, artistId);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}