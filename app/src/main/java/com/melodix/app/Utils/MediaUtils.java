package com.melodix.app.Utils;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

public class MediaUtils {

    // Thêm tham số Context vào đây 👇
    public static int getAudioDuration(Context context, Uri audioUri) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            // Nhờ cái Context của Activity để mở file
            mmr.setDataSource(context, audioUri);
            String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                return Integer.parseInt(durationStr) / 1000;
            }
        } catch (Exception e) {
            Log.e("AUDIO_DURATION_ERROR", "Không lấy được duration", e);
        } finally {
            try {
                mmr.release(); // Dùng xong phải trả lại bộ nhớ cho hệ thống
            } catch (Exception e) {
                Log.e("AUDIO_DURATION_ERROR", "Lỗi giải phóng MediaMetadataRetriever", e);
            }
        }
        return 0;
    }
}