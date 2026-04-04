package com.melodix.app.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.EditText;
import android.widget.Toast;

import com.melodix.app.Model.Playlist;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;

import java.util.ArrayList;

public class AppUiUtils {
    public static void toast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showAddToPlaylistDialog(Context context, AppRepository repository, String songId) {
        // 1. Bổ sung R.style.BottomSheetTheme giống các dialog khác
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.BottomSheetTheme);

        android.view.View sheetView = android.view.LayoutInflater.from(context).inflate(R.layout.layout_bottom_sheet_add_playlist, null);
        dialog.setContentView(sheetView);

        // 2. Dùng cách xóa viền bằng getParent() y hệt như hàm Speed và Timer
        android.view.View parent = (android.view.View) sheetView.getParent();
        if (parent != null) {
            parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        android.widget.LinearLayout container = sheetView.findViewById(R.id.ll_playlist_container);
        ArrayList<Playlist> playlists = repository.getCurrentUserPlaylists();

        // Tự động tạo từng dòng TextView tương ứng với mỗi playlist
        for (Playlist playlist : playlists) {
            android.widget.TextView tv = new android.widget.TextView(context);
            tv.setText(playlist.name);
            tv.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.mdx_text));
            tv.setTextSize(16);

            // Set padding chuẩn form (24dp ngang, 14dp dọc)
            int padH = (int) (24 * context.getResources().getDisplayMetrics().density);
            int padV = (int) (14 * context.getResources().getDisplayMetrics().density);
            tv.setPadding(padH, padV, padH, padV);

            // Set hiệu ứng Ripple khi chạm
            android.util.TypedValue outValue = new android.util.TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            tv.setBackgroundResource(outValue.resourceId);

            // Bắt sự kiện bấm vào thì thêm bài hát
//            tv.setOnClickListener(v -> {
//                repository.addSongToPlaylist(playlist.id, songId);
//                Toast.makeText(context, "Added to " + playlist.name, Toast.LENGTH_SHORT).show();
//                dialog.dismiss();
//            });

            container.addView(tv);
        }

        dialog.show();
    }

//    public static void showSpeedDialog(Context context) {
//        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
//                new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.BottomSheetTheme);
//
//        android.view.View view = android.view.LayoutInflater.from(context)
//                .inflate(R.layout.dialog_speed_menu, null);
//        bottomSheet.setContentView(view);
//
//
//        android.view.View parent = (android.view.View) view.getParent();
//        if (parent != null) {
//            parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
//        }
//
//        bindSpeed(view, bottomSheet, context, R.id.menu_speed_05, 0.5f, "0.5x");
//        bindSpeed(view, bottomSheet, context, R.id.menu_speed_10, 1.0f, "1.0x");
//        bindSpeed(view, bottomSheet, context, R.id.menu_speed_125, 1.25f, "1.25x");
//        bindSpeed(view, bottomSheet, context, R.id.menu_speed_15, 1.5f, "1.5x");
//        bindSpeed(view, bottomSheet, context, R.id.menu_speed_20, 2.0f, "2.0x");
//
//        bottomSheet.show();
//    }

//    private static void bindSpeed(android.view.View view,
//                                  com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet,
//                                  Context context,
//                                  int viewId,
//                                  float speed,
//                                  String label) {
//        view.findViewById(viewId).setOnClickListener(v -> {
//            PlaybackUtils.setSpeed(context, speed);
//            toast(context, "Speed set to " + label);
//            bottomSheet.dismiss();
//        });
//    }

//    public static void showSleepTimerDialog(Context context) {
//        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
//                new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.BottomSheetTheme);
//
//        android.view.View view = android.view.LayoutInflater.from(context)
//                .inflate(R.layout.dialog_sleep_timer_menu, null);
//        bottomSheet.setContentView(view);
//
//        android.view.View parent = (android.view.View) view.getParent();
//        if (parent != null) {
//            parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
//        }
//
//        bindTimer(view, bottomSheet, context, R.id.menu_timer_off, 0, "Off");
//        bindTimer(view, bottomSheet, context, R.id.menu_timer_15, 15, "15 minutes");
//        bindTimer(view, bottomSheet, context, R.id.menu_timer_30, 30, "30 minutes");
//        bindTimer(view, bottomSheet, context, R.id.menu_timer_60, 60, "60 minutes");
//
//        bottomSheet.show();
//    }
//
//    private static void bindTimer(android.view.View view,
//                                  com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet,
//                                  Context context,
//                                  int viewId,
//                                  int minutes,
//                                  String label) {
//        view.findViewById(viewId).setOnClickListener(v -> {
//            PlaybackUtils.setSleepTimer(context, minutes);
//            toast(context, minutes == 0 ? "Sleep timer off" : "Sleep timer: " + label);
//            bottomSheet.dismiss();
//        });
//    }

//    public static void showCreatePlaylistDialog(Context context, AppRepository repository, Runnable onSuccess) {
//        android.view.View dialogView = android.view.LayoutInflater.from(context).inflate(R.layout.dialog_create_playlist, null);
//        AlertDialog dialog = new AlertDialog.Builder(context)
//                .setView(dialogView)
//                .create();
//
//        // Làm trong suốt nền mặc định để hiện góc bo tròn của Custom Layout
//        if (dialog.getWindow() != null) {
//            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
//        }
//
//        EditText etName = dialogView.findViewById(R.id.et_playlist_name);
//
//        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
//
//        dialogView.findViewById(R.id.btn_create).setOnClickListener(v -> {
//            String name = etName.getText().toString().trim();
//            if (!name.isEmpty()) {
//                repository.createPlaylist(name, "app_logo");
//
//                // Gọi lệnh renderAll() truyền từ LibraryFragment sang để refresh màn hình ngay lập tức
//                if (onSuccess != null) {
//                    onSuccess.run();
//                }
//                dialog.dismiss();
//            }
//        });
//
//        dialog.show();
//    }
}
