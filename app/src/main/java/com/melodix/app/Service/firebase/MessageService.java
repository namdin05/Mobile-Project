package com.melodix.app.Service.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.melodix.app.Repository.ProfileRepository;

public class MessageService extends FirebaseMessagingService {

    private static final String TAG = "MELODIX_FCM";

    // Hàm này tự động chạy khi Google cấp cho máy 1 cái Token mới
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM Token mới: " + token);

        // 1. KHỞI TẠO REPOSITORY VỚI CONTEXT CỦA SERVICE
        // Dùng getApplicationContext() để đảm bảo an toàn tuyệt đối, không rò rỉ bộ nhớ
        ProfileRepository repository = new ProfileRepository(getApplicationContext());

        // 2. ĐẨY TOKEN LÊN SERVER THÔNG QUA REPO
        repository.updateTokenToServer(token);
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
}