package com.melodix.app.View.artist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.melodix.app.Model.Album;
import com.melodix.app.R;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.View.adapters.ManageAlbumAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageAlbumActivity extends AppCompatActivity {

    private RecyclerView rvAlbums;
    private ManageAlbumAdapter adapter;
    private List<Album> albumList;
    private SwipeRefreshLayout swipeRefresh;
    private ArtistAPIService artistApiService;
    private String myArtistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_album);

        // Initialize API
        artistApiService = RetrofitClient.getClient(getApplication()).create(ArtistAPIService.class);

        // Get USER_ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        myArtistId = prefs.getString("USER_ID", null);

        if (myArtistId == null) {
            Toast.makeText(this, "Session expired!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvAlbums = findViewById(R.id.rv_manage_albums);
        swipeRefresh = findViewById(R.id.swipe_refresh);

        albumList = new ArrayList<>();

        // 1. INITIALIZE UPGRADED ADAPTER
        adapter = new ManageAlbumAdapter(this, albumList, new ManageAlbumAdapter.OnAlbumOptionClickListener() {
            @Override
            public void onAlbumClick(Album album) {
                // Tap on album card -> Open album detail screen
                Intent intent = new Intent(ManageAlbumActivity.this, ManageAlbumDetailActivity.class);
                intent.putExtra("ALBUM_ID", album.id);
                intent.putExtra("ALBUM_TITLE", album.title);
                intent.putExtra("ALBUM_COVER", album.coverRes);
                intent.putExtra("ALBUM_STATUS", album.status);
                startActivity(intent);
            }

            @Override
            public void onOptionClick(Album album) {
                // Tap the 3-dot menu -> Open options menu
                showAlbumOptionsBottomSheet(album);
            }
        });

        rvAlbums.setLayoutManager(new LinearLayoutManager(this));
        rvAlbums.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadMyAlbums);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_create_new_album).setOnClickListener(v -> {
            startActivity(new Intent(ManageAlbumActivity.this, CreateAlbumActivity.class));
        });

        loadMyAlbums();
    }

    private void loadMyAlbums() {
        if (myArtistId == null) return;

        swipeRefresh.setRefreshing(true);
        artistApiService.getAlbumsByArtistId("eq." + myArtistId).enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    albumList.clear();
                    albumList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(ManageAlbumActivity.this, "Unable to load album list", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Album>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ManageAlbumActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==========================================
    // 2. BOTTOM SHEET MENU (EDIT & DELETE ALBUM)
    // ==========================================
    private void showAlbumOptionsBottomSheet(Album album) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetTheme);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 40, 0, 40);

        boolean isNightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int bgColor = isNightMode ? Color.parseColor("#1E1E1E") : Color.WHITE;
        int textColor = isNightMode ? Color.WHITE : Color.parseColor("#1C1C1E");

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(bgColor);
        bgShape.setCornerRadii(new float[]{60, 60, 60, 60, 0, 0, 0, 0});
        container.setBackground(bgShape);

        // Header: Album title
        TextView title = new TextView(this);
        title.setText(album.title);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(textColor);
        title.setPadding(60, 20, 60, 30);
        container.addView(title);

        // EDIT ALBUM BUTTON
        container.addView(createOptionItem("✏️", "Edit Album Details", textColor, v -> {
            dialog.dismiss();
            Intent intent = new Intent(ManageAlbumActivity.this, CreateAlbumActivity.class);
            intent.putExtra("IS_EDIT_MODE", true);
            intent.putExtra("EDIT_ALBUM_ID", album.id);
            intent.putExtra("EDIT_ALBUM_TITLE", album.title);
            intent.putExtra("EDIT_ALBUM_COVER", album.coverRes);
            startActivity(intent);
        }));

        // DELETE ALBUM BUTTON
        container.addView(createOptionItem("🗑️", "Delete Album Permanently", Color.parseColor("#FF453A"), v -> {
            dialog.dismiss();
            confirmDeleteAlbum(album);
        }));

        dialog.setContentView(container);
        ((View) container.getParent()).setBackgroundColor(Color.TRANSPARENT);
        dialog.show();
    }

    private View createOptionItem(String icon, String text, int textColor, View.OnClickListener onClick) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(60, 45, 60, 45);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        layout.setBackgroundResource(outValue.resourceId);
        layout.setClickable(true);
        layout.setOnClickListener(onClick);

        TextView tvIcon = new TextView(this);
        tvIcon.setText(icon);
        tvIcon.setTextSize(20);
        tvIcon.setPadding(0, 0, 40, 0);
        layout.addView(tvIcon);

        TextView tvText = new TextView(this);
        tvText.setText(text);
        tvText.setTextColor(textColor);
        tvText.setTextSize(16);
        tvText.setTypeface(null, Typeface.BOLD);
        layout.addView(tvText);

        return layout;
    }

    // ==========================================
    // 3. DELETE ALBUM LOGIC
    // ==========================================
    private void confirmDeleteAlbum(Album album) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Album Warning")
                .setMessage("Are you sure you want to permanently delete the album '" + album.title + "'?\n\nNote: The songs inside this album will NOT be deleted. They will automatically become singles.")
                .setPositiveButton("Delete Album", (dialog, which) -> {

                    swipeRefresh.setRefreshing(true);

                    // Call delete API (Make sure Supabase uses ON DELETE SET NULL for the songs table)
                    artistApiService.deleteAlbum("eq." + album.id).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            swipeRefresh.setRefreshing(false);
                            if (response.isSuccessful()) {
                                Toast.makeText(ManageAlbumActivity.this, "Album deleted successfully!", Toast.LENGTH_SHORT).show();
                                loadMyAlbums();
                            } else {
                                Toast.makeText(ManageAlbumActivity.this, "Delete failed: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            swipeRefresh.setRefreshing(false);
                            Toast.makeText(ManageAlbumActivity.this, "Network error!", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}