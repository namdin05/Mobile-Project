package com.melodix.app.Service;

import com.melodix.app.BuildConfig;
import com.melodix.app.Utils.Constants; // 👉 ĐÃ THÊM IMPORT CONSTANTS

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // 1. Máy chủ gốc (Dành cho Upload Storage, RPC, Auth...)
    private static Retrofit baseRetrofit = null;

    // 2. Máy chủ Database (Dành riêng cho truy vấn bảng, view có sẵn rest/v1/)
    private static Retrofit supabaseDatabaseRetrofit = null;

    // =========================================================================
    // HÀM 1: LẤY CLIENT GỐC (Dùng để up nhạc/ảnh)
    // =========================================================================
    public static Retrofit getClient() {
        if (baseRetrofit == null) {

            // 👉 BƠM 60 GIÂY THỜI GIAN CHỜ CHO VIỆC UPLOAD FILE NẶNG
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();

            baseRetrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(client) // Gắn cái client kiên nhẫn này vào
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return baseRetrofit;
    }

    // =========================================================================
    // HÀM 2: LẤY CLIENT DATABASE (Tự động gắn Header)
    // =========================================================================
    public static Retrofit getSupabaseClient() {
        if (supabaseDatabaseRetrofit == null) {

            // 👉 BƠM THÊM 60 GIÂY VÀO CHỖ CÓ SẴN INTERCEPTOR
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            // 👇 ĐÃ SỬA: Lấy thẻ xịn từ Constants thay vì BuildConfig bị lỗi
                            Request newRequest = chain.request().newBuilder()
                                    .addHeader("apikey", Constants.API_KEY)
                                    .addHeader("Authorization", "Bearer " + Constants.API_KEY)
                                    .build();
                            return chain.proceed(newRequest);
                        }
                    }).build();

            supabaseDatabaseRetrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL + "rest/v1/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return supabaseDatabaseRetrofit;
    }
}