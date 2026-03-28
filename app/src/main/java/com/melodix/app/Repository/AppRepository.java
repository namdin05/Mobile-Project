package com.melodix.app.Repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.melodix.app.Data.MockDatabase;
import com.melodix.app.Model.Album;
import com.melodix.app.Model.AppUser;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.SearchResultItem;
import com.melodix.app.Model.Song;
import com.melodix.app.Utils.Constants;
import com.melodix.app.Utils.SessionManager;

import java.util.ArrayList;
import java.util.Locale;

public class AppRepository {
    private static final String KEY_STATE = "app_state_json";
    private static AppRepository instance;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final SessionManager sessionManager;
    private MockDatabase.DataState state;

    private AppRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(Constants.PREFS_DATA, Context.MODE_PRIVATE);
        this.gson = new GsonBuilder().create();
        this.sessionManager = new SessionManager(appContext);
        load();
    }

    public static synchronized AppRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AppRepository(context);
        }
        return instance;
    }

    private void load() {
        String json = prefs.getString(KEY_STATE, null);
        if (TextUtils.isEmpty(json)) {
            state = MockDatabase.createDefaultState(appContext);
            save();
        } else {
            try {
                state = gson.fromJson(json, MockDatabase.DataState.class);
            } catch (Exception e) {
                state = MockDatabase.createDefaultState(appContext);
                save();
            }
        }
    }
    public Album getAlbumById(String id) {
        for (Album album : state.albums) {
            if (album.id.equals(id)) return album;
        }
        return null;
    }
    public Artist getArtistById(String id) {
        for (Artist artist : state.artists) {
            if (artist.id.equals(id)) return artist;
        }
        return null;
    }
    private void save() {
        prefs.edit().putString(KEY_STATE, gson.toJson(state)).apply();
    }

    // =========================================================================
    // CÁC HÀM PHỤ THUỘC (CẦN THIẾT ĐỂ TÌM KIẾM VÀ LƯU LỊCH SỬ HOẠT ĐỘNG)
    // =========================================================================

    public AppUser getCurrentUser() {
        // Bỏ qua sessionManager, ép cứng app luôn nhận diện tài khoản test "user_listener"
        return getUserById("user_listener");
    }

    public AppUser getUserById(String id) {
        if (id == null) return null;
        for (AppUser user : state.users) {
            if (user.id.equals(id)) return user;
        }
        return null;
    }

    public ArrayList<Song> getAllApprovedSongs() {
        ArrayList<Song> list = new ArrayList<>();
        for (Song song : state.songs) {
            if (song.approved) list.add(song);
        }
        return filterByOfflineMode(list);
    }

    private ArrayList<Song> filterByOfflineMode(ArrayList<Song> source) {
        AppUser user = getCurrentUser();
        if (user == null || !user.offlineMode) return source;
        ArrayList<Song> filtered = new ArrayList<>();
        for (Song song : source) {
            if (user.downloadedSongIds.contains(song.id)) {
                filtered.add(song);
            }
        }
        return filtered;
    }

    public ArrayList<Playlist> getCurrentUserPlaylists() {
        AppUser user = getCurrentUser();
        ArrayList<Playlist> list = new ArrayList<>();
        if (user == null) return list;
        for (Playlist playlist : state.playlists) {
            if (user.id.equals(playlist.ownerUserId)) list.add(playlist);
        }
        return list;
    }

    // =========================================================================
    // NHÓM CHỨC NĂNG TÌM KIẾM NÂNG CAO & LỊCH SỬ TÌM KIẾM
    // =========================================================================

    public ArrayList<SearchResultItem> search(String keyword, String filter) {
        ArrayList<SearchResultItem> results = new ArrayList<>();
        String q = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return results;
        }

        // 1. Lưu lịch sử tìm kiếm gần đây
        AppUser user = getCurrentUser();
        if (user != null) {
            user.recentSearches.remove(keyword);
            user.recentSearches.add(0, keyword);
            while (user.recentSearches.size() > 8) {
                user.recentSearches.remove(user.recentSearches.size() - 1);
            }
            save();
        }

        // 2. Lọc kết quả tìm kiếm theo: Bài hát, Nghệ sĩ, Album, Playlist
        if (Constants.FILTER_ALL.equals(filter) || Constants.FILTER_SONG.equals(filter)) {
            for (Song song : getAllApprovedSongs()) {
                if (contains(song.title, q) || contains(song.artistName, q) || contains(song.genre, q) || contains(song.description, q)) {
                    results.add(new SearchResultItem(Constants.FILTER_SONG, song.id, song.title, song.artistName + " • " + song.genre, song.coverRes));
                }
            }
        }

        if (Constants.FILTER_ALL.equals(filter) || Constants.FILTER_ARTIST.equals(filter)) {
            for (Artist artist : state.artists) {
                if (contains(artist.name, q) || contains(artist.bio, q)) {
                    results.add(new SearchResultItem(Constants.FILTER_ARTIST, artist.id, artist.name, "Artist", artist.avatarRes));
                }
            }
        }

        if (Constants.FILTER_ALL.equals(filter) || Constants.FILTER_ALBUM.equals(filter)) {
            for (Album album : state.albums) {
                if (contains(album.title, q) || contains(album.artistName, q) || contains(album.genre, q) || contains(album.description, q)) {
                    results.add(new SearchResultItem(Constants.FILTER_ALBUM, album.id, album.title, album.artistName + " • Album", album.coverRes));
                }
            }
        }

        if (Constants.FILTER_ALL.equals(filter) || Constants.FILTER_PLAYLIST.equals(filter)) {
            for (Playlist playlist : getCurrentUserPlaylists()) {
                if (contains(playlist.name, q)) {
                    results.add(new SearchResultItem(Constants.FILTER_PLAYLIST, playlist.id, playlist.name, "Playlist", playlist.coverRes));
                }
            }
        }
        return results;
    }

    // Hàm hỗ trợ tìm kiếm Full-text bằng Keyword (Kiểm tra chuỗi con)
    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    // Lấy danh sách lịch sử tìm kiếm
    public ArrayList<String> getRecentSearches() {
        AppUser user = getCurrentUser();
        if (user == null) return new ArrayList<>();
        return new ArrayList<>(user.recentSearches);
    }

    // Xóa lịch sử tìm kiếm
    public void clearRecentSearches() {
        AppUser user = getCurrentUser();
        if (user != null) {
            user.recentSearches.clear();
            save();
        }
    }
}