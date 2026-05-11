package com.melodix.app.View.dialogs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.melodix.app.Model.Artist;
import com.melodix.app.R;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Service.SearchAPIService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CollabSelectionDialog {

    private final Context context;
    private final OnCollabSelectedListener listener;

    // 1. TẠO INTERFACE TRẢ DỮ LIỆU VỀ
    public interface OnCollabSelectedListener {
        void onSelected(String artistId, String artistName);
    }

    // 2. CONSTRUCTOR
    public CollabSelectionDialog(Context context, OnCollabSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    // 3. HÀM HIỂN THỊ DIALOG TÌM KIẾM
    public void show() {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.BottomSheetTheme);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 40, 0, 60);

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadii(new float[]{60, 60, 60, 60, 0, 0, 0, 0});
        container.setBackground(bgShape);

        // Thanh tìm kiếm
        EditText edtSearch = new EditText(context);
        edtSearch.setHint("🔍 Enter artist name...");
        edtSearch.setTextColor(Color.BLACK);
        edtSearch.setHintTextColor(Color.parseColor("#8E8E93"));
        edtSearch.setPadding(50, 40, 50, 40);
        edtSearch.setSingleLine(true);
        edtSearch.setTextSize(16);
        edtSearch.setBackgroundResource(android.R.color.transparent);

        LinearLayout searchContainer = new LinearLayout(context);
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Color.parseColor("#F2F2F7"));
        searchBg.setCornerRadius(30);
        searchContainer.setBackground(searchBg);

        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(60, 20, 60, 40);
        searchContainer.setLayoutParams(searchParams);
        searchContainer.addView(edtSearch, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(searchContainer);

        // Khung chứa kết quả
        FrameLayout resultFrame = new FrameLayout(context);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        frameParams.setMargins(0, 0, 0, 40);
        resultFrame.setLayoutParams(frameParams);

        LinearLayout resultContainer = new LinearLayout(context);
        resultContainer.setOrientation(LinearLayout.VERTICAL);
        resultFrame.addView(resultContainer);

        ProgressBar progressBar = new ProgressBar(context);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        progressParams.topMargin = 50;
        progressBar.setLayoutParams(progressParams);
        progressBar.setVisibility(View.GONE);
        resultFrame.addView(progressBar);

        TextView tvEmpty = new TextView(context);
        tvEmpty.setText("Dữ liệu trống rỗng 🍃\nHãy thử tìm tên nghệ sĩ khác xem sao.");
        tvEmpty.setGravity(Gravity.CENTER);
        tvEmpty.setTextColor(Color.parseColor("#8E8E93"));
        tvEmpty.setTextSize(14);
        FrameLayout.LayoutParams emptyParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        emptyParams.topMargin = 100;
        tvEmpty.setLayoutParams(emptyParams);
        tvEmpty.setVisibility(View.GONE);
        resultFrame.addView(tvEmpty);

        container.addView(resultFrame);
        dialog.setContentView(container);
        ((View) container.getParent()).setBackgroundColor(Color.TRANSPARENT);

        SearchAPIService searchAPI = RetrofitClient.getClient(context.getApplicationContext()).create(SearchAPIService.class);
        Handler searchHandler = new Handler(Looper.getMainLooper());
        final Runnable[] searchRunnable = {null};

        // Bắt sự kiện gõ phím
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (searchRunnable[0] != null) searchHandler.removeCallbacks(searchRunnable[0]);

                if (query.isEmpty()) {
                    resultContainer.removeAllViews();
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.GONE);
                    return;
                }

                resultContainer.removeAllViews();
                tvEmpty.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);

                searchRunnable[0] = () -> {
                    String formattedQuery = query.replaceAll("\\s+", " & ") + ":*";
                    String ftsQuery = "fts(simple)." + Uri.encode(formattedQuery);

                    searchAPI.searchArtists(ftsQuery).enqueue(new Callback<List<Artist>>() {
                        @Override
                        public void onResponse(Call<List<Artist>> call, Response<List<Artist>> response) {
                            progressBar.setVisibility(View.GONE);
                            if (response.isSuccessful() && response.body() != null) {
                                if (response.body().isEmpty()) {
                                    tvEmpty.setVisibility(View.VISIBLE);
                                } else {
                                    for (Artist artist : response.body()) {
                                        resultContainer.addView(createPremiumArtistDialogItem(artist.avatarRes, artist.name, v -> {
                                            // Gọi ngược về Activity gốc
                                            listener.onSelected(artist.id, artist.name);
                                            dialog.dismiss();
                                        }));
                                    }
                                }
                            } else {
                                tvEmpty.setText("Có lỗi xảy ra khi tìm kiếm 😢");
                                tvEmpty.setVisibility(View.VISIBLE);
                            }
                        }
                        @Override public void onFailure(Call<List<Artist>> call, Throwable t) {
                            progressBar.setVisibility(View.GONE);
                            tvEmpty.setText("Lỗi kết nối mạng 📶");
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                    });
                };
                searchHandler.postDelayed(searchRunnable[0], 500); // 500ms delay chống spam API
            }
        });

        dialog.show();

        // Ép BottomSheet cao 85% màn hình
        int bottomSheetId = context.getResources().getIdentifier("design_bottom_sheet", "id", context.getPackageName());
        if (bottomSheetId == 0) bottomSheetId = context.getResources().getIdentifier("design_bottom_sheet", "id", "com.google.android.material");
        View bottomSheet = dialog.findViewById(bottomSheetId);

        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            int screenHeight = android.content.res.Resources.getSystem().getDisplayMetrics().heightPixels;
            bottomSheet.getLayoutParams().height = (int) (screenHeight * 0.85);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    // 4. HÀM VẼ ITEM NGHỆ SĨ (Kèm Avatar)
    private View createPremiumArtistDialogItem(String avatarUrl, String text, View.OnClickListener onClick) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(60, 40, 60, 40);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        layout.setBackgroundResource(outValue.resourceId);
        layout.setClickable(true);
        layout.setOnClickListener(onClick);

        ImageView imgAvatar = new ImageView(context);
        int avatarSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics());
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(avatarSize, avatarSize);
        imgParams.setMargins(0, 0, 40, 0);
        imgAvatar.setLayoutParams(imgParams);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(context).load(avatarUrl).placeholder(R.drawable.ic_menu_for).circleCrop().into(imgAvatar);
        } else {
            imgAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        TextView tvText = new TextView(context);
        tvText.setText(text);
        tvText.setTextColor(Color.parseColor("#1C1C1E"));
        tvText.setTextSize(16);
        tvText.setTypeface(null, Typeface.BOLD);

        layout.addView(imgAvatar);
        layout.addView(tvText);

        return layout;
    }
}