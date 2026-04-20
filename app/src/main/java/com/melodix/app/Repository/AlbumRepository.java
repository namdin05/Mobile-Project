package com.melodix.app.Repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Model.Album;
import com.melodix.app.Model.Song;
import com.melodix.app.Service.AlbumAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlbumRepository {
    private AlbumAPIService apiService;

    public AlbumRepository(Context context) {
        apiService = RetrofitClient.getClient(context).create(AlbumAPIService.class);
    }

    public MutableLiveData<List<Album>> getAllAlbums() {
        MutableLiveData<List<Album>> albums = new MutableLiveData<>();

        apiService.getAllAlbums().enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    albums.setValue(response.body());
                } else {
                    Log.e("AdminAlbum", "Lỗi tải album: " + response.code());
                    albums.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Album>> call, Throwable t) {
                Log.e("AdminAlbum", "Lỗi mạng: " + t.getMessage());
                albums.setValue(null);
            }
        });

        return albums;
    }

    public MutableLiveData<List<Song>> getSongsByAlbumId(String id) {
        MutableLiveData<List<Song>> songs = new MutableLiveData<>();

        apiService.getAlbumDetails(id).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    songs.setValue(response.body());
                } else {
                    Log.e("AdminAlbum", "Lỗi tải album: " + response.code());
                    songs.setValue(null);
                }
            }

            public void onFailure(Call<List<Song>> call, Throwable t) {
                Log.e("AdminAlbum", "Lỗi mạng: " + t.getMessage());
                songs.setValue(null);
            }
        });

        return songs;

    }

    public void getAlbumById(String id, AppRepository.AlbumCallback callback) {
        apiService.getAlbumById("eq." + id).enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, retrofit2.Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    callback.onSuccess(response.body().get(0));
                } else {
                    callback.onError("Không tìm thấy dữ liệu Album! Mã lỗi: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<List<Album>> call, Throwable t) {
                callback.onError("Lỗi kết nối mạng: " + t.getMessage());
            }
        });
    }
}
