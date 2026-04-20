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
import android.util.Log;

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

    private String getAuthToken() {
        SessionManager session = SessionManager.getInstance(context);
        if (session.getToken() != null) {
            return "Bearer " + session.getToken();   // Token JWT thật
        }
        // Fallback nếu không có token
        return "Bearer " + BuildConfig.API_KEY;
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
    private void fetchNewlyCreatedPlaylist(String userId, String playlistName, Callback<List<Playlist>> finalCallback) {
        if (userId == null) {
            android.util.Log.e("CREATE_PLAYLIST", "Cannot fetch playlist: userId is null");
            finalCallback.onFailure(null, new Throwable("User ID is null"));
            return;
        }

        // Filter an toàn: lấy theo user_id và name
        String filter = "user_id=eq." + userId + "&name=eq." + playlistName;

        android.util.Log.d("CREATE_PLAYLIST", "Đang query lại playlist với filter: " + filter);

        apiService.getUserPlaylists(filter)
                .enqueue(new Callback<List<Playlist>>() {
                    @Override
                    public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                        android.util.Log.d("CREATE_PLAYLIST", "Query lại thành công - Code: " + response.code()
                                + " | Số playlist: " + (response.body() != null ? response.body().size() : 0));

                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Playlist created = response.body().get(0);
                            android.util.Log.d("CREATE_PLAYLIST", "Playlist mới tạo: ID = " + created.id + " | Name = " + created.name);
                        }

                        // Trả về kết quả cho callback gốc
                        finalCallback.onResponse(call, response);
                    }

                    @Override
                    public void onFailure(Call<List<Playlist>> call, Throwable t) {
                        android.util.Log.e("CREATE_PLAYLIST", "Query lại thất bại: " + t.getMessage());
                        finalCallback.onFailure(call, t);
                    }
                });
    }

    private String getCurrentUserId() {
        SessionManager session = SessionManager.getInstance(context);
        if (session.getCurrentUser() != null) {
            return session.getCurrentUser().getId();
        }
        return null;
    }

    public void getUserPlaylists(String userId, Callback<List<Playlist>> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            Log.e("REPO_DEBUG", "User ID is null");
            callback.onFailure(null, new Throwable("User ID is null"));
            return;
        }

        String filter = "eq." + userId;

        Log.d("REPO_DEBUG", "=== GET USER PLAYLISTS ===");
        Log.d("REPO_DEBUG", "User ID: " + userId);
        Log.d("REPO_DEBUG", "Filter gửi đi: user_id=" + filter);

        apiService.getUserPlaylists(filter)
                .enqueue(new Callback<List<Playlist>>() {
                    @Override
                    public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                        Log.d("REPO_DEBUG", "Response code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            Log.d("REPO_DEBUG", "✅ THÀNH CÔNG - Số playlist: " + response.body().size());
                        } else {
                            try {
                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                                Log.e("REPO_DEBUG", "❌ LỖI: " + response.code() + " - " + errorBody);
                            } catch (Exception ignored) {}
                        }
                        callback.onResponse(call, response);
                    }

                    @Override
                    public void onFailure(Call<List<Playlist>> call, Throwable t) {
                        Log.e("REPO_DEBUG", "Lỗi mạng: " + t.getMessage());
                        callback.onFailure(call, t);
                    }
                });
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

    public void updatePlaylistSongOrder(String playlistId, String songId, int newOrderIndex, Callback<ResponseBody> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("order_index", newOrderIndex);

        apiService.updatePlaylistSongOrder(
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
}