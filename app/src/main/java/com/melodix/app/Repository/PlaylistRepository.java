package com.melodix.app.Repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.Model.Song;
import com.melodix.app.Service.PlaylistAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistRepository {

    private final PlaylistAPIService apiService;
    private final Context context;

    public PlaylistRepository(Context context) {
        this.context = context;
        apiService = RetrofitClient.getClient(context).create(PlaylistAPIService.class);

    }

    // ĐÃ SỬA: Lấy Token từ SharedPreferences
    private String getAuthToken() {
        SharedPreferences prefs = context.getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        String savedToken = prefs.getString("AUTH_TOKEN", null);
        if (savedToken != null && !savedToken.isEmpty()) {
            return "Bearer " + savedToken;   // Token JWT thật
        }
        // Fallback nếu không có token
        return "Bearer " + BuildConfig.API_KEY;
    }

    // ĐÃ SỬA: Lấy User ID từ SharedPreferences
    private String getCurrentUserId() {
        SharedPreferences prefs = context.getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        return prefs.getString("USER_ID", null);
    }

    // ==================== PLAYLIST ====================
    public void createPlaylist(String name, String coverUrl, Callback<List<Playlist>> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);

        String userId = getCurrentUserId();
        if (userId != null) {
            data.put("user_id", userId);
        }
        if (coverUrl != null && !coverUrl.isEmpty()) {
            data.put("cover_url", coverUrl);
        }
        data.put("is_public", true);

        Log.d("CREATE_PLAYLIST", "Bắt đầu tạo playlist: " + name + " | userId=" + userId);

        // Vì RLS của playlists đang tắt → dùng API Key cố định
        apiService.createPlaylist(
                "return=representation",
                data
        ).enqueue(callback);
    }

    //Query lại playlist vừa tạo theo user_id và nam
