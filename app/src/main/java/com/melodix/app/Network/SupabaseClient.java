package com.melodix.app.Network; // Thay bằng package của bạn

import com.melodix.app.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;



public class SupabaseClient {

    private static Retrofit retrofit = null;



    public static Retrofit getClient() {

        if (retrofit == null) {

            // Tạo bộ đánh chặn để tự động gắn API Key vào mọi request

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



            // Khởi tạo Retrofit

            // Supabase REST API luôn có hậu tố /rest/v1/ ở cuối Base URL

            retrofit = new Retrofit.Builder()

                    .baseUrl(BuildConfig.BASE_URL + "rest/v1/")

                    .client(client)

                    .addConverterFactory(GsonConverterFactory.create())

                    .build();

        }

        return retrofit;

    }

}