package com.melodix.app.Service;

import com.melodix.app.Model.Album;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface AlbumAPIService {
    @GET("albums?select=*")
    Call<List<Album>> getAllAlbums(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token
    );
}