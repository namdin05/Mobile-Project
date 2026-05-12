package com.melodix.app.View.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.melodix.app.Model.Genre;
import com.melodix.app.R;
import com.melodix.app.Service.GenreAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GenreSelectionDialog {

    private final Context context;
    private final OnGenreSelectedListener listener;

    
    public interface OnGenreSelectedListener {
        void onSelected(int genreId, String genreName);
    }

    
    public GenreSelectionDialog(Context context, OnGenreSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    
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
        title.setText("Chọn Thể loại âm nhạc");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(android.graphics.Color.BLACK);
        title.setPadding(60, 20, 60, 30);
        container.addView(title);

        GenreAPIService genreApi = RetrofitClient.getClient(context.getApplicationContext()).create(GenreAPIService.class);
        genreApi.getGenres().enqueue(new Callback<List<Genre>>() {
            @Override
            public void onResponse(Call<List<Genre>> call, Response<List<Genre>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Genre genre : response.body()) {
                        
                        if (!genre.isVisible()) continue;

                        container.addView(createPremiumDialogItem("🎧", genre.getName(), v -> {
                            int gId = Integer.parseInt(genre.getId());
                            
                            listener.onSelected(gId, genre.getName());
                            dialog.dismiss();
                        }));
                    }
                }
            }
            @Override public void onFailure(Call<List<Genre>> call, Throwable t) {}
        });
        dialog.show();
    }

    
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