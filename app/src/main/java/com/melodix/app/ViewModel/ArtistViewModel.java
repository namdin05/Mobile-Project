package com.melodix.app.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.melodix.app.Data.MockDataStore;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.Song;
import com.melodix.app.Model.Album;
import com.melodix.app.Repository.ArtistRepository;

import java.util.ArrayList;
import java.util.List;

public class ArtistViewModel extends ViewModel {

    private final ArtistRepository artistRepository;
    private final String artistId;

    private final MutableLiveData<Artist> artistLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> popularSongsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Album>> albumsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Artist>> relatedArtistsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> notFoundLiveData = new MutableLiveData<>(false);

    public ArtistViewModel(ArtistRepository artistRepository, String artistId) {
        this.artistRepository = artistRepository;
        this.artistId = artistId == null ? "" : artistId.trim();

        popularSongsLiveData.setValue(new ArrayList<Song>());
        albumsLiveData.setValue(new ArrayList<Album>());
        relatedArtistsLiveData.setValue(new ArrayList<Artist>());
        loadArtistDetail();
    }

    public LiveData<Artist> getArtistLiveData() { return artistLiveData; }
    public LiveData<List<Song>> getPopularSongsLiveData() { return popularSongsLiveData; }
    public LiveData<List<Album>> getAlbumsLiveData() { return albumsLiveData; }
    public LiveData<List<Artist>> getRelatedArtistsLiveData() { return relatedArtistsLiveData; }
    public LiveData<Boolean> getNotFoundLiveData() { return notFoundLiveData; }

    public void refresh() {
        loadArtistDetail();
    }

    private void loadArtistDetail() {
        if (artistId.isEmpty()) {
            artistLiveData.setValue(null);
            popularSongsLiveData.setValue(new ArrayList<Song>());
            albumsLiveData.setValue(new ArrayList<Album>());
            relatedArtistsLiveData.setValue(new ArrayList<Artist>());
            notFoundLiveData.setValue(true);
            return;
        }

        Artist artist = artistRepository.getArtistById(artistId);
        List<Song> songs = artistRepository.getPopularSongsByArtistId(artistId);

        // Lấy thêm Album và Nghệ sĩ tương tự từ MockDataStore
        List<Album> albums = MockDataStore.getAlbumsByArtistId(artistId);
        List<Artist> related = MockDataStore.getRelatedArtists(artistId);

        artistLiveData.setValue(artist);
        popularSongsLiveData.setValue(songs == null ? new ArrayList<Song>() : songs);
        albumsLiveData.setValue(albums == null ? new ArrayList<Album>() : albums);
        relatedArtistsLiveData.setValue(related == null ? new ArrayList<Artist>() : related);

        notFoundLiveData.setValue(artist == null);
    }
}