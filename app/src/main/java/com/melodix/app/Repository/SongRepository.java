package com.melodix.app.Repository;

import android.content.Context;
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
    private SongAPIService songAPIService;


    public SongRepository(Context context) {
        songAPIService = RetrofitClient.getClient(context).create(SongAPIService.class);
    }

    public MutableLiveData<List<Song>> fetchAllSongs(){
        MutableLiveData<List<Song>> songs = new MutableLiveData<>();

        songAPIService.getAllSongs().enqueue(new Callback<List<Song>>() {
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

        songAPIService.getNewReleaseSongs(5).enqueue(new Callback<List<Song>>() {
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

        songAPIService.getTrendingSongs(5).enqueue(new Callback<List<Song>>() {
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

    public void updateSongStatus(String songId, String newStatus, MutableLiveData<Boolean> isSuccess, MutableLiveData<String> message) {
        // Tái sử dụng model StatusUpdateRequest của bạn
        com.melodix.app.Model.StatusUpdateRequest body = new com.melodix.app.Model.StatusUpdateRequest(newStatus);

        songAPIService.updateRequestStatus("eq." + songId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    String msg = newStatus.equals("approved") ? "Đã duyệt bài thành công!" : "Đã từ chối bài hát.";
                    message.postValue(msg);
                    isSuccess.postValue(true);
                } else {
                    message.postValue("Lỗi từ máy chủ!");
                    isSuccess.postValue(false);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                message.postValue("Mất mạng rồi!");
                isSuccess.postValue(false);
            }
        });
    }

}
