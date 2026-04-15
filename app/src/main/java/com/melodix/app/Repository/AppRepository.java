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
import com.melodix.app.Model.ArtistStats;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.SearchResultItem;
import com.melodix.app.Model.Song;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Utils.Constants;
import com.melodix.app.Utils.SessionManager;

import java.util.ArrayList;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Import 3 file Service mới tách
import com.melodix.app.Service.AlbumAPIService;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.SearchAPIService;

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

    // Đã thay SupabaseApi bằng 3 Service mới
    private final SearchAPIService searchApiService;
    private final AlbumAPIService albumApiService;
    private final ArtistAPIService artistApiService;

    // Giữ lại bộ đếm thời gian để chống giật lag khi gõ phím
    private long lastSearchTime = 0;
    public interface SingleSongCallback {
        void onSuccess(Song song);
        void onError(String message);
    }
    private AppRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(Constants.PREFS_DATA, Context.MODE_PRIVATE);
        this.gson = new GsonBuilder().create();
        this.sessionManager = new SessionManager(appContext);
        load();

        // Vẫn giữ nguyên SupabaseClient như bạn yêu cầu, chỉ đổi Class truyền vào
        this.searchApiService = RetrofitClient.getSupabaseClient().create(SearchAPIService.class);
        this.albumApiService = RetrofitClient.getSupabaseClient().create(AlbumAPIService.class);
        this.artistApiService = RetrofitClient.getSupabaseClient().create(ArtistAPIService.class);
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
        albumApiService.getAlbumById("eq." + id).enqueue(new Callback<List<Album>>() {
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
        albumApiService.getSongsByAlbumId("eq." + albumId).enqueue(new Callback<List<Song>>() {
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
            // So sánh trạng thái bằng String theo logic database mới
            if ("approved".equals(song.getStatus())) {
                list.add(song);
            }
        }
        return filterByOfflineMode(list);
    }

    private ArrayList<Song> filterByOfflineMode(ArrayList<Song> source) {
        AppUser user = getCurrentUser();
        if (user == null || !user.offlineMode) return source;
        ArrayList<Song> filtered = new ArrayList<>();
        for (Song song : source) {
            if (user.downloadedSongIds.contains(song.getId())) {
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
            searchApiService.searchSongs(ftsQuery).enqueue(new Callback<List<Song>>() {
                @Override public void onResponse(Call<List<Song>> call, retrofit2.Response<List<Song>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Song song : response.body()) syncResults.add(new SearchResultItem(Constants.FILTER_SONG, song.getId(), song.getTitle(), "Bài hát", song.getCoverUrl()));
                    }
                    checkCompletion.run();
                }
                @Override public void onFailure(Call<List<Song>> call, Throwable t) { checkCompletion.run(); }
            });
        }

        if (searchArtists) {
            searchApiService.searchArtists(ftsQuery).enqueue(new Callback<List<Artist>>() {
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
            searchApiService.searchAlbums(ftsQuery).enqueue(new Callback<List<Album>>() {
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
            searchApiService.searchPlaylists(ftsQuery).enqueue(new Callback<List<Playlist>>() {
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
        artistApiService.getArtistByIdAPI("eq." + id).enqueue(new Callback<List<Artist>>() {
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

    // 1. Khai báo Interface để chờ kết quả
    public interface ArtistStatsCallback {
        void onSuccess(ArtistStats stats);
        void onError(String message);
    }

    // 2. Hàm gọi API
    public void getArtistStats(String artistId, ArtistStatsCallback callback) {
        artistApiService.getArtistStats("eq." + artistId).enqueue(new Callback<List<ArtistStats>>() {
            @Override
            public void onResponse(Call<List<ArtistStats>> call, Response<List<ArtistStats>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Thành công: Trả về kết quả đầu tiên
                    callback.onSuccess(response.body().get(0));
                } else {
                    // An toàn chống Crash: Nếu nghệ sĩ chưa có bài hát nào, trả về toàn số 0
                    ArtistStats emptyStats = new ArtistStats();
                    emptyStats.artistId = artistId;
                    emptyStats.totalSongs = 0;
                    emptyStats.totalStreams = 0;
                    emptyStats.totalLikes = 0;
                    callback.onSuccess(emptyStats);
                }
            }

            @Override
            public void onFailure(Call<List<ArtistStats>> call, Throwable t) {
                callback.onError("Lỗi mạng khi tải thống kê: " + t.getMessage());
            }
        });
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
        artistApiService.getSongsByArtistId("eq." + artistId).enqueue(new Callback<List<Song>>() {
            @Override public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(new ArrayList<>(response.body()));
                else callback.onError("Lỗi tải bài hát (Code: " + response.code() + ")");
            }
            @Override public void onFailure(Call<List<Song>> call, Throwable t) { callback.onError("Lỗi mạng"); }
        });
    }

    // 2. Lấy Album
    public void getAlbumsByArtist(String artistId, AlbumListCallback callback) {
        artistApiService.getAlbumsForPublic("eq." + artistId).enqueue(new Callback<List<Album>>() {
            @Override public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(new ArrayList<>(response.body()));
                else callback.onError("Lỗi tải Album");
            }
            @Override public void onFailure(Call<List<Album>> call, Throwable t) { callback.onError("Lỗi mạng"); }
        });
    }

    // 3. Lấy Nghệ sĩ liên quan (Dùng toán tử "neq." = Not Equal)
    public void getRelatedArtists(String currentArtistId, ArtistListCallback callback) {
        artistApiService.getRelatedArtists("neq." + currentArtistId, 5).enqueue(new Callback<List<Artist>>() {
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
            if (currentQueue.get(i).getId().equals(startSongId)) {
                this.currentQueueIndex = i;
                break;
            }
        }
    }

    public ArrayList<String> getCurrentQueueSongIds() {
        ArrayList<String> ids = new ArrayList<>();
        if (currentQueue != null) {
            for (Song s : currentQueue) ids.add(s.getId());
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
                if (song.getId().equals(id)) return song;
            }
        }
        // 2. Kế tiếp quét trong MockData phòng hờ
        if (state != null && state.songs != null) {
            for (Song song : state.songs) {
                if (song.getId().equals(id)) return song;
            }
        }
        return null;
    }

    // =========================================================================
    // HÀM GỌI API LẤY 1 BÀI HÁT TỪ SUPABASE (DÀNH CHO DEEP LINK)
    // =========================================================================
    public void getSongByIdAsync(String songId, SingleSongCallback callback) {
        // GIẢ SỬ BẠN SỬ DỤNG searchApiService ĐỂ GỌI API (Hoặc một SongAPIService nếu bạn có)
        // Lưu ý: Cần đảm bảo trong interface Service của bạn có hàm getSongById("eq." + songId)
        searchApiService.getSongById("eq." + songId).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Lấy thành công, trả về bài hát đầu tiên tìm được
                    callback.onSuccess(response.body().get(0));
                } else {
                    callback.onError("Không tìm thấy bài hát trên hệ thống!");
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                callback.onError("Lỗi mạng: " + t.getMessage());
            }
        });
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

    // Lấy tất cả bài hát của một nghệ sĩ (Cả đã duyệt và chưa duyệt)
    public void getMyUploadSongs(String artistId, SongListCallback callback) {
        // Gọi SupabaseClient để tự động gắn API Key
        com.melodix.app.Service.ArtistAPIService apiService =
                com.melodix.app.Service.RetrofitClient.getSupabaseClient().create(com.melodix.app.Service.ArtistAPIService.class);

        // Gọi API getMyUploadSongs đã định nghĩa trong ArtistAPIService
        apiService.getMyUploadSongs("eq." +artistId).enqueue(new retrofit2.Callback<java.util.List<com.melodix.app.Model.Song>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.melodix.app.Model.Song>> call, retrofit2.Response<java.util.List<com.melodix.app.Model.Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(new java.util.ArrayList<>(response.body()));
                } else {
                    callback.onError("Lỗi API: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.melodix.app.Model.Song>> call, Throwable t) {
                callback.onError("Lỗi mạng: " + t.getMessage());
            }
        });
    }
}