package com.melodix.app.Service;

import com.melodix.app.Model.Genre;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface GenreAPIService {
    @GET("rest/v1/genres")
    Call<List<Genre>> getGenres(
        @Header("apikey") String apiKey
    );

}
