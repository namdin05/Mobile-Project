package com.melodix.app.Repository;

import com.melodix.app.Data.MockDataStore;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ArtistRepository {

    public Artist getArtistById(String artistId) {
        return MockDataStore.getArtistById(artistId);
    }

    public List<Song> getPopularSongsByArtistId(String artistId) {
        List<Song> songs = new ArrayList<>(MockDataStore.getSongsByArtistId(artistId));

        Collections.sort(songs, new Comparator<Song>() {
            @Override
            public int compare(Song left, Song right) {
                if (left.isFavorite() != right.isFavorite()) {
                    return left.isFavorite() ? -1 : 1;
                }

                int durationCompare = Long.compare(right.getDurationMs(), left.getDurationMs());
                if (durationCompare != 0) {
                    return durationCompare;
                }

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