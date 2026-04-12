package com.melodix.app.Service.firebase; // Sửa lại package cho đúng

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.melodix.app.BuildConfig;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageService extends FirebaseMessagingService {

    private static final String TAG = "MELODIX_FCM";

    // Hàm này tự động chạy khi Google cấp cho máy 1 cái Token mới
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM Token mới: " + token);

        // Gửi token mới lên Supabase
        sendTokenToSupabase(token);
    }

    // Hàm này chạy khi App đang mở trên màn hình mà có thông báo bay tới
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        // Firebase mặc định sẽ tự hiện thông báo nếu App đang tắt (background).
        // Nếu App đang mở (foreground), nó sẽ nhả data vào đây để bạn tự xử lý (vd: show Toast).
        if (message.getNotification() != null) {
            Log.d(TAG, "Nhận thông báo: " + message.getNotification().getTitle());
        }
    }

    private void sendTokenToSupabase(String token) {
        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "");

        if (userId.isEmpty()) return; // Chưa đăng nhập thì thôi

        ProfileAPIService apiService = RetrofitClient.getClient().create(ProfileAPIService.class);

        // Đóng gói data
        Map<String, Object> body = new HashMap<>();
        body.put("fcm_token", token);

        // Gọi API cập nhật
        String apiKey = BuildConfig.SERVICE_KEY; // Hoặc Anon Key tùy bảo mật của bạn
        String authHeader = "Bearer " + BuildConfig.SERVICE_KEY;

        apiService.updateFcmToken(apiKey, authHeader, "eq." + userId, body)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) Log.d(TAG, "Cập nhật Token thành công!");
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "Lỗi cập nhật Token: " + t.getMessage());
                    }
                });
    }
}
