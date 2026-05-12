package com.melodix.app.Service;

import android.content.Context;
import android.util.Log;

import com.melodix.app.BuildConfig;
import com.melodix.app.Model.AuthResponse; 
import com.melodix.app.Utils.SessionManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static OkHttpClient sharedHttpClient = null;

    private static Retrofit databaseRetrofit = null;
    private static Retrofit storageRetrofit = null;
    private static Retrofit authRetrofit = null;

    private static OkHttpClient getSharedHttpClient(Context context) {
        if (sharedHttpClient == null) {
            final Context safeContext = context.getApplicationContext();

            sharedHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)

                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            SessionManager sessionManager = SessionManager.getInstance(safeContext);

                            String role = sessionManager.getRole();
                            if (role == null) role = "user";

                            
                            Request original = chain.request();
                            String path = original.url().encodedPath();

                            Log.e("RetrofitClient", "path: " + path);

                            String apikey;
                            String authBearer;
                            String userToken = sessionManager.getAccessToken();

                            if ("admin".equals(role)) {
                                apikey = BuildConfig.SERVICE_KEY;

                                
                                if (path.startsWith("/auth/v1/admin/")) {
                                    
                                    authBearer = BuildConfig.SERVICE_KEY;
                                } else if (path.startsWith("/auth")) {
                                    
                                    authBearer = (userToken != null && !userToken.isEmpty()) ? userToken : BuildConfig.SERVICE_KEY;
                                    Log.e("RetrofitClient", "userToken: " + authBearer);
                                } else {
                                    
                                    authBearer = BuildConfig.SERVICE_KEY;
                                }
                            } else {
                                
                                apikey = BuildConfig.API_KEY;
                                authBearer = (userToken != null && !userToken.isEmpty()) ? userToken : BuildConfig.API_KEY;
                            }

                            Request.Builder requestBuilder = original.newBuilder();

                            if (original.header("apikey") == null) {
                                requestBuilder.addHeader("apikey", apikey);
                            }

                            if (original.header("Authorization") == null) {
                                requestBuilder.addHeader("Authorization", "Bearer " + authBearer);
                            }

                            return chain.proceed(requestBuilder.build());
                        }
                    })

                    
                    .authenticator(new Authenticator() {
                        @Override
                        public Request authenticate(Route route, Response response) throws IOException {
                            
                            if (response.priorResponse() != null) {
                                return null;
                            }

                            SessionManager sessionManager = SessionManager.getInstance(safeContext);
                            String refreshToken = sessionManager.getRefreshToken(); 

                            if (refreshToken == null || refreshToken.isEmpty()) {
                                return null;
                            }

                            
                            AuthAPIService authService = getAuth(safeContext).create(AuthAPIService.class);

                            Map<String, String> body = new HashMap<>();
                            body.put("refresh_token", refreshToken);

                            
                            retrofit2.Response<AuthResponse> refreshCall = authService.refreshToken(body).execute();

                            if (refreshCall.isSuccessful() && refreshCall.body() != null) {
                                
                                String newAccessToken = refreshCall.body().getAccessToken();
                                String newRefreshToken = refreshCall.body().getRefreshToken();

                                sessionManager.updateTokens(newAccessToken, newRefreshToken); 

                                
                                return response.request().newBuilder()
                                        .header("Authorization", "Bearer " + newAccessToken)
                                        .build();
                            } else {
                                
                                
                                sessionManager.clear();
                                return null;
                            }
                        }
                    })
                    .build();
        }
        return sharedHttpClient;
    }


    public static Retrofit getClient(Context context) {
        if (databaseRetrofit == null) {
            databaseRetrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL + "rest/v1/")
                    .client(getSharedHttpClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return databaseRetrofit;
    }

    public static Retrofit getStorage(Context context) {
        if (storageRetrofit == null) {
            storageRetrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL + "storage/v1/object/")
                    .client(getSharedHttpClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return storageRetrofit;
    }


    public static Retrofit getAuth(Context context) {
        if (authRetrofit == null) {
            authRetrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL + "auth/v1/")
                    .client(getSharedHttpClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return authRetrofit;
    }
}