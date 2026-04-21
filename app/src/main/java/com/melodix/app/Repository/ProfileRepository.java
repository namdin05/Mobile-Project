package com.melodix.app.Repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Profile;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Utils.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileRepository {
    private ProfileAPIService profileAPIService;
    private Context context;
    public ProfileRepository(Context context) {
        profileAPIService = RetrofitClient.getClient(context).create(ProfileAPIService.class);
        this.context = context;
    }

    public MutableLiveData<List<Profile>> fetchAllProfiles() {
        // ... (Giữ nguyên code của bạn) ...
        MutableLiveData<List<Profile>> profile = new MutableLiveData<>();
        profileAPIService.getAllProfiles().enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    profile.setValue(response.body());
                } else {
                    profile.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                profile.setValue(null);
            }
        });

        return profile;
    }

    public MutableLiveData<Profile> fetchProfileById(String id) {
        MutableLiveData<Profile> profile = new MutableLiveData<>();

        Log.e("DEBUG_PROFILE", "1. Đang gọi API lấy Profile cho UID: " + id);

        profileAPIService.getProfileById("eq." + id).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Log.e("DEBUG_PROFILE", "2. Lấy Profile thành công!");
                    profile.setValue(response.body().get(0));
                } else {
                    Log.e("DEBUG_PROFILE", "2. LỖI API! Mã lỗi: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e("DEBUG_PROFILE", "Chi tiết lỗi từ Supabase: " + response.errorBody().string());
                        } else if (response.body() != null && response.body().isEmpty()) {
                            Log.e("DEBUG_PROFILE", "Lỗi: Database trả về mảng rỗng [] (Không có User nào mang ID này)");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    profile.setValue(null); // API lỗi nên trả về null
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                Log.e("DEBUG_PROFILE", "2. LỖI MẠNG hoặc sập Server: " + t.getMessage());
                profile.setValue(null);
            }
        });

        return profile;
    }

    public void getProfileById(String userId, Callback<List<Profile>> callback) {
        profileAPIService.getProfileById("eq." + userId).enqueue(callback);
    }

    public void updateTokenToServer(String token) {
        // ĐÃ SỬA LỖI: Lấy userId từ biến prefs đã khai báo ở Constructor
        String userId = SessionManager.getInstance(context).getUserId();

        if (userId.isEmpty()) {
            Log.w("MELODIX_FCM", "Chưa đăng nhập, không lưu Token");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("fcm_token", token);

        profileAPIService.updateFcmToken("eq." + userId, body)
                .enqueue(new Callback<Void>() {
                    // ... (Giữ nguyên code xử lý response của bạn) ...
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d("MELODIX_FCM", "Đã lưu Token lên Supabase thành công!");
                        } else {
                            Log.e("MELODIX_FCM", "Lưu Token thất bại, mã lỗi: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("MELODIX_FCM", "Lỗi mạng khi lưu Token: " + t.getMessage());
                    }
                });
    }

    // =======================================================
    // 2 HÀM MỚI BỔ SUNG ĐỂ PHỤC VỤ CHO ADMIN (VÀ CẢ USER)
    // =======================================================

    // Lấy ID người dùng đang đăng nhập hiện tại
    public String getCurrentUserId() {
        return SessionManager.getInstance(context).getUserId();
    }

    // Xóa bộ nhớ đệm khi Đăng xuất
    public void clearSession() {
        SessionManager.getInstance(context).clear();
    }
}