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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileRepository {
    private ProfileAPIService profileAPIService;
    private final MutableLiveData<List<Profile>> _profiles = new MutableLiveData<>();
    public LiveData<List<Profile>> profiles = _profiles;

    // THÊM BIẾN NÀY ĐỂ DÙNG CHUNG CHO TOÀN BỘ REPO
    private SharedPreferences prefs;

    public ProfileRepository(Context context) {
        profileAPIService = RetrofitClient.getClient(context).create(ProfileAPIService.class);
        // Khởi tạo SharedPreferences ngay từ đầu
        prefs = context.getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);

        fetchAllProfiles();
    }

    public MutableLiveData<List<Profile>> fetchAllProfiles() {
        // ... (Giữ nguyên code của bạn) ...
        profileAPIService.getAllProfiles().enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _profiles.setValue(response.body());
                } else {
                    _profiles.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                _profiles.setValue(null);
            }
        });

        return _profiles;
    }

    public MutableLiveData<Profile> fetchProfileById(String id) {
        // ... (Giữ nguyên code của bạn) ...
        MutableLiveData<Profile> profile = new MutableLiveData<>();
        profileAPIService.getProfileById("eq." + id).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    profile.setValue(response.body().get(0));
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

    public void updateTokenToServer(String token) {
        // ĐÃ SỬA LỖI: Lấy userId từ biến prefs đã khai báo ở Constructor
        String userId = prefs.getString("USER_ID", "");

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
        return prefs.getString("USER_ID", "");
    }

    // Xóa bộ nhớ đệm khi Đăng xuất
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}