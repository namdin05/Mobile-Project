package com.melodix.app.Service;

import android.content.Context;
import android.content.SharedPreferences;

import com.melodix.app.BuildConfig;
import com.melodix.app.Utils.SessionManager;

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

            sharedHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS).addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    SessionManager sessionManager = SessionManager.getInstance(safeContext);

                    String role = sessionManager.getRole();
                    if (role == null) role = "user";

                    String apikey;
                    String authBearer;

                    if ("admin".equals(role)) {
                        apikey = BuildConfig.SERVICE_KEY;
                        authBearer = BuildConfig.SERVICE_KEY;
                    } else {
                        apikey = BuildConfig.API_KEY;
                        String token = sessionManager.getAccessToken();
                        authBearer = (token != null && !token.isEmpty()) ? token : BuildConfig.API_KEY;
                    }

                    // 1. Lấy Request gốc mà API Service gửi xuống
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder();

                    // ==================================================================
                    // 2. CHỐT CHẶN: Chỉ tự động gắn Header nếu Request gốc CHƯA CÓ
                    // ==================================================================
                    if (original.header("apikey") == null) {
                        requestBuilder.addHeader("apikey", apikey);
                    }

                    if (original.header("Authorization") == null) {
                        requestBuilder.addHeader("Authorization", "Bearer " + authBearer);
                    }

                    return chain.proceed(requestBuilder.build());
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