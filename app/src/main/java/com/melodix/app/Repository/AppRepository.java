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
    private final SupabaseApi supabaseApi;

    // Giữ lại bộ đếm thời gian để chống giật lag khi gõ phím
    private long lastSearchTime = 0;

    private AppRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(Constants.PREFS_DATA, Context.MODE_PRIVATE);
        this.gson = new GsonBuilder().create();
        this.sessionManager = new SessionManager(appContext);
        load();
        this.supabaseApi = SupabaseClient.getClient().create(SupabaseApi.class);
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

    public void getAlbumById(String id, AlbumCallback callback) {
        supabaseApi.getAlbumById("eq." + id).enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, retrofit2.Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    callback.onSuccess(response.body().get(0));
                } else {
                    callback.onError("Không tìm thấy dữ liệu Album! Mã lỗi: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<List<Album>> call, Throwable t) {
                callback.onError("Lỗi kết nối mạng: " + t.getMessage());
            }
        });
    }

    public Artist getArtistById(String id) {
        for (Artist artist : state.artists) {
            if (artist.id.equals(id)) return artist;
        }
        return null;
    }

    public interface SongListCallback {
        void onSuccess(ArrayList<Song> songs);
        void onError(String message);
    }

    public void getSongsByAlbum(String albumId, SongListCallback callback) {
        supabaseApi.getSongsByAlbumId("eq." + albumId).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, retrofit2.Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(new ArrayList<>(response.body()));
                } else {
                    callback.onError("Không tải được danh sách bài hát.");
                }
            }
            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                callback.onError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    private void save() {
        prefs.edit().putString(KEY_STATE, gson.toJson(state)).apply();
    }

    public AppUser getCurrentUser() {
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
            if (song.status == "approved") list.add(song);
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
    // HÀM LƯU LỊCH SỬ (Đã được tách riêng ra để phục vụ SearchFragment)
    // =========================================================================
    public void saveToRecentSearch(String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) return;
        AppUser user = getCurrentUser();
        if (user != null) {
            user.recentSearches.remove(q);
            user.recentSearches.add(0, q);
            if (user.recentSearches.size() > 8) user.recentSearches.remove(user.recentSearches.size() - 1);
            save();
        }
    }

    // =========================================================================
    // HÀM SEARCH FULL-TEXT (CÚ PHÁP CHUẨN KẾT HỢP DEBOUNCE)
    // =========================================================================
    public void search(String keyword, String filter, SearchCallback callback) {
        ArrayList<SearchResultItem> results = new ArrayList<>();
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) {
            callback.onSuccess(results);
            return;
        }

        // Đánh dấu thời gian bắt đầu gọi mạng để chống lag
        // Đánh dấu thời gian bắt đầu gọi mạng để chống lag
        final long currentRequestTime = System.currentTimeMillis();
        lastSearchTime = currentRequestTime;

        // =======================================================
        // TÍNH NĂNG MỚI: TÌM KIẾM TIỀN TỐ (GÕ 1 CHỮ CŨNG RA)
        // =======================================================
        // 1. Biến đổi: "nhạc t" -> "nhạc & t:*"
        String formattedKeyword = q.replaceAll("\\s+", " & ") + ":*";

        // 2. Mã hóa chữ tiếng Việt & các ký tự đặc biệt để an toàn truyền đi
        String encodedKeyword = android.net.Uri.encode(formattedKeyword);

        // 3. Đổi wfts thành fts nguyên bản để hỗ trợ dấu :*
        String ftsQuery = "fts(simple)." + encodedKeyword;
        // =======================================================


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

        AtomicInteger pendingRequests = new AtomicInteger(totalRequests);
        List<SearchResultItem> syncResults = Collections.synchronizedList(new ArrayList<>());

        Runnable checkCompletion = () -> {
            if (pendingRequests.decrementAndGet() == 0) {
                // Chỉ trả kết quả về giao diện nếu đây là lần gõ phím mới nhất
                if (currentRequestTime == lastSearchTime) {
                    callback.onSuccess(new ArrayList<>(syncResults));
                }
            }
        };

        if (searchSongs) {
            supabaseApi.searchSongs(ftsQuery).enqueue(new Callback<List<Song>>() {
                @Override public void onResponse(Call<List<Song>> call, retrofit2.Response<List<Song>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Song song : response.body()) syncResults.add(new SearchResultItem(Constants.FILTER_SONG, song.id, song.title, "Bài hát", song.coverRes));
                    }
                    checkCompletion.run();
                }
                @Override public void onFailure(Call<List<Song>> call, Throwable t) { checkCompletion.run(); }
            });
        }

        if (searchArtists) {
            supabaseApi.searchArtists(ftsQuery).enqueue(new Callback<List<Artist>>() {
                @Override public void onResponse(Call<List<Artist>> call, retrofit2.Response<List<Artist>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Artist artist : response.body()) syncResults.add(new SearchResultItem(Constants.FILTER_ARTIST, artist.id, artist.name, "Nghệ sĩ", artist.avatarRes));
                    }
                    checkCompletion.run();
                }
                @Override public void onFailure(Call<List<Artist>> call, Throwable t) { checkCompletion.run(); }
            });
        }

        if (searchAlbums) {
            supabaseApi.searchAlbums(ftsQuery).enqueue(new Callback<List<Album>>() {
                @Override public void onResponse(Call<List<Album>> call, retrofit2.Response<List<Album>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Album album : response.body()) syncResults.add(new SearchResultItem(Constants.FILTER_ALBUM, album.id, album.title, "Album", album.coverRes));
                    }
                    checkCompletion.run();
                }
                @Override public void onFailure(Call<List<Album>> call, Throwable t) { checkCompletion.run(); }
            });
        }

        if (searchPlaylists) {
            supabaseApi.searchPlaylists(ftsQuery).enqueue(new Callback<List<Playlist>>() {
                @Override public void onResponse(Call<List<Playlist>> call, retrofit2.Response<List<Playlist>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Playlist playlist : response.body()) syncResults.add(new SearchResultItem(Constants.FILTER_PLAYLIST, playlist.id, playlist.name, "Playlist", playlist.coverRes));
                    }
                    checkCompletion.run();
                }
                @Override public void onFailure(Call<List<Playlist>> call, Throwable t) { checkCompletion.run(); }
            });
        }
    }
    public interface ArtistCallback {
        void onSuccess(Artist artist);
        void onError(String message);
    }

    public void getArtistByIdAsync(String id, ArtistCallback callback) {
        supabaseApi.getArtistByIdAPI("eq." + id).enqueue(new Callback<List<Artist>>() {
            @Override
            public void onResponse(Call<List<Artist>> call, Response<List<Artist>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    callback.onSuccess(response.body().get(0));
                } else {
                    callback.onError("Không tìm thấy dữ liệu Nghệ sĩ!");
                }
            }
            @Override
            public void onFailure(Call<List<Artist>> call, Throwable t) {
                callback.onError("Lỗi kết nối mạng: " + t.getMessage());
            }
        });
    }
    public interface SearchCallback {
        void onSuccess(ArrayList<SearchResultItem> results);
        void onError(String message);
    }
    public interface AlbumCallback {
        void onSuccess(Album album);
        void onError(String message);
    }
    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    public ArrayList<String> getRecentSearches() {
        AppUser user = getCurrentUser();
        if (user == null) return new ArrayList<>();
        return new ArrayList<>(user.recentSearches);
    }

    public void clearRecentSearches() {
        AppUser user = getCurrentUser();
        if (user != null) {
            user.recentSearches.clear();
            save();
        }
    }

    public void removeRecentSearch(String keyword) {
        AppUser user = getCurrentUser();
        if (user != null && keyword != null) {
            user.recentSearches.remove(keyword);
            save();
        }
    }
    public interface AlbumListCallback {
        void onSuccess(ArrayList<Album> albums);
        void onError(String message);
    }

    public interface ArtistListCallback {
        void onSuccess(ArrayList<Artist> artists);
        void onError(String message);
    }

    // 1. Lấy Bài hát
    public void getSongsByArtist(String artistId, SongListCallback callback) {
        supabaseApi.getSongsByArtistId("eq." + artistId).enqueue(new Callback<List<Song>>() {
            @Override public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(new ArrayList<>(response.body()));
                else callback.onError("Lỗi tải bài hát (Code: " + response.code() + ")");
            }
            @Override public void onFailure(Call<List<Song>> call, Throwable t) { callback.onError("Lỗi mạng"); }
        });
    }

    // 2. Lấy Album
    public void getAlbumsByArtist(String artistId, AlbumListCallback callback) {
        supabaseApi.getAlbumsByArtistId("eq." + artistId).enqueue(new Callback<List<Album>>() {
            @Override public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(new ArrayList<>(response.body()));
                else callback.onError("Lỗi tải Album");
            }
            @Override public void onFailure(Call<List<Album>> call, Throwable t) { callback.onError("Lỗi mạng"); }
        });
    }

    // 3. Lấy Nghệ sĩ liên quan (Dùng toán tử "neq." = Not Equal)
    public void getRelatedArtists(String currentArtistId, ArtistListCallback callback) {
        supabaseApi.getRelatedArtists("neq." + currentArtistId, 5).enqueue(new Callback<List<Artist>>() {
            @Override public void onResponse(Call<List<Artist>> call, Response<List<Artist>> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(new ArrayList<>(response.body()));
                else callback.onError("Lỗi tải Nghệ sĩ liên quan");
            }
            @Override public void onFailure(Call<List<Artist>> call, Throwable t) { callback.onError("Lỗi mạng"); }
        });
    }

    // =========================================================================
    // QUẢN LÝ HÀNG ĐỢI PHÁT NHẠC (QUEUE) - DÀNH CHO NHẠC TỪ SUPABASE
    // =========================================================================
    private ArrayList<Song> currentQueue = new ArrayList<>();
    private int currentQueueIndex = -1;

    public void setCurrentQueue(ArrayList<Song> queue, String startSongId) {
        this.currentQueue = new ArrayList<>(queue != null ? queue : new ArrayList<>());
        this.currentQueueIndex = 0;
        // Tìm vị trí bài hát người dùng vừa bấm vào
        for (int i = 0; i < currentQueue.size(); i++) {
            if (currentQueue.get(i).id.equals(startSongId)) {
                this.currentQueueIndex = i;
                break;
            }
        }
    }

    public ArrayList<String> getCurrentQueueSongIds() {
        ArrayList<String> ids = new ArrayList<>();
        if (currentQueue != null) {
            for (Song s : currentQueue) ids.add(s.id);
        }
        return ids;
    }

    public void setCurrentQueueIndex(int index) {
        if (currentQueue != null && index >= 0 && index < currentQueue.size()) {
            this.currentQueueIndex = index;
        }
    }

    public Song getCurrentQueueSong() {
        if (currentQueue != null && currentQueueIndex >= 0 && currentQueueIndex < currentQueue.size()) {
            return currentQueue.get(currentQueueIndex);
        }
        return null;
    }

    public Song moveNextInQueue() {
        if (currentQueue == null || currentQueue.isEmpty()) return null;
        if (currentQueueIndex < currentQueue.size() - 1) {
            currentQueueIndex++;
            return currentQueue.get(currentQueueIndex);
        }
        return null; // Tạm thời dừng khi hết danh sách
    }

    public Song movePreviousInQueue() {
        if (currentQueue == null || currentQueue.isEmpty()) return null;
        if (currentQueueIndex > 0) {
            currentQueueIndex--;
            return currentQueue.get(currentQueueIndex);
        }
        return null;
    }

    // =========================================================================
    // TÌM KIẾM BÀI HÁT TỪ QUEUE ĐỂ PHÁT
    // =========================================================================
    public Song getSongById(String id) {
        if (id == null) return null;

        // 1. Ưu tiên quét trong Hàng đợi hiện tại (Vì đây là nhạc load từ Supabase về)
        if (currentQueue != null) {
            for (Song song : currentQueue) {
                if (song.id.equals(id)) return song;
            }
        }
        // 2. Kế tiếp quét trong MockData phòng hờ
        if (state != null && state.songs != null) {
            for (Song song : state.songs) {
                if (song.id.equals(id)) return song;
            }
        }
        return null;
    }
    // =========================================================================
    // CÁC HÀM XỬ LÝ LỊCH SỬ / DOWNLOAD / AI MÀ PLAYER ACTIVITY ĐANG GỌI
    // =========================================================================
    public boolean toggleDownloadSong(String songId) {
        AppUser user = getCurrentUser();
        if (user == null) return false;
        boolean isDownloaded = user.downloadedSongIds.contains(songId);
        if (isDownloaded) {
            user.downloadedSongIds.remove(songId);
        } else {
            user.downloadedSongIds.add(songId);
        }
        save();
        return !isDownloaded;
    }

    public java.io.File getPlayableFileIfDownloaded(String songId) {
        // Tạm thời luôn trả về NULL để ép AppStream nhạc trực tiếp từ URL Supabase mạng!
        return null;
    }

    public void recordPlay(String songId, int listenedSec) {
        // Tương lai bạn sẽ gọi API Supabase lên bảng 'plays' và 'listen_history' ở đây.
        // Tạm thời để trống, app vẫn phát nhạc bình thường.
    }

    public String getAiSummaryForSong(String songId) {
        return "AI Summary đang phân tích hàng ngàn bình luận... Bài hát này hiện đang được stream trực tiếp từ hệ thống Supabase!";
    }
}