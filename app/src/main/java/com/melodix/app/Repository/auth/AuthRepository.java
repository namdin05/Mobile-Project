package com.melodix.app.Repository.auth;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.melodix.app.Model.AuthResponse;
import com.melodix.app.Model.Banner;
import com.melodix.app.Model.Genre;
import com.melodix.app.Model.SessionManager;
import com.melodix.app.Model.SignInRequest;
import com.melodix.app.Model.SignUpRequest;
import com.melodix.app.Model.LoginResult;
import com.melodix.app.Model.Profile;
import com.melodix.app.Model.SessionManager;

import com.melodix.app.Model.Song;
import com.melodix.app.Service.AuthAPIService;
import com.melodix.app.Service.BannerAPIService;
import com.melodix.app.Service.GenreAPIService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.melodix.app.BuildConfig;
import com.melodix.app.Service.SongAPIService;

import java.util.List;

public class AuthRepository {
    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static final String API_KEY = BuildConfig.API_KEY;

    private AuthAPIService apiService;
    private GenreAPIService genreAPIService;
    private SongAPIService songAPIService;
    private BannerAPIService bannerAPIService;

    public AuthRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(AuthAPIService.class);
        genreAPIService = retrofit.create(GenreAPIService.class);
        songAPIService = retrofit.create(SongAPIService.class);
        bannerAPIService = retrofit.create(BannerAPIService.class);
    }

    // Trả về MutableLiveData để ViewModel quan sát
    public MutableLiveData<LoginResult> signIn(String email, String password, Context context) {
        MutableLiveData<LoginResult> result = new MutableLiveData<>();
        SignInRequest request = new SignInRequest(email, password);

        apiService.signInWithEmail(BuildConfig.API_KEY, request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 1. Đăng nhập thành công, lấy Token và User ID
                    String token = response.body().getAccessToken();
                    String userId = response.body().getUser().getId();

                    // 2. Gọi API hỏi xem ông này role gì
                    fetchUserRole(userId, token, result);
                    fetchCurrentUser(userId, token, context);

                    // Log.d("test_u", new Gson().toJson(response.body().getUser()));

                    SessionManager.getInstance(context).saveLogInSession(response.body().getUser(), token);
                } else {
                    result.setValue(new LoginResult(false, "Sai tài khoản hoặc mật khẩu", true));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.setValue(new LoginResult(false, "Lỗi kết nối mạng", true));
            }
        });

        return result;
    }

    private void fetchUserRole(String userId, String token, MutableLiveData<LoginResult> result) {
        String authHeader = "Bearer " + token;
        String idFilter = "eq." + userId; // Cú pháp lọc của Supabase: id = userId

        apiService.getProfile(BuildConfig.API_KEY, authHeader, idFilter).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // 3. Lấy được role từ database
                    String role = response.body().get(0).getRole();

                    // Trả kết quả cuối cùng về cho Activity
                    result.setValue(new LoginResult(true, role));
                } else {
                    // Ép hệ thống in ra chi tiết lỗi
                    int statusCode = response.code();
                    Log.e("AUTH ROLE", "Thất bại với HTTP Code: " + statusCode);

                    try {
                        if (response.errorBody() != null) {
                            // Lỗi do sai quyền, sai cấu trúc API (Code 400, 401, 403...)
                            String errorMsg = response.errorBody().string();
                            Log.e("AUTH ROLE", "Chi tiết Supabase: " + errorMsg);
                        } else if (response.body() != null && response.body().isEmpty()) {
                            // Lỗi gọi thành công (Code 200) nhưng Database không có dữ liệu trả về []
                            Log.e("AUTH ROLE", "Lỗi: Tìm không thấy Profile nào có ID này trong bảng profiles!");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    result.setValue(new LoginResult(true, "user")); // Tạm thời vẫn cho vào app
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                result.setValue(new LoginResult(false, "Lỗi tải thông tin phân quyền", true));
            }
        });
    }

    public MutableLiveData<Profile> fetchCurrentUser(String userId, String token, Context context){
        MutableLiveData<Profile> current_user = new MutableLiveData<>();
        String modified_token = "Bearer " + token;
        String modified_user_id = "eq." + userId;

        apiService.getProfile(API_KEY, modified_token, modified_user_id).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if(response.isSuccessful() && response.body() != null){
                    Profile profile = response.body().get(0);
                    current_user.setValue(profile);
                    SessionManager.getInstance(context).saveLogInSession(profile, token);
                } else {
                    current_user.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                Log.e("FETCH_CURRENT_USER", t.getMessage());
                current_user.setValue(null);
            }
        });
        return current_user;
    }

    public MutableLiveData<String> signUp(String email, String password, String fullName) {
        MutableLiveData<String> registerResult = new MutableLiveData<>();
        SignUpRequest request = new SignUpRequest(email, password, fullName);

        apiService.signUpWithEmail(API_KEY, request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Đăng ký thành công
                    registerResult.setValue("SUCCESS");
                } else {
                    // Thất bại (có thể do Email đã tồn tại hoặc mật khẩu quá ngắn)
                    registerResult.setValue("ERROR: Email đã tồn tại hoặc không hợp lệ!");
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                registerResult.setValue("ERROR: Lỗi kết nối mạng");
            }
        });

        return registerResult;
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

    public MutableLiveData<List<Song>> fetchNewReleaseSongs(){
        MutableLiveData<List<Song>> songs = new MutableLiveData<>();

        songAPIService.getNewReleaseSongs(API_KEY, 5).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if(response.isSuccessful() && response.body() != null){
                    songs.setValue(response.body());
                    Log.d("NEW_RELEASE_SONGS", new Gson().toJson(response.body()));
                } else {
                    songs.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                Log.e("API ERROR", "Loi mang: " + t.getMessage());
                songs.setValue(null);
            }
        });
        return songs;
    }

    public MutableLiveData<List<Song>> fetchTrendingSongs(){
        MutableLiveData<List<Song>> songs = new MutableLiveData<>();

        songAPIService.getTrendingSongs(API_KEY, 5).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if(response.isSuccessful() && response.body() != null){
                    Log.d("TRENDING", new Gson().toJson(response.body()));
                    songs.setValue(response.body());
                } else {
                    Log.d("TRENDING", "K co bai hat trend");
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                Log.d("TRENDING", "fail");
            }
        });
        return songs;
    }

    public MutableLiveData<List<Banner>> fetchBanners(){
        MutableLiveData<List<Banner>> banners = new MutableLiveData<>();

        bannerAPIService.getBanners(API_KEY).enqueue(new Callback<List<Banner>>() {
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