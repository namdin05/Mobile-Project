package com.melodix.app.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;

import com.melodix.app.R;

public class LoadingDialog {
    private AlertDialog dialog;

    // Hiển thị vòng xoay
    public void showLoading(Activity activity) {
        // Tránh lỗi crash nếu dialog đã hiển thị rồi
        if (dialog != null && dialog.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_loading, null));

        // Cực kỳ quan trọng: Ngăn người dùng bấm ra ngoài hoặc bấm nút Back để hủy
        builder.setCancelable(false);

        dialog = builder.create();

        // Làm nền của Dialog trong suốt để hiện rõ phần thẻ bo tròn bên trong
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();
    }

    // Tắt vòng xoay
    public void hideLoading() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}