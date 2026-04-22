package com.melodix.app.Utils;

import android.content.Context;
import android.widget.Toast;

import com.melodix.app.Model.Playlist;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Service.AudioPlayerService;

import java.util.ArrayList;

public class AppUiUtils {
    public static void toast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showAddToPlaylistDialog(Context context, AppRepository repository, String songId) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.BottomSheetTheme);

        android.view.View sheetView = android.view.LayoutInflater.from(context).inflate(R.layout.layout_bottom_sheet_add_playlist, null);
        dialog.setContentView(sheetView);

        android.view.View parent = (android.view.View) sheetView.getParent();
        if (parent != null) {
            parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        android.widget.LinearLayout container = sheetView.findViewById(R.id.ll_playlist_container);
        ArrayList<Playlist> playlists = repository.getCurrentUserPlaylists();

        for (Playlist playlist : playlists) {
            android.widget.TextView tv = new android.widget.TextView(context);
            tv.setText(playlist.name);
            tv.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.mdx_text));
            tv.setTextSize(16);

            int padH = (int) (24 * context.getResources().getDisplayMetrics().density);
            int padV = (int) (14 * context.getResources().getDisplayMetrics().density);
            tv.setPadding(padH, padV, padH, padV);

            android.util.TypedValue outValue = new android.util.TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            tv.setBackgroundResource(outValue.resourceId);

            // Bỏ comment nút thêm vào Playlist (Nếu bạn muốn dùng sau này)
            /*
            tv.setOnClickListener(v -> {
                repository.addSongToPlaylist(playlist.id, songId);
                Toast.makeText(context, "Added to " + playlist.name, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            */

            container.addView(tv);
        }

        dialog.show();
    }

    // ==========================================
    // TÍNH NĂNG TỐC ĐỘ PHÁT NHẠC (ĐÃ THÊM LOGIC ĐỔI MÀU XANH)
    // ==========================================
    public static void showSpeedDialog(Context context) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.BottomSheetTheme);

        android.view.View view = android.view.LayoutInflater.from(context)
                .inflate(R.layout.dialog_speed_menu, null);
        bottomSheet.setContentView(view);

        android.view.View parent = (android.view.View) view.getParent();
        if (parent != null) {
            parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        // ==========================================
        // DÙNG APPLICATION_CONTEXT ĐỂ CHỐNG LỆCH PHA GIỮA CÁC FRAGMENT
        // ==========================================
        android.content.SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        float currentSpeed = prefs.getFloat("saved_speed", 1.0f); // Mặc định là 1.0f

        int colorPrimary = androidx.core.content.ContextCompat.getColor(context, R.color.mdx_primary);
        int colorNormal = androidx.core.content.ContextCompat.getColor(context, R.color.mdx_text);

        bindSpeed(view, bottomSheet, context, R.id.menu_speed_05, 0.5f, "0.5x", currentSpeed, colorPrimary, colorNormal);
        bindSpeed(view, bottomSheet, context, R.id.menu_speed_10, 1.0f, "1.0x", currentSpeed, colorPrimary, colorNormal);
        bindSpeed(view, bottomSheet, context, R.id.menu_speed_125, 1.25f, "1.25x", currentSpeed, colorPrimary, colorNormal);
        bindSpeed(view, bottomSheet, context, R.id.menu_speed_15, 1.5f, "1.5x", currentSpeed, colorPrimary, colorNormal);
        bindSpeed(view, bottomSheet, context, R.id.menu_speed_20, 2.0f, "2.0x", currentSpeed, colorPrimary, colorNormal);

        bottomSheet.show();
    }

    private static void bindSpeed(android.view.View view,
                                  com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet,
                                  Context context,
                                  int viewId,
                                  float targetSpeed,
                                  String label,
                                  float currentSpeed,
                                  int colorPrimary,
                                  int colorNormal) {

        android.widget.TextView tv = view.findViewById(viewId);

        // ==========================================
        // KHẮC PHỤC LỖI SO SÁNH FLOAT TRONG JAVA BẰNG Math.abs
        // (Kiểm tra xem 2 số trừ đi nhau có gần bằng 0 không)
        // ==========================================
        if (Math.abs(currentSpeed - targetSpeed) < 0.01f) {
            tv.setTextColor(colorPrimary);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            tv.setTextColor(colorNormal);
            tv.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        tv.setOnClickListener(v -> {
            // LƯU BẰNG APPLICATION CONTEXT
            android.content.SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
            prefs.edit().putFloat("saved_speed", targetSpeed).apply();

            PlaybackUtils.setSpeed(context, targetSpeed);
            toast(context, "Speed set to " + label);
            bottomSheet.dismiss();
        });
    }

    // ==========================================
    // TÍNH NĂNG HẸN GIỜ TẮT NHẠC
    // ==========================================
    public static void showSleepTimerDialog(Context context) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.BottomSheetTheme);

        android.view.View view = android.view.LayoutInflater.from(context)
                .inflate(R.layout.dialog_sleep_timer_menu, null);
        bottomSheet.setContentView(view);

        android.view.View parent = (android.view.View) view.getParent();
        if (parent != null) {
            parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        bindTimer(view, bottomSheet, context, R.id.menu_timer_off, 0, "Off");
        bindTimer(view, bottomSheet, context, R.id.menu_timer_15, 15, "15 minutes");
        bindTimer(view, bottomSheet, context, R.id.menu_timer_30, 30, "30 minutes");
        bindTimer(view, bottomSheet, context, R.id.menu_timer_60, 60, "60 minutes");

        bottomSheet.show();
    }

    private static void bindTimer(android.view.View view,
                                  com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet,
                                  Context context,
                                  int viewId,
                                  int minutes,
                                  String label) {
        view.findViewById(viewId).setOnClickListener(v -> {
            PlaybackUtils.setSleepTimer(context, minutes);
            toast(context, minutes == 0 ? "Sleep timer off" : "Sleep timer: " + label);
            bottomSheet.dismiss();
        });
    }
}