package com.melodix.app.Repository;

import android.content.Context;
import android.util.Log;

import com.melodix.app.Model.AddToPlaylistRequest;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.Model.PlaylistCreateRequest;
import com.melodix.app.Model.PlaylistUpdateRequest;
import com.melodix.app.Model.UpdateOrderRequest;
import com.melodix.app.Service.PlaylistAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistRepository {

    private static final String TAG = "PlaylistRepository";
    private PlaylistAPIService apiService;

    public PlaylistRepository() {
        apiService = RetrofitClient.getClient().create(PlaylistAPIService.class);
    }

    // ==================== HELPER METHODS ====================

    private String getApiKey() {
        return BuildConfig.API_KEY;
    }

    private String getAuthToken() {
        return "Bearer " + BuildConfig.API_KEY;
    }

    // ==================== PLAYLIST CRUD ====================

    /**
     * Lấy danh sách playlist của user hiện tại
     */
    public void getMyPlaylists(String userId, Callback<List<Playlist>> callback) {
        String userFilter = "eq." + userId;

        apiService.getMyPlaylists(getApiKey(), getAuthToken(), userFilter)
                .enqueue(new Callback<List<Playlist>>() {
                    @Override
                    public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onResponse(call, response);
                        } else {
                            Log.e(TAG, "Lỗi lấy playlist: " + response.code());
                            callback.onFailure(call, new Throwable("Lỗi server: " + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Playlist>> call, Throwable t) {
                        Log.e(TAG, "Lỗi mạng khi lấy playlist: " + t.getMessage());
                        callback.onFailure(call, t);
                    }
                });
    }

    /**
     * Tạo playlist mới
     */
    public void createPlaylist(PlaylistCreateRequest request, Callback<Playlist> callback) {
        apiService.createPlaylist(getApiKey(), getAuthToken(), request)
                .enqueue(callback);
    }

    /**
     * Cập nhật playlist
     */
    public void updatePlaylist(String playlistId, PlaylistUpdateRequest request, Callback<Void> callback) {
        String idFilter = "eq." + playlistId;
        apiService.updatePlaylist(getApiKey(), getAuthToken(), idFilter, request)
                .enqueue(callback);
    }

    /**
     * Xóa playlist
     */
    public void deletePlaylist(String playlistId, Callback<Void> callback) {
        String idFilter = "eq." + playlistId;
        apiService.deletePlaylist(getApiKey(), getAuthToken(), idFilter)
                .enqueue(callback);
    }

    // ==================== PLAYLIST SONGS ====================

    /**
     * Lấy danh sách bài hát trong playlist
     */
    public void getPlaylistSongs(String playlistId, Callback<List<PlaylistSong>> callback) {
        String playlistFilter = "eq." + playlistId;

        apiService.getPlaylistSongs(getApiKey(), getAuthToken(), playlistFilter)
                .enqueue(callback);
    }

    /**
     * Thêm bài hát vào playlist
     */
    public void addSongToPlaylist(AddToPlaylistRequest request, Callback<Void> callback) {
        apiService.addSongToPlaylist(getApiKey(), getAuthToken(), request)
                .enqueue(callback);
    }

    /**
     * Xóa bài hát khỏi playlist
     */
    public void removeSongFromPlaylist(String playlistId, String songId, Callback<Void> callback) {
        String playlistFilter = "eq." + playlistId;
        String songFilter = "eq." + songId;   // Supabase hỗ trợ nhiều query param

        apiService.removeSongFromPlaylist(getApiKey(), getAuthToken(), playlistFilter, songFilter)
                .enqueue(callback);
    }

    /**
     * Cập nhật thứ tự bài hát (Drag & Drop)
     */
    public void updateSongOrder(String playlistId, String songId, int newOrderIndex, Callback<Void> callback) {
        String playlistFilter = "eq." + playlistId;
        String songFilter = "eq." + songId;

        UpdateOrderRequest body = new UpdateOrderRequest(newOrderIndex);

        apiService.updateSongOrder(getApiKey(), getAuthToken(), playlistFilter, songFilter, body)
                .enqueue(callback);
    }
}