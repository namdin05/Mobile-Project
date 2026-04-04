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

import com.melodix.app.Service.AdminAPIService;
import com.melodix.app.Service.AuthAPIService;
import com.melodix.app.Service.BannerAPIService;
import com.melodix.app.Service.GenreAPIService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.melodix.app.BuildConfig;
import com.melodix.app.Service.RetrofitClient;

import java.util.List;

public class AuthRepository {
    private AuthAPIService apiService;

    public AuthRepository() {
        apiService = RetrofitClient.getClient().create(AuthAPIService.class);
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

        Log.d("MELODIX_DEBUG", "Trạm 2");

        apiService.getProfile(BuildConfig.API_KEY, authHeader, idFilter).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // 3. Lấy được role từ database
                    String role = response.body().get(0).getRole();
                    String uid = response.body().get(0).getId();

                    // Truyền thêm uid vào vị trí thứ 3
                    LoginResult successData = new LoginResult(true, role, uid);

                    result.setValue(successData);

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

        apiService.signUpWithEmail(BuildConfig.API_KEY, request).enqueue(new Callback<AuthResponse>() {
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

    public MutableLiveData<LoginResult> handleSocialLogin(String accessToken) {
        MutableLiveData<LoginResult> result = new MutableLiveData<>();
        String apiKey = BuildConfig.API_KEY;
        String authHeader = "Bearer " + accessToken;

        // 1. Gọi API hỏi Supabase xem Access Token này là của UID nào
        apiService.getUserInfo(apiKey, authHeader).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // 2. Bóc tách JSON để lấy chữ "id" (chính là UID)
                        String jsonString = response.body().string();
                        org.json.JSONObject jsonObject = new org.json.JSONObject(jsonString);
                        String uid = jsonObject.getString("id");

                        // 3. Quá đỉnh! Dùng lại chính hàm fetchUserRole của bạn để lấy Role và trả về LoginResult
                        fetchUserRole(uid, accessToken, result);

                    } catch (Exception e) {
                        e.printStackTrace();
                        result.setValue(new LoginResult(false, "Lỗi phân tích dữ liệu User từ Supabase", true));
                    }
                } else {
                    result.setValue(new LoginResult(false, "Token Mạng xã hội không hợp lệ hoặc hết hạn", true));
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                result.setValue(new LoginResult(false, "Lỗi kết nối mạng khi xác thực MXH", true));
            }
        });

        return result;
    }
}