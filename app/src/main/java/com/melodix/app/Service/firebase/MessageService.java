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

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM Token mới: " + token);

        ProfileRepository repository = new ProfileRepository(getApplicationContext());
        repository.updateTokenToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Map<String, String> data = message.getData();

        if (data.size() > 0) {
            String title = data.get("title");
            String body = data.get("body");

            Log.d(TAG, "Nhận thông báo ngầm: " + title);

            sendNotification(title, body, data);
        }
    }


    private void sendNotification(String title, String messageBody, Map<String, String> dataPayload) {
        Intent intent = new Intent(this, MainActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (dataPayload != null && !dataPayload.isEmpty()) {
            String songId = dataPayload.get("song_id");
            String action = dataPayload.get("action");

            intent.putExtra("song_id", songId);
            intent.putExtra("action", action);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_logo)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Thông báo hệ thống Melodix",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Kênh nhận kết quả duyệt bài hát và tin tức");
            notificationManager.createNotificationChannel(channel);
        }

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}