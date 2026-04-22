package com.melodix.app.Service;

import com.melodix.app.Model.Album;
import com.melodix.app.Model.Song;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

public interface AlbumAPIService {
    @GET("album_details_view")
    Call<List<Album>> getAllAlbums();

    @GET("album_details_view?select=*")
    Call<List<Album>> getAlbumById(@Query("id") String idQuery);

    @GET("song_details_view?select=*&status=eq.approved")
    Call<List<Song>> getSongsByAlbumId(@Query("album_id") String albumIdQuery);

    // DRY
    @GET("song_details_view")
    Call<List<Song>> getAlbumDetails(@Query("album_id") String albumIdQuery);

    @GET("song_details_view?select=*")
    Call<List<Song>> getSongsByAlbumIdForArtist(@Query("album_id") String albumIdQuery);

    @PATCH("albums")
    Call<ResponseBody> updateStatus(
            @Query("id") String idFilter,
            @Body Map<String, Object> body
    );
}