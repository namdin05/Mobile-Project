package com.melodix.app.Repository;

import static com.melodix.app.BuildConfig.API_KEY;
import static com.melodix.app.BuildConfig.BASE_URL;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Model.Banner;
import com.melodix.app.Service.AuthAPIService;
import com.melodix.app.Service.BannerAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Service.SongAPIService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BannerRepository {
    private BannerAPIService bannerAPIService;
    public BannerRepository(Context context) {
        bannerAPIService = RetrofitClient.getClient(context).create(BannerAPIService.class);
    }

    public MutableLiveData<List<Banner>> fetchBanners(){
        MutableLiveData<List<Banner>> banners = new MutableLiveData<>();

        bannerAPIService.getBanners().enqueue(new Callback<List<Banner>>() {
            @Override
            public void onResponse(Call<List<Banner>> call, Response<List<Banner>> response) {
                if(response.isSuccessful() && response.body() != null){
                    banners.setValue(response.body());
                } else {
                    banners.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Banner>> call, Throwable t) {
                Log.e("GET_BANNER", t.getMessage());
                banners.setValue(null);
            }
        });
        return banners;
    }
}
