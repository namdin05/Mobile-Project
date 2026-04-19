package com.melodix.app.Repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.melodix.app.Model.Album;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.ArtistStats;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.SearchResultItem;
import com.melodix.app.Model.Song;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Import 4 file Service API chuẩn
import com.melodix.app.Service.AlbumAPIService;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.SearchAPIService;

public class AppRepository {
    private static final String KEY_RECENT_SEARCHES = "recent_searches_json";
    private static final String KEY_DOWNLOADED_SONGS = "downloaded_songs_json";

    private static AppRepository instance;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final Gson gson;

    private final SearchAPIService searchApiService;
    private final AlbumAPIService albumApiService;
    private final ArtistAPIService artistApiService;
    private final ProfileAPIService profileApiService; // Thêm Profile API cho User Profile/Follow

    private long lastSearchTime = 0;

    public interface SingleSongCallback {
        void onSuccess(Song song);

        void onError(String message);
    }

    private AppRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(Constants.PREFS_DATA, Context.MODE_PRIVATE);
        this.gson = new GsonBuilder().create();

        // Khởi tạo các Service gọi thẳng vào Supabase
        this.searchApiService = RetrofitClient.getSupabaseClient().create(SearchAPIService.class);
        this.albumApiService = RetrofitClient.getSupabaseClient().create(AlbumAPIService.class);
        this.artistApiService = RetrofitClient.getSupabaseClient().create(ArtistAPIService.class);
        this.profileApiService = RetrofitClient.getSupabaseClient().create(ProfileAPIService.class);
    }

    public static synchronized AppRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AppRepository(context);
        }
        return instance;
    }

    // =========================================================================
    // QUẢN LÝ USER HIỆN TẠI (ĐỒNG BỘ 100% VỚI LOGIN_ACTIVITY BẰNG PREFS)
    // =========================================================================

    public String getCurrentUserId() {
        SharedPreferences melodixPrefs = appContext.getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        return melodixPrefs.getString("USER_ID", null);
    }

    public String getCurrentUserRole() {
        SharedPreferences melodixPrefs = appContext.getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        return melodixPrefs.getString("USER_ROLE", "user");
    }

    // =========================================================================
    // CÁC HÀM GET DỮ LIỆU ĐÃ CHUYỂN SANG API (BỎ MOCK HOÀN TOÀN)
    // =========================================================================

    public ArrayList<Song> getAllApprovedSongs() {
        // Tương lai sếp nên viết API cho hàm này. Tạm thời trả về rỗng vì giao diện đã gọi API riêng.
        return new ArrayList<>();
    }

    public ArrayList<Playlist> getCurrentUserPlaylists() {
        return new ArrayList<>(); // Đã chuyển sang PlaylistRepository lo vụ này
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

    // =========================================================================
    // HÀM LƯU LỊCH SỬ TÌM KIẾM (LƯU LOCAL BẰNG SHAREDPREFERENCES)
    // =========================================================================

    public ArrayList<String> getRecentSearches() {
        String json = prefs.getString(KEY_RECENT_SEARCHES, "[]");
        java.lang.reflect.Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public void saveToRecentSearch(String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) return;
        ArrayList<String> searches = getRecentSearches();
        searches.remove(q);
        searches.add(0, q);
        if (searches.size() > 8) searches.remove(searches.size() - 1);
        prefs.edit().putString(KEY_RECENT_SEARCHES, gson.toJson(searches)).apply();
    }

    public void clearRecentSearches() {
        prefs.edit().putString(KEY_RECENT_SEARCHES, "[]").apply();
    }

    public void removeRecentSearch(String keyword) {
        if (keyword != null) {
            ArrayList<String> searches = getRecentSearches();
            searches.remove(keyword);
            prefs.edit().putString(KEY_RECENT_SEARCHES, gson.toJson(searches)).apply();
        }
    }

    // =========================================================================
    // HÀM SEARCH FULL-TEXT (CÚ PHÁP CHUẨN SUPABASE KẾT HỢP DEBOUNCE)
    // =========================================================================
    public void search(String keyword, String filter, SearchCallback callback) {
        ArrayList<SearchResultItem> results = new ArrayList<>();
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) {
            callback.onSuccess(results);
            return;
        }

        final long currentRequestTime = System.currentTimeMillis();
        lastSearchTime = currentRequestTime;

        String formattedKeyword = q.replaceAll("\\s+", " & ") + ":*";
        String encodedKeyword = android.net.Uri.encode(formattedKeyword);
        String ftsQuery = "fts(simple)." + encodedKeyword;

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
                if (currentRequestTime == lastSearchTime) {
                    callback.onSuccess(new ArrayList<>(syncResults));
                }
            }
        };

        if (searchSongs) {
            searchApiService.searchSongs(ftsQuery).enqueue(new Callback<List<Song>>() {
                @Override
                public void onResponse(Call<List<Song>> call, retrofit2.Response<List<Song>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        searchCacheSongs.clear();
                        for (Song song : response.body()) {
                            searchCacheSongs.add(song);
                            syncResults.add(new SearchResultItem(Constants.FILTER_SONG, song.getId(), song.getTitle(), "Bài hát", song.getCoverUrl()));
                        }
                    }
                    checkCompletion.run();
                }

                @Override
                public void onFailure(Call<List<Song>> call, Throwable t) {
                    checkCompletion.run();
                }
            });
        }

        if (searchArtists) {
            searchApiService.searchArtists(ftsQuery).enqueue(new Callback<List<Artist>>() {
                @Override
                public void onResponse(Call<List<Artist>> call, retrofit2.Response<List<Artist>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Artist artist : response.body())
                            syncResults.add(new SearchResultItem(Constants.FILTER_ARTIST, artist.id, artist.name, "Nghệ sĩ", artist.avatarRes));
                    }
                    checkCompletion.run();
                }

                @Override
                public void onFailure(Call<List<Artist>> call, Throwable t) {
                    checkCompletion.run();
                }
            });
        }

        if (searchAlbums) {
            searchApiService.searchAlbums(ftsQuery).enqueue(new Callback<List<Album>>() {
                @Override
                public void onResponse(Call<List<Album>> call, retrofit2.Response<List<Album>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Album album : response.body())
                            syncResults.add(new SearchResultItem(Constants.FILTER_ALBUM, album.id, album.title, "Album", album.coverRes));
                    }
                    checkCompletion.run();
                }

                @Override
                public void onFailure(Call<List<Album>> call, Throwable t) {
                    checkCompletion.run();
                }
            });
        }

        if (searchPlaylists) {
            searchApiService.searchPlaylists(ftsQuery).enqueue(new Callback<List<Playlist>>() {
                @Override
                public void onResponse(Call<List<Playlist>> call, retrofit2.Response<List<Playlist>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Playlist playlist : response.body())
                            syncResults.add(new SearchResultItem(Constants.FILTER_PLAYLIST, playlist.id, playlist.name, "Playlist", playlist.coverRes));
                    }
                    checkCompletion.run();
                }

                @Override
                public void onFailure(Call<List<Playlist>> call, Throwable t) {
                    checkCompletion.run();
                }
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

    public interface ArtistStatsCallback {
        void onSuccess(ArtistStats stats);

        void onError(String message);
    }

    public void getArtistStats(String artistId, ArtistStatsCallback callback) {
        artistApiService.getArtistStats("eq." + artistId).enqueue(new Callback<List<ArtistStats>>() {
            @Override
            public void onResponse(Call<List<ArtistStats>> call, Response<List<ArtistStats>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    callback.onSuccess(response.body().get(0));
                } else {
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

    public interface AlbumListCallback {
        void onSuccess(ArrayList<Album> albums);

        void onError(String message);
    }

    public interface ArtistListCallback {
        void onSuccess(ArrayList<Artist> artists);

        void onError(String message);
    }

    public void getSongsByArtist(String artistId, SongListCallback callback) {
        artistApiService.getSongsByArtistId("eq." + artistId).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null)
                    callback.onSuccess(new ArrayList<>(response.body()));
                else callback.onError("Lỗi tải bài hát (Code: " + response.code() + ")");
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                callback.onError("Lỗi mạng");
            }
        });
    }

    public void getAlbumsByArtist(String artistId, AlbumListCallback callback) {
        artistApiService.getAlbumsForPublic("eq." + artistId).enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null)
                    callback.onSuccess(new ArrayList<>(response.body()));
                else callback.onError("Lỗi tải Album");
            }

            @Override
            public void onFailure(Call<List<Album>> call, Throwable t) {
                callback.onError("Lỗi mạng");
            }
        });
    }

    public void getRelatedArtists(String currentArtistId, ArtistListCallback callback) {
        artistApiService.getRelatedArtists("neq." + currentArtistId, 5).enqueue(new Callback<List<Artist>>() {
            @Override
            public void onResponse(Call<List<Artist>> call, Response<List<Artist>> response) {
                if (response.isSuccessful() && response.body() != null)
                    callback.onSuccess(new ArrayList<>(response.body()));
                else callback.onError("Lỗi tải Nghệ sĩ liên quan");
            }

            @Override
            public void onFailure(Call<List<Artist>> call, Throwable t) {
                callback.onError("Lỗi mạng");
            }
        });
    }

    // =========================================================================
    // QUẢN LÝ HÀNG ĐỢI PHÁT NHẠC (ĐÃ CHUYỂN GIAO QUYỀN CHO PLAYBACK_REPOSITORY)
    // =========================================================================
    private final ArrayList<Song> searchCacheSongs = new ArrayList<>();

    public void setCurrentQueue(ArrayList<Song> queue, String startSongId) {
        // Giao quyền cho PlaybackRepository
        com.melodix.app.Repository.PlaybackRepository.getInstance().setQueue(queue, startSongId);
    }

    public Song getCurrentQueueSong() {
        return com.melodix.app.Repository.PlaybackRepository.getInstance().getCurrentSong();
    }

    public Song moveNextInQueue() {
        return com.melodix.app.Repository.PlaybackRepository.getInstance().moveNext();
    }

    public Song movePreviousInQueue() {
        return com.melodix.app.Repository.PlaybackRepository.getInstance().movePrevious();
    }

    public Song getSongById(String id) {
        if (id == null) return null;

        // 1. Ưu tiên quét trong Hàng đợi xịn (PlaybackRepository)
        try {
            Song s = com.melodix.app.Repository.PlaybackRepository.getInstance().getSongById(id);
            if (s != null) return s;
        } catch (Exception e) {
            android.util.Log.e("APPREPO", "Lỗi lấy bài hát từ PlaybackRepo", e);
        }

        // 2. Kế tiếp quét trong Cache tìm kiếm phòng hờ
        if (searchCacheSongs != null) {
            for (Song song : searchCacheSongs) {
                if (song.getId().equals(id)) return song;
            }
        }
        return null;
    }

    // =========================================================================
    // HÀM GỌI API LẤY 1 BÀI HÁT TỪ SUPABASE (DÀNH CHO DEEP LINK / PLAY LẺ)
    // =========================================================================
    public void getSongByIdAsync(String songId, SingleSongCallback callback) {
        searchApiService.getSongById("eq." + songId).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
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

    public ArrayList<String> getDownloadedSongIds() {
        String json = prefs.getString(KEY_DOWNLOADED_SONGS, "[]");
        java.lang.reflect.Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public boolean toggleDownloadSong(String songId) {
        ArrayList<String> downloaded = getDownloadedSongIds();
        boolean isDownloaded = downloaded.contains(songId);
        if (isDownloaded) {
            downloaded.remove(songId);
        } else {
            downloaded.add(songId);
        }
        prefs.edit().putString(KEY_DOWNLOADED_SONGS, gson.toJson(downloaded)).apply();
        return !isDownloaded;
    }

    public java.io.File getPlayableFileIfDownloaded(String songId) {
        // Tạm thời luôn trả về NULL để ép App stream nhạc trực tiếp từ URL Supabase mạng!
        return null;
    }

    public void recordPlay(String songId, int listenedSec) {
        // Tương lai bạn sẽ gọi API Supabase lên bảng 'plays' và 'listen_history' ở đây.
    }

    public String getAiSummaryForSong(String songId) {
        return "AI Summary đang phân tích hàng ngàn bình luận... Bài hát này hiện đang được stream trực tiếp từ hệ thống Supabase!";
    }

    public void getMyUploadSongs(String artistId, SongListCallback callback) {
        artistApiService.getMyUploadSongs("eq." + artistId).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(new ArrayList<>(response.body()));
                } else {
                    callback.onError("Lỗi API: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                callback.onError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    // =========================================================================
    // QUẢN LÝ SỐ LƯỢNG FOLLOW
    // =========================================================================

    public interface CountCallback {
        void onSuccess(int count);
    }


    public void getFollowerCount(String artistId, CountCallback callback) {
        profileApiService.getFollowerCount("count=exact", "eq." + artistId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                String range = response.headers().get("Content-Range");
                if (range != null && range.contains("/")) {
                    try {
                        int count = Integer.parseInt(range.split("/")[1]);
                        callback.onSuccess(count);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                callback.onSuccess(0);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onSuccess(0);
            }
        });
    }

    // Lấy số Following

    public void getFollowingCount(String userId, CountCallback callback) {
        profileApiService.getFollowingCount("count=exact", "eq." + userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                String range = response.headers().get("Content-Range");
                if (range != null && range.contains("/")) {
                    try {
                        callback.onSuccess(Integer.parseInt(range.split("/")[1]));
                        return;
                    } catch (Exception ignored) {
                    }
                }
                callback.onSuccess(0);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onSuccess(0);
            }
        });
    }
}
