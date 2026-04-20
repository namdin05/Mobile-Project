package com.melodix.app.Repository.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Model.AuthResponse;
import com.melodix.app.Model.LoginResult;
import com.melodix.app.Model.Profile;
import com.melodix.app.Model.SessionManager;
import com.melodix.app.Model.SignInRequest;
import com.melodix.app.Model.SignUpRequest;
import com.melodix.app.Service.AuthAPIService;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private AuthAPIService apiService;
    private ProfileAPIService profileAPISerivce;

    public AuthRepository(Context context) {
        // Dùng nhánh Auth
        apiService = RetrofitClient.getAuth(context).create(AuthAPIService.class);
        profileAPISerivce = RetrofitClient.getClient(context).create(ProfileAPIService.class);
    }

    public MutableLiveData<LoginResult> signIn(String email, String password, Context context) {
        MutableLiveData<LoginResult> result = new MutableLiveData<>();
        SignInRequest request = new SignInRequest(email, password);

        apiService.signInWithEmail(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    // 1. Lấy Token và UID
                    String token = response.body().getAccessToken();
                    String userId = response.body().getUser().getId();

                    Log.e("TOKEN", token);

                    // 2. BẮT BUỘC: Lưu ngay Token vào SharedPreferences ĐỂ INTERCEPTOR XÀI CHO CÁC API SAU
                    try {
                        SharedPreferences prefs = context.getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
                        prefs.edit().putString("ACCESS_TOKEN", token).apply();
                        Log.e("DEBUG", "Lưu SharedPreferences thành công!");
                    } catch (Exception e) {
                        Log.e("DEBUG_ERROR", "Lỗi khi lưu SharedPreferences: " + e.getMessage());
                        e.printStackTrace();
                    }



                    // 3. Gọi API lấy Profile (Không cần truyền API_KEY hay Token nữa)
                    fetchUserRoleAndProfile(userId, token, context, result);

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

    public MutableLiveData<String> signUp(String email, String password, String fullName) {
        MutableLiveData<String> registerResult = new MutableLiveData<>();
        SignUpRequest request = new SignUpRequest(email, password, fullName);

        apiService.signUpWithEmail(request).enqueue(new Callback<AuthResponse>() {
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

    // Gộp 2 hàm fetchUserRole và fetchCurrentUser thành 1 cho tối ưu
    private void fetchUserRoleAndProfile(String userId, String token, Context context, MutableLiveData<LoginResult> result) {
        String idFilter = "eq." + userId;

        // XÓA SẠCH tham số API_KEY và AuthHeader. Chỉ cần truyền idFilter!
        // (Nhớ mở file AuthAPIService.java xóa mấy cái @Header đi luôn nhé)
        profileAPISerivce.getProfileById(idFilter).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Profile profile = response.body().get(0);
                    String role = profile.getRole();
                    String uid = profile.getId();

                    // Lưu thông tin người dùng vào SessionManager như cũ
                    SessionManager.getInstance(context).saveLogInSession(profile, token);

                    // Trả kết quả về cho Activity
                    result.setValue(new LoginResult(true, role, uid));
                } else {
                    Log.e("AUTH ROLE", "Lỗi tải Profile: " + response.code());
                    result.setValue(new LoginResult(false, "Không lấy được thông tin Profile", true));
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                result.setValue(new LoginResult(false, "Lỗi tải thông tin phân quyền", true));
            }
        });
    }

    // ... (Giữ nguyên hàm signUp) ...

    public MutableLiveData<LoginResult> handleSocialLogin(String accessToken, Context context) {
        MutableLiveData<LoginResult> result = new MutableLiveData<>();

        // LƯU NGAY TOKEN MXH VÀO ĐỂ INTERCEPTOR LÀM VIỆC
        SharedPreferences prefs = context.getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("ACCESS_TOKEN", accessToken).apply();

        apiService.getUserInfo().enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        org.json.JSONObject jsonObject = new org.json.JSONObject(jsonString);
                        String uid = jsonObject.getString("id");

                        // Gọi lại hàm gộp
                        fetchUserRoleAndProfile(uid, accessToken, context, result);

                    } catch (Exception e) {
                        result.setValue(new LoginResult(false, "Lỗi phân tích dữ liệu", true));
                    }
                } else {
                    result.setValue(new LoginResult(false, "Token MXH không hợp lệ", true));
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                result.setValue(new LoginResult(false, "Lỗi kết nối mạng", true));
            }
        });
        return result;
    }
}