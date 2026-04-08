package com.melodix.app.Repository;

import android.content.Context;
import android.widget.Toast;

import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.Model.SessionManager;
import com.melodix.app.Model.Song;
import com.melodix.app.Service.PlaylistAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistRepository {

    private final PlaylistAPIService apiService;
    private final String apiKey;
    private final String token;  // Dùng API Key cố định
    private final Context context;

    public PlaylistRepository(Context context) {
        this.context = context;
        apiService = RetrofitClient.getClient().create(PlaylistAPIService.class);
        apiKey = BuildConfig.API_KEY;
        // Dùng API Key thay vì User Token
        token = "Bearer " + BuildConfig.API_KEY;
    }

    // ==================== PLAYLIST ====================

    public void createPlaylist(String name, String coverUrl, Callback<Playlist> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);

        String userId = getCurrentUserId();
        if (userId != null) {
            data.put("user_id", userId);
        } else {
            Toast.makeText(context, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.onFailure(null, new Exception("User not logged in"));
            }
            return;
        }

        if (coverUrl != null && !coverUrl.isEmpty()) {
            data.put("cover_url", coverUrl);
        }
        data.put("is_public", true);

        android.util.Log.d("REPO_DEBUG", "Creating playlist with API Key");
        apiService.createPlaylist(apiKey, token, data).enqueue(callback);
    }

    private String getCurrentUserId() {
        SessionManager session = SessionManager.getInstance(context);
        if (session.getCurrentUser() != null) {
            return session.getCurrentUser().getId();
        }
        return null;
    }

    public void getUserPlaylists(String userId, Callback<List<Playlist>> callback) {
        String filter = "eq." + userId;

        android.util.Log.d("REPO_DEBUG", "=== GET USER PLAYLISTS ===");
        android.util.Log.d("REPO_DEBUG", "User ID: " + userId);
        android.util.Log.d("REPO_DEBUG", "Filter: " + filter);
        android.util.Log.d("REPO_DEBUG", "Using API Key (no user token)");

        apiService.getUserPlaylists(apiKey, token, filter).enqueue(new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                android.util.Log.d("REPO_DEBUG", "Response code: " + response.code());
                if (response.isSuccessful()) {
                    android.util.Log.d("REPO_DEBUG", "Success, playlists count: " + (response.body() != null ? response.body().size() : 0));
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown";
                        android.util.Log.e("REPO_DEBUG", "Error: " + response.code() + " - " + errorBody);
                    } catch (Exception e) {}
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                android.util.Log.e("REPO_DEBUG", "Failure: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    public void updatePlaylist(String playlistId, String name, String coverUrl, Callback<ResponseBody> callback) {
        Map<String, Object> data = new HashMap<>();
        if (name != null) data.put("name", name);
        if (coverUrl != null) data.put("cover_url", coverUrl);

        apiService.updatePlaylist(apiKey, token, "eq." + playlistId, data).enqueue(callback);
    }

    public void deletePlaylist(String playlistId, Callback<ResponseBody> callback) {
        apiService.deletePlaylist(apiKey, token, "eq." + playlistId).enqueue(callback);
    }

    // ==================== PLAYLIST_SONGS ====================

    public void addSongToPlaylist(String playlistId, String songId, int orderIndex, Callback<ResponseBody> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("playlist_id", playlistId);
        data.put("song_id", songId);
        data.put("order_index", orderIndex);

        apiService.addSongToPlaylist(apiKey, token, data).enqueue(callback);
    }

    public void removeSongFromPlaylist(String playlistId, String songId, Callback<ResponseBody> callback) {
        apiService.removeSongFromPlaylist(apiKey, token,
                "eq." + playlistId,
                "eq." + songId).enqueue(callback);
    }

    /**
     * Lấy danh sách bài hát trong playlist + tên artist (fix Unknown Artist và lỗi tải)
     * KHÔNG set artist vào Song, giữ nguyên artistname trong PlaylistSong
     */
    public void getPlaylistSongs(String playlistId, Callback<List<PlaylistSong>> callback) {
        String filter = "eq." + playlistId;

        android.util.Log.d("PLAYLIST_DEBUG", "Gọi View với playlist_id = " + filter);

        apiService.getPlaylistSongsFromView(apiKey, token, filter)
                .enqueue(new Callback<List<PlaylistSong>>() {
                    @Override
                    public void onResponse(Call<List<PlaylistSong>> call, Response<List<PlaylistSong>> response) {
                        android.util.Log.d("PLAYLIST_DEBUG", "Response code từ View: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            android.util.Log.d("PLAYLIST_DEBUG", "Số bài hát trả về: " + response.body().size());

                            // CHỈ LOG, KHÔNG SET ARTIST VÀO SONG
                            for (PlaylistSong ps : response.body()) {
                                if (ps.song != null) {
                                    // Debug: kiểm tra artistname từ view
                                    android.util.Log.d("PLAYLIST_DEBUG", "Song: " + ps.song.getTitle() +
                                            " | Artist from view: " + ps.artistname);
                                }
                            }

                            // IN RA TOÀN BỘ RESPONSE ĐỂ DEBUG
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

    public void updatePlaylistSongOrder(String playlistId, String songId, int newOrderIndex, Callback<ResponseBody> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("order_index", newOrderIndex);

        apiService.updatePlaylistSongOrder(
                apiKey,
                token,
                "eq." + playlistId,
                "eq." + songId,
                data
        ).enqueue(callback);
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
                    apiKey,
                    token,
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
        apiService.getPlaylistById(apiKey, token, filter).enqueue(callback);
    }
}