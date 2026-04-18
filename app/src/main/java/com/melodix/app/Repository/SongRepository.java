package com.melodix.app.Repository;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Song;
import com.melodix.app.Service.AuthAPIService;
import com.melodix.app.Service.BannerAPIService;
import com.melodix.app.Service.GenreAPIService;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Service.SongAPIService;
import com.melodix.app.Utils.PlaybackUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SongRepository {
    private static final String API_KEY = BuildConfig.API_KEY;

    private static final String token = "Bearer " + BuildConfig.SERVICE_KEY;
    private SongAPIService songAPIService;

    public SongRepository() {
        songAPIService = RetrofitClient.getClient().create(SongAPIService.class);
    }
    public androidx.lifecycle.LiveData<List<Song>> fetchSongsByGenre(int genreId) {
        androidx.lifecycle.MutableLiveData<List<Song>> liveData = new androidx.lifecycle.MutableLiveData<>();

        java.util.Map<String, Integer> body = new java.util.HashMap<>();
        body.put("p_genre_id", genreId);

        // 1. Khai báo đủ 2 loại Key
        String apiKey = BuildConfig.SERVICE_KEY;
        String authHeader = "Bearer " + apiKey;

        // 2. Gọi hàm và truyền đủ 3 tham số (apiKey, authHeader, body)
        songAPIService.getSongsByGenre(apiKey, authHeader, body).enqueue(new retrofit2.Callback<List<Song>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Song>> call, retrofit2.Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // MÁY QUAY 1: Xem API trả về bao nhiêu bài
                    android.util.Log.d("DEBUG_API", "Thành công! Supabase trả về: " + response.body().size() + " bài hát.");
                    liveData.setValue(response.body());
                } else {
                    liveData.setValue(new java.util.ArrayList<>());
                    try {
                        String errorDetail = response.errorBody() != null ? response.errorBody().string() : "Trống";
                        android.util.Log.e("DEBUG_API", "Lỗi: " + response.code() + " - " + errorDetail);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<Song>> call, Throwable t) {
                liveData.setValue(new java.util.ArrayList<>());
                android.util.Log.e("API_ERROR", "Lỗi mạng: " + t.getMessage());
            }
        });

        return liveData;
    }
    public MutableLiveData<List<Song>> fetchAllSongs(){
        MutableLiveData<List<Song>> songs = new MutableLiveData<>();

        songAPIService.getAllSongs(API_KEY, token).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    songs.setValue(response.body());
                    Log.d("ALL_SONGS", new Gson().toJson(response.body()));
                } else {
                    Log.d("FAILED", "L");
                    songs.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                Log.e("API ERROR", "Loi mang: " + t.getMessage());
                songs.setValue(null);
            }
        });
        return songs;
    }


    public MutableLiveData<List<Song>> fetchNewReleaseSongs(){
        MutableLiveData<List<Song>> songs = new MutableLiveData<>();

        songAPIService.getNewReleaseSongs(API_KEY, 5).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if(response.isSuccessful() && response.body() != null){
                    songs.setValue(response.body());
                    Log.d("NEW_RELEASE_SONGS", new Gson().toJson(response.body()));
                } else {
                    songs.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                Log.e("API ERROR", "Loi mang: " + t.getMessage());
                songs.setValue(null);
            }
        });
        return songs;
    }

    public MutableLiveData<List<Song>> fetchTrendingSongs(){
        MutableLiveData<List<Song>> songs = new MutableLiveData<>();

        songAPIService.getTrendingSongs(API_KEY, 5).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if(response.isSuccessful() && response.body() != null){
                    Log.d("TRENDING", new Gson().toJson(response.body()));
                    songs.setValue(response.body());
                } else {
                    Log.d("TRENDING", "K co bai hat trend");
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                Log.d("TRENDING", "fail");
            }
        });
        return songs;
    }

}