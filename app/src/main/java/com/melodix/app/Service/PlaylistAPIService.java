package com.melodix.app.Service;

import com.melodix.app.Model.Playlist;
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

    @POST("playlists")
    Call<List<Playlist>> createPlaylist(
            @Header("Prefer") String prefer,
            @Body Map<String, Object> data
    );

    @GET("playlists")
    Call<List<Playlist>> getUserPlaylists(
            @Query("user_id") String userIdFilter
    );

    @PATCH("playlists")
    Call<ResponseBody> updatePlaylist(
            @Query("id") String idFilter,
            @Body Map<String, Object> data
    );

    @DELETE("playlists")
    Call<ResponseBody> deletePlaylist(
            @Query("id") String idFilter
    );

    // Playlist Songs
    @POST("playlist_songs")
    Call<ResponseBody> addSongToPlaylist(
            @Body Map<String, Object> data
    );

    @DELETE("playlist_songs")
    Call<ResponseBody> removeSongFromPlaylist(
            @Query("playlist_id") String playlistFilter,
            @Query("song_id") String songFilter
    );

    @PATCH("playlist_songs")
    Call<ResponseBody> updatePlaylistSongOrder(
            @Query("playlist_id") String playlistFilter,
            @Query("song_id") String songFilter,
            @Body Map<String, Object> data
    );

    @GET("playlist_songs?select=*,songs(*)")
    Call<List<PlaylistSong>> getPlaylistSongs(
            @Query("playlist_id") String playlistFilter
    );

    @GET("playlists")
    Call<List<Playlist>> getPlaylistById(
            @Query("id") String idFilter
    );

    @GET("playlist_song_details?select=*,songs(*)")
    Call<List<PlaylistSong>> getPlaylistSongsFromView(
            @Query("playlist_id") String playlistFilter
    );
}