package com.melodix.app.Service;

import com.melodix.app.Model.Genre;
import com.melodix.app.Model.Profile;
import com.melodix.app.Model.Song;
import com.melodix.app.Model.StatusUpdateRequest;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AdminAPIService {
    @GET("rest/v1/songs?status=eq.pending")
    Call<List<Song>> getPendingRequests(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token
    );

    @PATCH("rest/v1/songs")
    Call<Void> updateRequestStatus(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("id") String idFilter,
            @Body StatusUpdateRequest body
    );

    @GET("rest/v1/songs?select=*,profiles!song_artists(display_name)")
    Call<List<Song>> getAllSongs(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token
    );

    // Lấy tất cả User
    @GET("rest/v1/profiles?select=*")
    Call<List<Profile>> getAllUsers(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token
    );


    @GET("rest/v1/genres?select=*")
    Call<List<Genre>> getAllGenres(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token

    );

    @POST("rest/v1/genres")
    Call<ResponseBody> createGenre(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Body java.util.Map<String, Object> genreData
    );

    // UPDATE & SOFT DELETE (Cập nhật tên, hình ảnh, hoặc ẩn đi)
    @PATCH("rest/v1/genres")
    Call<ResponseBody> updateGenre(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("id") String idFilter, // eq.xxx
            @Body java.util.Map<String, Object> genreData
    );
}