package com.melodix.app.Service;

import android.content.Context;
import android.content.SharedPreferences;

import com.melodix.app.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static OkHttpClient sharedHttpClient = null;

    private static Retrofit databaseRetrofit = null;
    private static Retrofit storageRetrofit = null;
    private static Retrofit authRetrofit = null;

    // =========================================================================
    // HÀM CHUNG: SETUP OKHTTP CLIENT (Gắn Chìa Khóa + Token)
    // =========================================================================
    private static OkHttpClient getSharedHttpClient(Context context) {
        if (sharedHttpClient == null) {
            final Context safeContext = context.getApplicationContext();

            sharedHttpClient = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    SharedPreferences prefs = safeContext.getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);

                    String role = prefs.getString("USER_ROLE", "user");

                    String apikey;
                    String authBearer;

                    if ("admin".equals(role)) {
                        // NẾU LÀ ADMIN: Dùng SERVICE_KEY cho cả 2 để bypass RLS (bỏ qua quyền truy cập)
                        apikey = BuildConfig.SERVICE_KEY;
                        authBearer = BuildConfig.SERVICE_KEY;
                    } else {
                        // NẾU LÀ USER BÌNH THƯỜNG: Dùng API_KEY và Token của User
                        apikey = BuildConfig.API_KEY;
                        authBearer = prefs.getString("ACCESS_TOKEN", BuildConfig.API_KEY);
                    }

                    Request newRequest = chain.request().newBuilder()
                            .addHeader("apikey", apikey)
                            .addHeader("Authorization", "Bearer " + authBearer)
                            .build();

                    return chain.proceed(newRequest);
                }
            }).build();
        }
        return sharedHttpClient;
    }

    // =========================================================================
    // NHÁNH 1: DATABASE (rest/v1/)
    // =========================================================================
    public static Retrofit getClient(Context context) {
        if (databaseRetrofit == null) {
            databaseRetrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL + "rest/v1/") // Đuôi rest
                    .client(getSharedHttpClient(context))       // Nhét bộ gắn Key chung vào
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return databaseRetrofit;
    }

    // =========================================================================
    // NHÁNH 2: STORAGE (storage/v1/)
    // =========================================================================
    public static Retrofit getStorage(Context context) {
        if (storageRetrofit == null) {
            storageRetrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL + "storage/v1/object/") // Đuôi storage
                    .client(getSharedHttpClient(context))          // Nhét bộ gắn Key chung vào
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return storageRetrofit;
    }

    // =========================================================================
    // NHÁNH 3: AUTH (auth/v1/)
    // =========================================================================
    public static Retrofit getAuth(Context context) {
        if (authRetrofit == null) {
            authRetrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL + "auth/v1/") // Đuôi auth
                    .client(getSharedHttpClient(context))       // Nhét bộ gắn Key chung vào
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return authRetrofit;
    }
}