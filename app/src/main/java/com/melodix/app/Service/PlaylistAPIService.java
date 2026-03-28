package com.melodix.app.Service;

import com.melodix.app.Model.AddToPlaylistRequest;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.Model.PlaylistUpdateRequest;
import com.melodix.app.Model.UpdateOrderRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PlaylistAPIService {

    // Lấy tất cả playlist của user hiện tại
    @GET("rest/v1/playlists?select=*")
    Call<List<Playlist>> getMyPlaylists(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("user_id") String userIdFilter   // eq.{user_id}
    );

    // Tạo playlist mới
    @POST("rest/v1/playlists")
    Call<Playlist> createPlaylist(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Body PlaylistCreateRequest body
    );

    // Cập nhật playlist (tên, cover, public)
    @PATCH("rest/v1/playlists")
    Call<Void> updatePlaylist(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("id") String idFilter,   // eq.{playlist_id}
            @Body PlaylistUpdateRequest body
    );

    // Xóa playlist
    @DELETE("rest/v1/playlists")
    Call<Void> deletePlaylist(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("id") String idFilter
    );

    // Lấy danh sách bài hát trong playlist (có join songs)
    @GET("rest/v1/playlist_songs?select=order_index,song_id,added_at,songs(id,title,audio_url,cover_url,duration_seconds)")
    Call<List<PlaylistSong>> getPlaylistSongs(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("playlist_id") String playlistIdFilter   // eq.{playlist_id}
    );

    // Thêm bài hát vào playlist
    @POST("rest/v1/playlist_songs")
    Call<Void> addSongToPlaylist(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Body AddToPlaylistRequest body
    );

    // Xóa bài hát khỏi playlist
    @DELETE("rest/v1/playlist_songs")
    Call<Void> removeSongFromPlaylist(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("playlist_id") String playlistIdFilter,
            @Query("song_id") String songIdFilter
    );

    // Cập nhật thứ tự (order_index)
    @PATCH("rest/v1/playlist_songs")
    Call<Void> updateSongOrder(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("playlist_id") String playlistIdFilter,
            @Query("song_id") String songIdFilter,
            @Body UpdateOrderRequest body
    );
}