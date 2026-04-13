package com.melodix.app.Service;

import com.melodix.app.Model.Album;
import com.melodix.app.Model.Song;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface AlbumAPIService {

    @GET("rest/v1/album_details_view")
    Call<List<Album>> getAllAlbums(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token
    );

    @GET("album_details_view?select=*")
    Call<List<Album>> getAlbumById(@Query("id") String idQuery);

    @GET("song_details_view?select=*&status=eq.approved")
    Call<List<Song>> getSongsByAlbumId(@Query("album_id") String albumIdQuery);
}