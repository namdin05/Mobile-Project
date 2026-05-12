package com.melodix.app.Service;

import com.melodix.app.Model.Genre;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GenreAPIService {
    @GET("genres")
    Call<List<Genre>> getGenres();

    @POST("genres")
    Call<ResponseBody> createGenre(
            @Body java.util.Map<String, Object> genreData
    );

    
    @PATCH("genres")
    Call<ResponseBody> updateGenre(
            @Query("id") String idFilter, 
            @Body java.util.Map<String, Object> genreData
    );

}
