package com.melodix.app.Repository;

import static com.melodix.app.BuildConfig.API_KEY;
import static com.melodix.app.BuildConfig.BASE_URL;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.melodix.app.Model.Genre;
import com.melodix.app.Service.AuthAPIService;
import com.melodix.app.Service.BannerAPIService;
import com.melodix.app.Service.GenreAPIService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GenreRepository {
    private GenreAPIService genreAPIService;
    public GenreRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        genreAPIService = retrofit.create(GenreAPIService.class);
    }

    public MutableLiveData<List<Genre>> fetchGenres(){ // su dung MutableLiveData vi chay ham bat dong bo
        MutableLiveData<List<Genre>> genres = new MutableLiveData<>();

        genreAPIService.getGenres(API_KEY).enqueue(new Callback<List<Genre>>() { // ham callback se chay khi server gui phan hoi
            @Override
            public void onResponse(Call<List<Genre>> call, Response<List<Genre>> response) {
                if(response.isSuccessful() && response.body() != null){
                    genres.setValue(response.body());
                    Log.d("GENRES", "goi database thanh cong");
                    Log.d("GENRES", new Gson().toJson(response.body()));
                }
            }

            @Override
            public void onFailure(Call<List<Genre>> call, Throwable t) {
                Log.e("API ERROR", "Loi mang: " + t.getMessage());
                genres.setValue(null);
            }
        });
        return genres;
    }

}
