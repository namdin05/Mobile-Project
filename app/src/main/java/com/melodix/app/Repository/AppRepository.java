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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.melodix.app.Network.SupabaseClient;
import com.melodix.app.Network.SupabaseApi;
import java.util.List;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class AppRepository {
    private static final String KEY_STATE = "app_state_json";
    private static AppRepository instance;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final SessionManager sessionManager;
    private MockDatabase.DataState state;
    private final SupabaseApi supabaseApi; // THÊM DÒNG NÀY

    private AppRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(Constants.PREFS_DATA, Context.MODE_PRIVATE);
        this.gson = new GsonBuilder().create();
        this.sessionManager = new SessionManager(appContext);
        load();
        this.supabaseApi = SupabaseClient.getClient().create(SupabaseApi.class);
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

    // HÀM SEARCH PHIÊN BẢN MẠNG THẬT
    // =========================================================================
    // HÀM SEARCH FULL-TEXT (TÌM BÀI HÁT, NGHỆ SĨ, ALBUM, PLAYLIST SONG SONG)
    // =========================================================================
    public void search(String keyword, String filter, SearchCallback callback) {
        ArrayList<SearchResultItem> results = new ArrayList<>();
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) {
            callback.onSuccess(results);
            return;
        }

        // 1. Cập nhật lịch sử tìm kiếm
        AppUser user = getCurrentUser();
        if (user != null) {
            user.recentSearches.remove(keyword);
            user.recentSearches.add(0, keyword);
            while (user.recentSearches.size() > 8) {
                user.recentSearches.remove(user.recentSearches.size() - 1);
            }
            save();
        }

        // 2. Cú pháp FTS của PostgREST Supabase
        String ftsQuery = "wfts." + q;

        // 3. Chuẩn bị biến đếm để gọi 4 luồng mạng song song
        boolean searchSongs = Constants.FILTER_ALL.equals(filter) || Constants.FILTER_SONG.equals(filter);
        boolean searchArtists = Constants.FILTER_ALL.equals(filter) || Constants.FILTER_ARTIST.equals(filter);
        boolean searchAlbums = Constants.FILTER_ALL.equals(filter) || Constants.FILTER_ALBUM.equals(filter);
        boolean searchPlaylists = Constants.FILTER_ALL.equals(filter) || Constants.FILTER_PLAYLIST.equals(filter);

        int totalRequests = 0;
        if (searchSongs) totalRequests++;
        if (searchArtists) totalRequests++;
        if (searchAlbums) totalRequests++;
        if (searchPlaylists) totalRequests++;

        if (totalRequests == 0) {
            callback.onSuccess(results);
            return;
        }

        // AtomicInteger giúp đếm lùi số lượng request một cách an toàn
        AtomicInteger pendingRequests = new AtomicInteger(totalRequests);

        // Dùng danh sách đồng bộ để không bị lỗi khi 4 kết quả cùng ùa về 1 lúc
        List<SearchResultItem> syncResults = Collections.synchronizedList(new ArrayList<>());

        // Lệnh kiểm tra: Nếu đếm ngược về 0 (tất cả API đã trả kết quả về), thì xuất lên màn hình
        Runnable checkCompletion = () -> {
            if (pendingRequests.decrementAndGet() == 0) {
                callback.onSuccess(new ArrayList<>(syncResults));
            }
        };

        // 4. BẮT ĐẦU GỌI MẠNG ĐỒNG LOẠT
        // TÌM BÀI HÁT
        if (searchSongs) {
            supabaseApi.searchSongs(ftsQuery).enqueue(new Callback<List<Song>>() {
                @Override public void onResponse(Call<List<Song>> call, retrofit2.Response<List<Song>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Song song : response.body()) {
                            syncResults.add(new SearchResultItem(Constants.FILTER_SONG, song.id, song.title, "Bài hát", song.coverRes));
                        }
                    }
                    checkCompletion.run(); // Báo cáo: "Tôi tìm xong bài hát rồi nhé!"
                }
                @Override public void onFailure(Call<List<Song>> call, Throwable t) { checkCompletion.run(); }
            });
        }

        // TÌM NGHỆ SĨ
        if (searchArtists) {
            supabaseApi.searchArtists(ftsQuery).enqueue(new Callback<List<Artist>>() {
                @Override public void onResponse(Call<List<Artist>> call, retrofit2.Response<List<Artist>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Artist artist : response.body()) {
                            syncResults.add(new SearchResultItem(Constants.FILTER_ARTIST, artist.id, artist.name, "Nghệ sĩ", artist.avatarRes));
                        }
                    }
                    checkCompletion.run();
                }
                @Override public void onFailure(Call<List<Artist>> call, Throwable t) { checkCompletion.run(); }
            });
        }

        // TÌM ALBUM
        if (searchAlbums) {
            supabaseApi.searchAlbums(ftsQuery).enqueue(new Callback<List<Album>>() {
                @Override public void onResponse(Call<List<Album>> call, retrofit2.Response<List<Album>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Album album : response.body()) {
                            syncResults.add(new SearchResultItem(Constants.FILTER_ALBUM, album.id, album.title, "Album", album.coverRes));
                        }
                    }
                    checkCompletion.run();
                }
                @Override public void onFailure(Call<List<Album>> call, Throwable t) { checkCompletion.run(); }
            });
        }

        // TÌM PLAYLIST
        if (searchPlaylists) {
            supabaseApi.searchPlaylists(ftsQuery).enqueue(new Callback<List<Playlist>>() {
                @Override public void onResponse(Call<List<Playlist>> call, retrofit2.Response<List<Playlist>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Playlist playlist : response.body()) {
                            syncResults.add(new SearchResultItem(Constants.FILTER_PLAYLIST, playlist.id, playlist.name, "Playlist", playlist.coverRes));
                        }
                    }
                    checkCompletion.run();
                }
                @Override public void onFailure(Call<List<Playlist>> call, Throwable t) { checkCompletion.run(); }
            });
        }
    }
    public interface SearchCallback {
        void onSuccess(ArrayList<SearchResultItem> results);
        void onError(String message);
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