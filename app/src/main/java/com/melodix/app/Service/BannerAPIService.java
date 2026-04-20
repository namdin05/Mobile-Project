package com.melodix.app.Service;

import com.melodix.app.Model.Banner;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface BannerAPIService {
    @GET("banners?is_active=eq.true&order=order_index.asc")
    Call<List<Banner>> getBanners(
    );
}
