package com.melodix.app.Service.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.melodix.app.MainActivity;
import com.melodix.app.R;
import com.melodix.app.Repository.ProfileRepository;

import java.util.Map;

public class MessageService extends FirebaseMessagingService {

    private static final String TAG = "MELODIX_FCM";
    private static final String CHANNEL_ID = "Melodix_Notification_Channel";

    // Hàm này tự động chạy khi Google cấp cho máy 1 cái Token mới
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM Token mới: " + token);

        // ĐẨY TOKEN LÊN SERVER THÔNG QUA REPO
        ProfileRepository repository = new ProfileRepository(getApplicationContext());
        repository.updateTokenToServer(token);
    }

    // Hàm này chạy khi App đang MỞ trên màn hình mà có thông báo bay tới
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        // ĐỌC TRỰC TIẾP TỪ GÓI DATA PAYLOAD
        Map<String, String> data = message.getData();

        if (data.size() > 0) {
            String title = data.get("title");
            String body = data.get("body");

            Log.d(TAG, "Nhận thông báo ngầm: " + title);

            // Gọi hàm tự vẽ thông báo nổi lên màn hình
            sendNotification(title, body, data);
        }
    }

    // ==========================================
    // HÀM TỰ DỰNG THÔNG BÁO VÀ NHÉT DỮ LIỆU NGẦM
    // ==========================================
    private void sendNotification(String title, String messageBody, Map<String, String> dataPayload) {
        // Tạo chuyến xe chở khách về MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        // Cờ này giúp tái sử dụng MainActivity đang mở thay vì đẻ ra 1 màn hình mới
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // 3. ĐỌC DỮ LIỆU NGẦM TỪ FIREBASE VÀ NHÉT VÀO BALO (EXTRAS)
        if (dataPayload != null && !dataPayload.isEmpty()) {
            String songId = dataPayload.get("song_id");
            String action = dataPayload.get("action");

            intent.putExtra("song_id", songId);
            intent.putExtra("action", action);
        }

        // 4. Bọc Intent bằng PendingIntent (tấm vé đưa cho hệ điều hành giữ dùm)
        // Dùng FLAG_UPDATE_CURRENT để đảm bảo dữ liệu song_id mới nhất đè lên dữ liệu cũ
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // 5. Trang trí hình thức của thông báo (Icon, Âm thanh, Rung...)
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        // BẠN CÓ THỂ ĐỔI ICON Ở ĐÂY. KHUYẾN CÁO DÙNG ICON TRONG SUỐT (PNG)
                        .setSmallIcon(R.drawable.ic_logo)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true) // Bấm vào tự động biến mất
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent); // Gắn tấm vé chứa song_id vào đây!

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // 6. Xây dựng Notification Channel (BẮT BUỘC cho Android 8.0 trở lên)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Thông báo hệ thống Melodix",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Kênh nhận kết quả duyệt bài hát và tin tức");
            notificationManager.createNotificationChannel(channel);
        }

        // 7. Bấm nút "Phát" thông báo ra màn hình
        // Dùng thời gian hiện tại làm ID để các thông báo không đè mất nhau
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}