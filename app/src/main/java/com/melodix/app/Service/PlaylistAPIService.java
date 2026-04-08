package com.melodix.app.Service;

import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.Song;
import com.melodix.app.Model.PlaylistSong;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PlaylistAPIService {

    @POST("rest/v1/playlists")
    Call<Playlist> createPlaylist(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Body Map<String, Object> data
    );

    @GET("rest/v1/playlists")
    Call<List<Playlist>> getUserPlaylists(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("user_id") String userIdFilter
    );

    @PATCH("rest/v1/playlists")
    Call<ResponseBody> updatePlaylist(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("id") String idFilter,
            @Body Map<String, Object> data
    );

    @DELETE("rest/v1/playlists")
    Call<ResponseBody> deletePlaylist(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("id") String idFilter
    );

    // Playlist Songs
    @POST("rest/v1/playlist_songs")
    Call<ResponseBody> addSongToPlaylist(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Body Map<String, Object> data
    );

    @DELETE("rest/v1/playlist_songs")
    Call<ResponseBody> removeSongFromPlaylist(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("playlist_id") String playlistFilter,
            @Query("song_id") String songFilter
    );

    // Cập nhật thứ tự bài hát trong playlist (dùng cho Drag & Drop)
    @PATCH("rest/v1/playlist_songs")
    Call<ResponseBody> updatePlaylistSongOrder(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("playlist_id") String playlistFilter,
            @Query("song_id") String songFilter,
            @Body Map<String, Object> data
    );

    @GET("rest/v1/playlist_songs?select=*,songs(*)")
    Call<List<PlaylistSong>> getPlaylistSongs(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("playlist_id") String playlistFilter
    );

    @GET("rest/v1/playlists")
    Call<List<Playlist>> getPlaylistById(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("id") String idFilter   // eq.xxxx
    );

    @GET("rest/v1/playlist_song_details?select=*,songs(*)")
    Call<List<PlaylistSong>> getPlaylistSongsFromView(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("playlist_id") String playlistFilter
    );
}