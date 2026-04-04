package com.melodix.app.Repository;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Song;
import com.melodix.app.Service.AuthAPIService;
import com.melodix.app.Service.BannerAPIService;
import com.melodix.app.Service.GenreAPIService;
import com.melodix.app.Service.SongAPIService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SongRepository {
    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static final String API_KEY = BuildConfig.API_KEY;
    private SongAPIService songAPIService;

    public SongRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        songAPIService = retrofit.create(SongAPIService.class);
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
