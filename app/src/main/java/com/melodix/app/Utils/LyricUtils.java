package com.melodix.app.Utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.melodix.app.Model.LyricLine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricUtils {

    // Tạo một interface để callback khi tải xong
    public interface LyricCallback {
        void onLyricsLoaded(ArrayList<LyricLine> lyrics);
    }

    public static void downloadAndParseLrc(String lrcUrl, LyricCallback callback) {
        if (lrcUrl == null || lrcUrl.isEmpty()) {
            callback.onLyricsLoaded(new ArrayList<>());
            return;
        }

        // Chạy một luồng phụ để tải mạng
        new Thread(() -> {
            ArrayList<LyricLine> lyricsList = new ArrayList<>();
            try {
                URL url = new URL(lrcUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                String line;

                // Mẫu regex của bạn giữ nguyên
                Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");

                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        int min = Integer.parseInt(matcher.group(1));
                        int sec = Integer.parseInt(matcher.group(2));
                        int millis = Integer.parseInt(matcher.group(3));

                        if (matcher.group(3).length() == 2) millis *= 10;

                        long totalTimeMs = (min * 60 * 1000) + (sec * 1000) + millis;
                        String text = matcher.group(4).trim();

                        // Tránh thêm dòng trống nếu không có chữ
                        if (!text.isEmpty()) {
                            lyricsList.add(new LyricLine(totalTimeMs, text));
                        }
                    }
                }
                reader.close();

            } catch (Exception e) {
                Log.e("LYRICS", "Lỗi tải file LRC từ mạng: " + e.getMessage());
            }

            // Đưa kết quả về luồng chính (Main Thread) để cập nhật UI
            new Handler(Looper.getMainLooper()).post(() -> {
                callback.onLyricsLoaded(lyricsList);
            });

        }).start();
    }
}