//    private void fetchNewlyCreatedPlaylist(String userId, String playlistName, Callback<List<Playlist>> finalCallback) {
//        if (userId == null) {
//            android.util.Log.e("CREATE_PLAYLIST", "Cannot fetch playlist: userId is null");
//            finalCallback.onFailure(null, new Throwable("User ID is null"));
//            return;
//        }
//
//        // Filter an toàn: lấy theo user_id và name
//        String filter = "user_id=eq." + userId + "&name=eq." + playlistName;
//
//        android.util.Log.d("CREATE_PLAYLIST", "Đang query lại playlist với filter: " + filter);
//
//        apiService.getUserPlaylists(filter)
//                .enqueue(new Callback<List<Playlist>>() {
//                    @Override
//                    public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
//                        android.util.Log.d("CREATE_PLAYLIST", "Query lại thành công - Code: " + response.code()
//                                + " | Số playlist: " + (response.body() != null ? response.body().size() : 0));
//
//                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
//                            Playlist created = response.body().get(0);
//                            android.util.Log.d("CREATE_PLAYLIST", "Playlist mới tạo: ID = " + created.id + " | Name = " + created.name);
//                        }
//
//                        // Trả về kết quả cho callback gốc
//                        finalCallback.onResponse(call, response);
//                    }
//
//                    @Override
//                    public void onFailure(Call<List<Playlist>> call, Throwable t) {
//                        android.util.Log.e("CREATE_PLAYLIST", "Query lại thất bại: " + t.getMessage());
//                        finalCallback.onFailure(call, t);
//                    }
//                });
//    }

    public void getUserPlaylists(String userId, Callback<List<Playlist>> callback) {
        if (userId == null) {
            callback.onFailure(null, new Throwable("User ID is null"));
            return;
        }

        Map<String, String> filters = new HashMap<>();
        filters.put("user_id", "eq." + userId);

        apiService.getUserPlaylists(filters).enqueue(callback);
    }

    public void updatePlaylist(String playlistId, String name, String coverUrl, Callback<ResponseBody> callback) {
        Map<String, Object> data = new HashMap<>();
        if (name != null) data.put("name", name);
        if (coverUrl != null) data.put("cover_url", coverUrl);

        apiService.updatePlaylist("eq." + playlistId, data).enqueue(callback);
    }

    public void deletePlaylist(String playlistId, Callback<ResponseBody> callback) {
        apiService.deletePlaylist("eq." + playlistId).enqueue(callback);
    }

    // ==================== PLAYLIST_SONGS ====================

    public void addSongToPlaylist(String playlistId, String songId, int orderIndex, Callback<ResponseBody> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("playlist_id", playlistId);
        data.put("song_id", songId);
        data.put("order_index", orderIndex);

        apiService.addSongToPlaylist(data).enqueue(callback);
    }

    public void removeSongFromPlaylist(String playlistId, String songId, Callback<ResponseBody> callback) {
        apiService.removeSongFromPlaylist(
                "eq." + playlistId,
                "eq." + songId).enqueue(callback);
    }


    //Lấy danh sách bài hát trong playlist + tên artist

    public void getPlaylistSongs(String playlistId, Callback<List<PlaylistSong>> callback) {
        String filter = "eq." + playlistId;

        android.util.Log.d("PLAYLIST_DEBUG", "Gọi View với playlist_id = " + filter);

        apiService.getPlaylistSongsFromView(filter)
                .enqueue(new Callback<List<PlaylistSong>>() {
                    @Override
                    public void onResponse(Call<List<PlaylistSong>> call, Response<List<PlaylistSong>> response) {
                        android.util.Log.d("PLAYLIST_DEBUG", "Response code từ View: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            android.util.Log.d("PLAYLIST_DEBUG", "Số bài hát trả về: " + response.body().size());

                            for (PlaylistSong ps : response.body()) {
                                if (ps.song != null) {
                                    // Debug: kiểm tra artistname từ view
                                    android.util.Log.d("PLAYLIST_DEBUG", "Song: " + ps.song.getTitle() +
                                            " | Artist from view: " + ps.artistname);
                                }
                            }

                            try {
                                String jsonResponse = new com.google.gson.Gson().toJson(response.body());
                                android.util.Log.d("PLAYLIST_DEBUG", "Full JSON Response: " + jsonResponse);
                            } catch (Exception e) {
                                android.util.Log.e("PLAYLIST_DEBUG", "Lỗi parse JSON: " + e.getMessage());
                            }
                        }

                        callback.onResponse(call, response);
                    }

                    @Override
                    public void onFailure(Call<List<PlaylistSong>> call, Throwable t) {
                        android.util.Log.e("PLAYLIST_ERROR", "Failure: " + t.getMessage());
                        callback.onFailure(call, t);
                    }
                });
    }

    private String getStoredToken() {
        SharedPreferences prefs = context.getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        return prefs.getString("ACCESS_TOKEN", null);
    }

    public void updatePlaylistSongOrder(String playlistId, String songId, int newOrderIndex, Callback<ResponseBody> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("order_index", newOrderIndex);

        String playlistFilter = "eq." + playlistId;
        String songFilter = "eq." + songId;

        Log.d("ORDER_UPDATE", "Gọi PATCH: playlist=" + playlistFilter + ", song=" + songFilter + ", order=" + newOrderIndex);

        String token = getStoredToken();
        if (token == null || token.isEmpty()) {
            Log.e("ORDER_UPDATE", "❌ Không có token! Kiểm lại SharedPreferences");
            // Debug thêm
            SharedPreferences prefs = context.getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
            Log.e("ORDER_UPDATE", "ACCESS_TOKEN: " + (prefs.getString("ACCESS_TOKEN", null) != null ? "exists" : "NULL"));
            Log.e("ORDER_UPDATE", "AUTH_TOKEN: " + (prefs.getString("AUTH_TOKEN", null) != null ? "exists" : "NULL"));
            callback.onFailure(null, new Throwable("No auth token"));
            return;
        }

        Log.d("ORDER_UPDATE", "✅ Có token, length=" + token.length());

        apiService.updatePlaylistSongOrderWithAuth(
                "Bearer " + token,  // 👈 Header Authorization
                "return=representation",
                playlistFilter,
                songFilter,
                data
        ).enqueue(callback);
    }

    public void isSongLiked(String userId, String songId, retrofit2.Callback<Boolean> callback) {
        getLikedPlaylist(userId, new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    String likedPlaylistId = response.body().get(0).id;

                    getPlaylistSongs(likedPlaylistId, new Callback<List<PlaylistSong>>() {
                        @Override
                        public void onResponse(Call<List<PlaylistSong>> call2, Response<List<PlaylistSong>> response2) {
                            boolean liked = false;
                            if (response2.isSuccessful() && response2.body() != null) {
                                for (PlaylistSong ps : response2.body()) {
                                    if (ps.song != null && songId.equals(ps.song.getId())) {
                                        liked = true;
                                        break;
                                    }
                                }
                            }
                            callback.onResponse(null, retrofit2.Response.success(liked));
                        }

                        @Override
                        public void onFailure(Call<List<PlaylistSong>> call2, Throwable t) {
                            callback.onFailure(null, t);
                        }
                    });
                } else {
                    callback.onResponse(null, retrofit2.Response.success(false));
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                callback.onFailure(null, t);
            }
        });
    }

    public void updatePlaylistOrder(String playlistId, List<Song> songs, Callback<ResponseBody> callback) {
        if (songs == null || songs.isEmpty()) {
            if (callback != null) callback.onResponse(null, null);
            return;
        }

        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            Map<String, Object> data = new HashMap<>();
            data.put("order_index", i);

            apiService.updatePlaylistSongOrder(
                    "return=representation",
                    "eq." + playlistId,
                    "eq." + song.getId(),
                    data
            ).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    // Success
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    android.util.Log.e("PlaylistOrder", "Update order failed: " + t.getMessage());
                }
            });
        }

        if (callback != null) {
            callback.onResponse(null, null);
        }
    }

    public void getPlaylistById(String playlistId, Callback<List<Playlist>> callback) {
        String filter = "eq." + playlistId;
        apiService.getPlaylistById(filter).enqueue(callback);
    }

    public void getLikedPlaylist(String userId, Callback<List<Playlist>> callback) {
        if (userId == null) {
            callback.onFailure(null, new Throwable("User ID is null"));
            return;
        }

        // ✅ Dùng Map để truyền nhiều filter
        Map<String, String> filters = new HashMap<>();
        filters.put("user_id", "eq." + userId);
        filters.put("is_liked_playlist", "eq.true");

        apiService.getUserPlaylists(filters).enqueue(new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    callback.onResponse(call, response);
                } else {
                    callback.onFailure(call, new Throwable("No liked playlist found"));
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }
    public void toggleLikeSong(String userId, String songId, Callback<Boolean> callback) {
        Log.d("TOGGLE_LIKE", "🎵 Bắt đầu toggle like cho song: " + songId);

        getLikedPlaylist(userId, new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    String playlistId = response.body().get(0).id;
                    Log.d("TOGGLE_LIKE", "📝 Playlist liked ID: " + playlistId);

                    // Kiểm tra bài hát đã có trong playlist chưa
                    checkSongInPlaylist(playlistId, songId, new Callback<Boolean>() {
                        @Override
                        public void onResponse(Call<Boolean> checkCall, Response<Boolean> checkResponse) {
                            if (checkResponse.isSuccessful() && checkResponse.body() != null) {
                                boolean isLiked = checkResponse.body();
                                Log.d("TOGGLE_LIKE", "Trạng thái like hiện tại: " + isLiked);

                                if (isLiked) {
                                    // Unlike: xóa khỏi playlist
                                    removeSongFromPlaylist(playlistId, songId, new Callback<ResponseBody>() {
                                        @Override
                                        public void onResponse(Call<ResponseBody> call3, Response<ResponseBody> response3) {
                                            if (response3.isSuccessful()) {
                                                Log.d("TOGGLE_LIKE", "✅ Unlike thành công");
                                                callback.onResponse(null, Response.success(false));
                                            } else {
                                                Log.e("TOGGLE_LIKE", "❌ Unlike thất bại, code: " + response3.code());
                                                callback.onFailure(null, new Throwable("Unlike failed"));
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<ResponseBody> call3, Throwable t) {
                                            Log.e("TOGGLE_LIKE", "❌ Unlike thất bại: " + t.getMessage());
                                            callback.onFailure(null, t);
                                        }
                                    });
                                } else {
                                    // Like: thêm vào playlist
                                    addSongToPlaylist(playlistId, songId, 0, new Callback<ResponseBody>() {
                                        @Override
                                        public void onResponse(Call<ResponseBody> call3, Response<ResponseBody> response3) {
                                            if (response3.isSuccessful()) {
                                                Log.d("TOGGLE_LIKE", "✅ Like thành công");
                                                callback.onResponse(null, Response.success(true));
                                            } else {
                                                Log.e("TOGGLE_LIKE", "❌ Like thất bại, code: " + response3.code());
                                                callback.onFailure(null, new Throwable("Like failed"));
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<ResponseBody> call3, Throwable t) {
                                            Log.e("TOGGLE_LIKE", "❌ Like thất bại: " + t.getMessage());
                                            callback.onFailure(null, t);
                                        }
                                    });
                                }
                            } else {
                                Log.e("TOGGLE_LIKE", "❌ Kiểm tra trạng thái thất bại");
                                callback.onFailure(null, new Throwable("Cannot check like status"));
                            }
                        }

                        @Override
                        public void onFailure(Call<Boolean> checkCall, Throwable t) {
                            Log.e("TOGGLE_LIKE", "❌ Kiểm tra thất bại: " + t.getMessage());
                            callback.onFailure(null, t);
                        }
                    });
                } else {
                    Log.e("TOGGLE_LIKE", "❌ Không tìm thấy playlist liked");
                    callback.onFailure(null, new Throwable("Cannot find liked playlist"));
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                Log.e("TOGGLE_LIKE", "❌ Lỗi: " + t.getMessage());
                callback.onFailure(null, t);
            }
        });
    }
    private void checkSongInPlaylist(String playlistId, String songId, Callback<Boolean> callback) {
        getPlaylistSongs(playlistId, new Callback<List<PlaylistSong>>() {
            @Override
            public void onResponse(Call<List<PlaylistSong>> call, Response<List<PlaylistSong>> response) {
                boolean exists = false;
                if (response.isSuccessful() && response.body() != null) {
                    for (PlaylistSong ps : response.body()) {
                        if (ps.song != null && songId.equals(ps.song.getId())) {
                            exists = true;
                            break;
                        }
                    }
                }
                Log.d("CHECK_SONG", "Bài hát " + songId + " tồn tại trong playlist? " + exists);

                // ✅ Giải pháp: truyền null - callback không thực sự cần dùng đến call
                callback.onResponse(null, Response.success(exists));
            }

            @Override
            public void onFailure(Call<List<PlaylistSong>> call, Throwable t) {
                Log.e("CHECK_SONG", "Lỗi kiểm tra: " + t.getMessage());
                callback.onFailure(null, t);
            }
        });
    }

}