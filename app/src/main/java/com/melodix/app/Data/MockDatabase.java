package com.melodix.app.Data;

import com.melodix.app.Model.Album;
import com.melodix.app.Model.AppUser;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.Song;
import com.melodix.app.Utils.Constants;
import com.melodix.app.Utils.SecurityUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class MockDatabase {

    public static class DataState {
        // Chỉ giữ lại các danh sách phục vụ cho tìm kiếm và lưu lịch sử
        public ArrayList<AppUser> users = new ArrayList<>();
        public ArrayList<Artist> artists = new ArrayList<>();
        public ArrayList<Album> albums = new ArrayList<>();
        public ArrayList<Song> songs = new ArrayList<>();
        public ArrayList<Playlist> playlists = new ArrayList<>();
    }

    public static DataState createDefaultState(android.content.Context context) {
        DataState state = new DataState();

        // 1. DỮ LIỆU USER (Cần thiết để lưu lịch sử tìm kiếm recentSearches)
        AppUser user = new AppUser("user_listener", "Alex Rivera", "alex", "user@melodix.app",
                SecurityUtils.sha256("123456"), Constants.ROLE_USER, "avatar_listener_1",
                "Good evening", "Music lover, playlist collector.");
        user.offlineMode = false;
        // Mock sẵn vài từ khóa lịch sử tìm kiếm
        user.recentSearches.addAll(Arrays.asList("Lo-fi Beats", "Alex Rivers", "Late Night"));
        state.users.add(user);

        // 2. DỮ LIỆU ARTIST (Phục vụ Filter ARTIST)
        Artist alex = new Artist("artist_alex", "user_artist_alex", "Alex Rivers", "avatar_artist_alex",
                "Alex Rivers blends synth-pop hooks with neon textures. Great for late night drives.",
                "banner_artist_hub");
        Artist luna = new Artist("artist_luna", "user_artist_luna", "Luna Ray", "avatar_artist_luna",
                "Luna Ray writes soft dream-pop.",
                "banner_new_release");
        state.artists.addAll(Arrays.asList(alex, luna));

        // 3. DỮ LIỆU ALBUM (Phục vụ Filter ALBUM)
        Album neonSkies = new Album("album_neon_skies","Neon Skies", alex.id, alex.name,
                "cover_neon_horizons", "Synth-pop", "A personal synth-pop statement.", 2024);
        Album starlightDreams = new Album("album_starlight_dreams","Starlight Dreams", luna.id, luna.name,
                "cover_starlight_serenade", "Dream Pop", "Soft lights and intimate refrains.", 2024);
        state.albums.addAll(Arrays.asList(neonSkies, starlightDreams));

        // 4. DỮ LIỆU SONG (Phục vụ Filter SONG)
        Song neonHorizons = song("song_neon_horizons", "Neon Horizons", alex, neonSkies,
                "cover_neon_horizons", "audio_neon_horizons", "Synth-pop",
                "Alex Rivers opens his album with cinematic momentum.");
        neonHorizons.approved = true;

        Song starlightSerenade = song("song_starlight_serenade", "Starlight Serenade", luna, starlightDreams,
                "cover_starlight_serenade", "audio_starlight_serenade", "Dream Pop",
                "Luna floats over warm pads.");
        starlightSerenade.approved = true;

        Song electricPulse = song("song_electric_pulse", "Electric Pulse", alex, neonSkies,
                "cover_electric_pulse", "audio_electric_pulse", "EDM",
                "A brighter festival-ready cut.");
        electricPulse.approved = true;

        state.songs.addAll(Arrays.asList(neonHorizons, starlightSerenade, electricPulse));

        // 5. DỮ LIỆU PLAYLIST (Phục vụ Filter PLAYLIST)
        Playlist lateNight = new Playlist("playlist_latenight", user.id, "Late Night Vibes", "cover_playlist_latenight", false);
        Playlist summer = new Playlist("playlist_summer", user.id, "Summer Festival 2024", "cover_playlist_summer", false);

        user.playlistIds.addAll(Arrays.asList(lateNight.id, summer.id));
        state.playlists.addAll(Arrays.asList(lateNight, summer));

        return state;
    }

    // Hàm Helper tạo Song nhanh gọn (Đã bỏ bớt lyrics vì tìm kiếm không quét qua lyrics)
    private static Song song(String id, String title, Artist artist, Album album, String coverRes,
                             String audioRes, String genre, String description) {
        return new Song(id, title, artist.id, artist.name, album.id, album.title, coverRes, audioRes, genre, description, 200, 0, 0);
    }
}