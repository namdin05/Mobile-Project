package com.melodix.app.Repository;

import com.melodix.app.Data.MockDataStore;
import com.melodix.app.Model.Album;
import com.melodix.app.Model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AlbumRepository {

    public Album getAlbumById(String albumId) {
        return MockDataStore.getAlbumById(albumId);
    }

    public List<Song> getSongsByAlbumId(String albumId) {
        List<Song> songs = new ArrayList<>(MockDataStore.getSongsByAlbumId(albumId));

        Collections.sort(songs, new Comparator<Song>() {
            @Override
            public int compare(Song left, Song right) {
                int leftTrack = left.getTrackNumber() > 0 ? left.getTrackNumber() : Integer.MAX_VALUE;
                int rightTrack = right.getTrackNumber() > 0 ? right.getTrackNumber() : Integer.MAX_VALUE;

                int trackCompare = Integer.compare(leftTrack, rightTrack);
                if (trackCompare != 0) {
                    return trackCompare;
                }

                return safe(left.getTitle()).compareToIgnoreCase(safe(right.getTitle()));
            }
        });

        return songs;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}