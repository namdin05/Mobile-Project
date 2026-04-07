package com.melodix.app.Service;

import com.melodix.app.BuildConfig;

import java.io.IOException;

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
    // HÀM 1: LẤY CLIENT GỐC (Thay thế cho RetrofitClient.getClient() cũ)
    // =========================================================================
    public static Retrofit getClient() {
        if (baseRetrofit == null) {
            baseRetrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return baseRetrofit;
    }

    // =========================================================================
    // HÀM 2: LẤY CLIENT DATABASE (Thay thế cho SupabaseClient.getClient() cũ)
    // =========================================================================
    public static Retrofit getSupabaseClient() {
        if (supabaseDatabaseRetrofit == null) {

            // Bộ đánh chặn tự động gắn Header
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request newRequest = chain.request().newBuilder()
                            .addHeader("apikey", BuildConfig.API_KEY)
                            .addHeader("Authorization", "Bearer " + BuildConfig.API_KEY)
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