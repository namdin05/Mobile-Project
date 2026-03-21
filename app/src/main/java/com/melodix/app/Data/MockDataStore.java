package com.melodix.app.Data;

import com.melodix.app.Model.Album;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.Song;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class MockDataStore {

    private static final List<String> GENRES = Collections.unmodifiableList(
            Arrays.asList(
                    "Synthwave",
                    "Electronic",
                    "Indie Pop",
                    "Alternative",
                    "Ambient",
                    "Chill"
            )
    );

    private static final List<Artist> ARTISTS = Collections.unmodifiableList(createArtists());
    private static final List<Album> ALBUMS = Collections.unmodifiableList(createAlbums());
    private static final List<Song> SONGS = Collections.unmodifiableList(createSongs());

    private MockDataStore() {
        // Utility class
    }

    public static List<String> getGenres() {
        return new ArrayList<>(GENRES);
    }

    public static List<Artist> getArtists() {
        return new ArrayList<>(ARTISTS);
    }

    public static List<Album> getAlbums() {
        return new ArrayList<>(ALBUMS);
    }

    public static List<Song> getSongs() {
        return new ArrayList<>(SONGS);
    }

    public static Artist getArtistById(String artistId) {
        for (Artist artist : ARTISTS) {
            if (artist.getId().equals(artistId)) {
                return artist;
            }
        }
        return null;
    }

    public static Album getAlbumById(String albumId) {
        for (Album album : ALBUMS) {
            if (album.getId().equals(albumId)) {
                return album;
            }
        }
        return null;
    }

    public static Song getSongById(String songId) {
        for (Song song : SONGS) {
            if (song.getId().equals(songId)) {
                return song;
            }
        }
        return null;
    }

    public static List<Album> getAlbumsByArtistId(String artistId) {
        List<Album> results = new ArrayList<>();
        for (Album album : ALBUMS) {
            if (album.getArtistId().equals(artistId)) {
                results.add(album);
            }
        }
        return results;
    }

    public static List<Song> getSongsByArtistId(String artistId) {
        List<Song> results = new ArrayList<>();
        for (Song song : SONGS) {
            if (song.getArtistId().equals(artistId)) {
                results.add(song);
            }
        }
        return results;
    }

    public static List<Song> getSongsByAlbumId(String albumId) {
        List<Song> results = new ArrayList<>();
        for (Song song : SONGS) {
            if (song.getAlbumId().equals(albumId)) {
                results.add(song);
            }
        }
        return results;
    }

    public static List<Song> searchSongs(String keyword) {
        if (isBlank(keyword)) {
            return getSongs();
        }

        String query = keyword.trim().toLowerCase(Locale.ROOT);
        List<Song> results = new ArrayList<>();

        for (Song song : SONGS) {
            if (contains(song.getTitle(), query)
                    || contains(song.getArtistName(), query)
                    || contains(song.getAlbumTitle(), query)
                    || contains(song.getGenre(), query)) {
                results.add(song);
            }
        }
        return results;
    }

    public static List<Artist> searchArtists(String keyword) {
        if (isBlank(keyword)) {
            return getArtists();
        }

        String query = keyword.trim().toLowerCase(Locale.ROOT);
        List<Artist> results = new ArrayList<>();

        for (Artist artist : ARTISTS) {
            if (contains(artist.getName(), query)
                    || contains(artist.getGenre(), query)
                    || contains(artist.getBio(), query)) {
                results.add(artist);
            }
        }
        return results;
    }

    public static List<Album> searchAlbums(String keyword) {
        if (isBlank(keyword)) {
            return getAlbums();
        }

        String query = keyword.trim().toLowerCase(Locale.ROOT);
        List<Album> results = new ArrayList<>();

        for (Album album : ALBUMS) {
            if (contains(album.getTitle(), query)
                    || contains(album.getArtistName(), query)
                    || contains(album.getGenre(), query)
                    || contains(album.getDescription(), query)) {
                results.add(album);
            }
        }
        return results;
    }

    private static boolean contains(String source, String query) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(query);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static List<Artist> createArtists() {
        List<Artist> list = new ArrayList<>();

        list.add(new Artist(
                "art_001",
                "Nova Lane",
                "https://picsum.photos/seed/nova_lane_avatar/600/600",
                "https://picsum.photos/seed/nova_lane_header/1400/900",
                "Synthwave",
                "Nova Lane blends retro synth textures, neon bass lines and late-night city energy into polished electronic records.",
                1240000L,
                584000L,
                2,
                4,
                true
        ));

        list.add(new Artist(
                "art_002",
                "Aster Bloom",
                "https://picsum.photos/seed/aster_bloom_avatar/600/600",
                "https://picsum.photos/seed/aster_bloom_header/1400/900",
                "Indie Pop",
                "Aster Bloom writes soft indie-pop songs with airy vocals, intimate lyrics and warm analog textures.",
                860000L,
                321000L,
                1,
                2,
                true
        ));

        list.add(new Artist(
                "art_003",
                "Midnight Echo",
                "https://picsum.photos/seed/midnight_echo_avatar/600/600",
                "https://picsum.photos/seed/midnight_echo_header/1400/900",
                "Alternative",
                "Midnight Echo mixes alternative guitars, dark urban atmospheres and cinematic hooks made for night drives.",
                640000L,
                207000L,
                1,
                2,
                false
        ));

        list.add(new Artist(
                "art_004",
                "Solstice Waves",
                "https://picsum.photos/seed/solstice_waves_avatar/600/600",
                "https://picsum.photos/seed/solstice_waves_header/1400/900",
                "Ambient",
                "Solstice Waves focuses on ambient, chill and ocean-inspired soundscapes designed for focus and relaxation.",
                430000L,
                156000L,
                1,
                2,
                false
        ));

        return list;
    }

    private static List<Album> createAlbums() {
        List<Album> list = new ArrayList<>();

        list.add(new Album(
                "alb_001",
                "Neon Skyline",
                "art_001",
                "Nova Lane",
                "https://picsum.photos/seed/neon_skyline_cover/800/800",
                "https://picsum.photos/seed/neon_skyline_header/1400/900",
                "Synthwave",
                "2025-01-18",
                2,
                186000L,
                "A cinematic night-drive record filled with bright arps, soft pads and deep bass movement.",
                false
        ));

        list.add(new Album(
                "alb_002",
                "Afterglow Drive",
                "art_001",
                "Nova Lane",
                "https://picsum.photos/seed/afterglow_drive_cover/800/800",
                "https://picsum.photos/seed/afterglow_drive_header/1400/900",
                "Electronic",
                "2025-06-07",
                2,
                57000L,
                "A shorter, punchier electronic release built around glossy hooks and late-night momentum.",
                false
        ));

        list.add(new Album(
                "alb_003",
                "Petals After Rain",
                "art_002",
                "Aster Bloom",
                "https://picsum.photos/seed/petals_after_rain_cover/800/800",
                "https://picsum.photos/seed/petals_after_rain_header/1400/900",
                "Indie Pop",
                "2024-11-12",
                2,
                15000L,
                "A delicate indie-pop release with intimate songwriting and soft textures.",
                false
        ));

        list.add(new Album(
                "alb_004",
                "City After Midnight",
                "art_003",
                "Midnight Echo",
                "https://picsum.photos/seed/city_after_midnight_cover/800/800",
                "https://picsum.photos/seed/city_after_midnight_header/1400/900",
                "Alternative",
                "2024-09-27",
                2,
                27000L,
                "Dark city moods, tight guitars and a nocturnal alternative edge.",
                false
        ));

        list.add(new Album(
                "alb_005",
                "Tidal Dreams",
                "art_004",
                "Solstice Waves",
                "https://picsum.photos/seed/tidal_dreams_cover/800/800",
                "https://picsum.photos/seed/tidal_dreams_header/1400/900",
                "Ambient",
                "2025-02-21",
                2,
                18000L,
                "A minimal ambient collection designed for calm listening, reading and focus.",
                false
        ));

        return list;
    }

    private static List<Song> createSongs() {
        List<Song> list = new ArrayList<>();

        list.add(new Song(
                "song_001",
                "Midnight Drive",
                "art_001",
                "Nova Lane",
                "alb_001",
                "Neon Skyline",
                "Synthwave",
                "https://picsum.photos/seed/neon_skyline_cover/800/800",
                "https://getsamplefiles.com/download/mp3/sample-1.mp3",
                96000L,
                1,
                false,
                true
        ));

        list.add(new Song(
                "song_002",
                "Neon Pulse",
                "art_001",
                "Nova Lane",
                "alb_001",
                "Neon Skyline",
                "Synthwave",
                "https://picsum.photos/seed/neon_skyline_cover/800/800",
                "https://getsamplefiles.com/download/mp3/sample-2.mp3",
                90000L,
                2,
                false,
                false
        ));

        list.add(new Song(
                "song_003",
                "Afterglow",
                "art_001",
                "Nova Lane",
                "alb_002",
                "Afterglow Drive",
                "Electronic",
                "https://picsum.photos/seed/afterglow_drive_cover/800/800",
                "https://getsamplefiles.com/download/mp3/sample-5.mp3",
                45000L,
                1,
                false,
                true
        ));

        list.add(new Song(
                "song_004",
                "Static Hearts",
                "art_001",
                "Nova Lane",
                "alb_002",
                "Afterglow Drive",
                "Electronic",
                "https://picsum.photos/seed/afterglow_drive_cover/800/800",
                "https://download.samplelib.com/mp3/sample-12s.mp3",
                12000L,
                2,
                false,
                false
        ));

        list.add(new Song(
                "song_005",
                "Blue Petals",
                "art_002",
                "Aster Bloom",
                "alb_003",
                "Petals After Rain",
                "Indie Pop",
                "https://picsum.photos/seed/petals_after_rain_cover/800/800",
                "https://download.samplelib.com/mp3/sample-6s.mp3",
                6000L,
                1,
                false,
                false
        ));

        list.add(new Song(
                "song_006",
                "Rainlight",
                "art_002",
                "Aster Bloom",
                "alb_003",
                "Petals After Rain",
                "Indie Pop",
                "https://picsum.photos/seed/petals_after_rain_cover/800/800",
                "https://download.samplelib.com/mp3/sample-9s.mp3",
                9000L,
                2,
                false,
                true
        ));

        list.add(new Song(
                "song_007",
                "City After Midnight",
                "art_003",
                "Midnight Echo",
                "alb_004",
                "City After Midnight",
                "Alternative",
                "https://picsum.photos/seed/city_after_midnight_cover/800/800",
                "https://getsamplefiles.com/download/mp3/sample-4.mp3",
                14000L,
                1,
                true,
                false
        ));

        list.add(new Song(
                "song_008",
                "Concrete Stars",
                "art_003",
                "Midnight Echo",
                "alb_004",
                "City After Midnight",
                "Alternative",
                "https://picsum.photos/seed/city_after_midnight_cover/800/800",
                "https://getsamplefiles.com/download/mp3/sample-3.mp3",
                13000L,
                2,
                false,
                false
        ));

        list.add(new Song(
                "song_009",
                "Tidal Dreams",
                "art_004",
                "Solstice Waves",
                "alb_005",
                "Tidal Dreams",
                "Ambient",
                "https://picsum.photos/seed/tidal_dreams_cover/800/800",
                "https://download.samplelib.com/mp3/sample-15s.mp3",
                15000L,
                1,
                false,
                true
        ));

        list.add(new Song(
                "song_010",
                "Drift Theory",
                "art_004",
                "Solstice Waves",
                "alb_005",
                "Tidal Dreams",
                "Chill",
                "https://picsum.photos/seed/tidal_dreams_cover/800/800",
                "https://download.samplelib.com/mp3/sample-3s.mp3",
                3000L,
                2,
                false,
                false
        ));

        return list;
    }
}