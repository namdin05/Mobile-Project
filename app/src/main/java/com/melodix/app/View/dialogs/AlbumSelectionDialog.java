package com.melodix.app.View.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.melodix.app.Model.Album;
import com.melodix.app.R;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlbumSelectionDialog {

    private final Context context;
    private final String currentUserId;
    private final OnAlbumSelectedListener listener;

    // 1. TẠO INTERFACE ĐỂ TRẢ KẾT QUẢ VỀ CHO ACTIVITY
    public interface OnAlbumSelectedListener {
        void onSelected(String albumId, String albumTitle);
    }

    // 2. CONSTRUCTOR NHẬN DỮ LIỆU TỪ NGOÀI TRUYỀN VÀO
    public AlbumSelectionDialog(Context context, String currentUserId, OnAlbumSelectedListener listener) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    // 3. HÀM HIỂN THỊ DIALOG (Đổi tên thành show() cho chuyên nghiệp)
    public void show() {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.BottomSheetTheme);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
        android.widget.LinearLayout container = new android.widget.LinearLayout(context);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(0, 40, 0, 40);
        scrollView.addView(container);
        dialog.setContentView(scrollView);

        android.graphics.drawable.GradientDrawable bgShape = new android.graphics.drawable.GradientDrawable();
        bgShape.setColor(android.graphics.Color.WHITE);
        bgShape.setCornerRadii(new float[]{60, 60, 60, 60, 0, 0, 0, 0});
        ((View) scrollView.getParent()).setBackgroundColor(android.graphics.Color.TRANSPARENT);
        container.setBackground(bgShape);

        TextView title = new TextView(context);
        title.setText("Chọn Album");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(android.graphics.Color.BLACK);
        title.setPadding(60, 20, 60, 30);
        container.addView(title);

        ArtistAPIService supabaseApi = RetrofitClient.getClient(context.getApplicationContext()).create(ArtistAPIService.class);

        supabaseApi.getAlbumsByArtistId("eq." + currentUserId).enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    // Lựa chọn Single
                    container.addView(createPremiumDialogItem("🎵", "Single (Không thuộc album nào)", v -> {
                        listener.onSelected(null, "Single (Không thuộc album nào)"); // Bắn kết quả về!
                        dialog.dismiss();
                    }));

                    View divider = new View(context);
                    divider.setBackgroundColor(android.graphics.Color.parseColor("#E5E5EA"));
                    android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 3);
                    params.setMargins(60, 10, 60, 10);
                    divider.setLayoutParams(params);
                    container.addView(divider);

                    // Danh sách Album
                    for (Album album : response.body()) {
                        if (album.status != null && album.status.equalsIgnoreCase("rejected")) {
                            continue;
                        }
                        String displayTitle = album.title;
                        if (album.status != null && album.status.equalsIgnoreCase("pending")) {
                            displayTitle = album.title + " ( Pending Review )";
                        } else {
                            displayTitle = album.title + " ( " + album.year + " )";
                        }

                        container.addView(createPremiumDialogItem("💽", displayTitle, v -> {
                            listener.onSelected(album.id, album.title); // Bắn kết quả về!
                            dialog.dismiss();
                        }));
                    }
                }
            }
            @Override public void onFailure(Call<List<Album>> call, Throwable t) {}
        });
        dialog.show();
    }

    // 4. MANG LUÔN CÁI HÀM VẼ UI NÀY SANG ĐÂY ĐỂ HOẠT ĐỘNG ĐỘC LẬP
    private View createPremiumDialogItem(String icon, String text, View.OnClickListener onClick) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setPadding(60, 45, 60, 45);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        android.util.TypedValue outValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        layout.setBackgroundResource(outValue.resourceId);
        layout.setClickable(true);
        layout.setOnClickListener(onClick);

        TextView tvIcon = new TextView(context);
        tvIcon.setText(icon);
        tvIcon.setTextSize(22);
        tvIcon.setPadding(0, 0, 40, 0);

        TextView tvText = new TextView(context);
        tvText.setText(text);
        tvText.setTextColor(android.graphics.Color.parseColor("#1C1C1E"));
        tvText.setTextSize(16);
        tvText.setTypeface(null, android.graphics.Typeface.BOLD);

        layout.addView(tvIcon);
        layout.addView(tvText);

        return layout;
    }
}