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
import okhttp3.ResponseBody;

public class ProfileRepository {
    private ProfileAPIService profileAPIService;
    private Context context;
    public ProfileRepository(Context context) {
        profileAPIService = RetrofitClient.getClient(context).create(ProfileAPIService.class);
        this.context = context;
    }

    public MutableLiveData<List<Profile>> fetchAllProfiles() {
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

        profileAPIService.getProfileById("eq." + id).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    profile.setValue(response.body().get(0));
                } else {
                    try {
                        if (response.errorBody() != null) {
                            Log.e("DEBUG_PROFILE", "Chi tiết lỗi từ Supabase: " + response.errorBody().string());
                        } else if (response.body() != null && response.body().isEmpty()) {
                            Log.e("DEBUG_PROFILE", "Lỗi: Database trả về mảng rỗng [] (Không có User nào mang ID này)");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    profile.setValue(null);
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
        String userId = SessionManager.getInstance(context).getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            Log.w("MELODIX_FCM", "Chưa đăng nhập, không lưu Token");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("fcm_token", token);

        profileAPIService.updateFcmToken("eq." + userId, body)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d("MELODIX_FCM", "Đã lưu Token lên Supabase thành công!" + userId);

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
    /**
     * Cập nhật cài đặt quyền riêng tư
     */
    public void updatePrivacySettings(String userId, Map<String, Object> updates) {
        if (userId == null || userId.trim().isEmpty()) {
            Log.w("PRIVACY_SETTINGS", "UserId rỗng, không thể cập nhật");
            return;
        }

        Log.d("PRIVACY_SETTINGS", "Đang cập nhật cài đặt cho user " + userId + ": " + updates);

        ProfileAPIService apiService = RetrofitClient.getClient(context)
                .create(ProfileAPIService.class);

        apiService.updatePrivacySettings("eq." + userId, updates)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d("PRIVACY_SETTINGS", "✓ Cập nhật thành công!");
                        } else {
                            Log.e("PRIVACY_SETTINGS", "✗ Cập nhật thất bại - Code: " + response.code());
                            try {
                                if (response.errorBody() != null) {
                                    Log.e("PRIVACY_SETTINGS", "Error body: " + response.errorBody().string());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("PRIVACY_SETTINGS", "Lỗi mạng: " + t.getMessage(), t);
                    }
                });
    }
    /**
     * Cập nhật thông tin Profile (display_name, avatar_url, ...)
     */

    public void updateProfile(String userId, Map<String, Object> updates) {
        if (userId == null || userId.trim().isEmpty()) {
            Log.w("PROFILE_UPDATE", "UserId rỗng, không thể cập nhật");
            return;
        }

        Log.d("PROFILE_UPDATE", "Đang cập nhật profile cho user " + userId + ": " + updates);

        ProfileAPIService apiService = RetrofitClient.getClient(context)
                .create(ProfileAPIService.class);

        apiService.updateProfile("eq." + userId, updates)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Log.d("PROFILE_UPDATE", "✓ Cập nhật profile thành công!");
                        } else {
                            Log.e("PROFILE_UPDATE", "✗ Cập nhật profile thất bại - Code: " + response.code());
                            try {
                                if (response.errorBody() != null) {
                                    Log.e("PROFILE_UPDATE", "Error body: " + response.errorBody().string());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e("PROFILE_UPDATE", "Lỗi mạng khi cập nhật profile: " + t.getMessage(), t);
                    }
                });
    }

    public interface OnPrivacyUpdateListener {
        void onComplete(boolean success);
    }
    public String getCurrentUserId() {
        return SessionManager.getInstance(context).getUserId();
    }

    public void clearSession() {
        SessionManager.getInstance(context).clear();
    }
}