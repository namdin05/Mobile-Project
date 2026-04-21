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
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.View.adapters.ManageSongAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageSongActivity extends AppCompatActivity {

    private RecyclerView rvSongs;
    private ManageSongAdapter adapter;
    private List<Song> songList;
    private ArtistAPIService apiService;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_song);

        // 1. Ánh xạ View
        apiService = RetrofitClient.getClient(getApplicationContext()).create(ArtistAPIService.class);
        rvSongs = findViewById(R.id.rv_manage_songs);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));

        swipeRefresh = findViewById(R.id.swipe_refresh);
        layoutEmptyState = findViewById(R.id.layout_empty_state);

        songList = new ArrayList<>();
        adapter = new ManageSongAdapter(this, songList, new ManageSongAdapter.OnSongOptionClickListener() {
            @Override
            public void onOptionClick(Song song) {
                showSongOptionsBottomSheet(song);
            }

            @Override
            public void onSongClick(Song song) {
                PlaybackUtils.playSong(ManageSongActivity.this, new ArrayList<>(songList), song.getId());
            }
        });
        rvSongs.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.mdx_primary);
        swipeRefresh.setOnRefreshListener(this::loadMySongsAndStats);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_upload_new).setOnClickListener(v ->
                startActivity(new Intent(this, UploadSongActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMySongsAndStats();
    }

    private void loadMySongsAndStats() {
        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        String myArtistId = prefs.getString("USER_ID", null);

        if (myArtistId == null) {
            Toast.makeText(this, "Session expired. Please sign in again!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        swipeRefresh.setRefreshing(true);

        apiService.getMyUploadSongs("eq." + myArtistId).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    songList.clear();
                    songList.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (songList.isEmpty()) {
                        layoutEmptyState.setVisibility(View.VISIBLE);
                        rvSongs.setVisibility(View.GONE);
                    } else {
                        layoutEmptyState.setVisibility(View.GONE);
                        rvSongs.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(ManageSongActivity.this, "Unable to load your tracks", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ManageSongActivity.this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSongOptionsBottomSheet(Song song) {
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

        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(song.getTitle());
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(textColor);
        title.setPadding(60, 20, 60, 30);
        container.addView(title);

        container.addView(createDynamicOptionItem("▶️", "Preview Track", textColor, v -> {
            dialog.dismiss();
            PlaybackUtils.playSong(ManageSongActivity.this, new ArrayList<>(songList), song.getId());
        }));

        container.addView(createDynamicOptionItem("✏️", "Edit Track Details", textColor, v -> {
            dialog.dismiss();
            Intent intent = new Intent(ManageSongActivity.this, UploadSongActivity.class);
            intent.putExtra("IS_EDIT_MODE", true);
            intent.putExtra("EDIT_SONG_ID", song.getId());
            intent.putExtra("EDIT_SONG_TITLE", song.getTitle());
            intent.putExtra("EDIT_SONG_COVER", song.getCoverUrl());
            startActivity(intent);
        }));

        container.addView(createDynamicOptionItem("🗑️", "Delete Track", Color.parseColor("#FF453A"), v -> {
            dialog.dismiss();
            confirmDeleteSong(song);
        }));

        dialog.setContentView(container);
        ((View) container.getParent()).setBackgroundColor(Color.TRANSPARENT);
        dialog.show();
    }

    private View createDynamicOptionItem(String icon, String text, int textColor, View.OnClickListener onClick) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(60, 45, 60, 45);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        layout.setBackgroundResource(outValue.resourceId);
        layout.setClickable(true);
        layout.setOnClickListener(onClick);

        android.widget.TextView tvIcon = new android.widget.TextView(this);
        tvIcon.setText(icon);
        tvIcon.setTextSize(20);
        tvIcon.setPadding(0, 0, 40, 0);

        android.widget.TextView tvText = new android.widget.TextView(this);
        tvText.setText(text);
        tvText.setTextColor(textColor);
        tvText.setTextSize(16);
        tvText.setTypeface(null, Typeface.BOLD);

        layout.addView(tvIcon);
        layout.addView(tvText);
        return layout;
    }

    private void confirmDeleteSong(Song song) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Warning")
                .setMessage("Are you sure you want to permanently delete the track '" + song.getTitle() + "'? The track and audio file will be completely removed from the system.")
                .setPositiveButton("Delete Permanently", (dialog, which) -> {

                    SharedPreferences prefs = getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);

                    String audioFileName = extractFileNameFromUrl(song.getAudioUrl());
                    String coverFileName = extractFileNameFromUrl(song.getCoverUrl());

                    if (audioFileName != null) {
                        apiService.deleteAudioFile(audioFileName)
                                .enqueue(new Callback<Void>() {
                                    @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                                    @Override public void onFailure(Call<Void> c, Throwable t) {}
                                });
                    }

                    if (coverFileName != null && !song.getCoverUrl().contains("default")) {
                        apiService.deleteCoverFile(coverFileName)
                                .enqueue(new Callback<Void>() {
                                    @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                                    @Override public void onFailure(Call<Void> c, Throwable t) {}
                                });
                    }

                    apiService.deleteSong("eq." + song.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(ManageSongActivity.this, "Track and files deleted successfully!", Toast.LENGTH_SHORT).show();
                                loadMySongsAndStats();
                            } else {
                                Toast.makeText(ManageSongActivity.this, "Database delete failed: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(ManageSongActivity.this, "Connection error. Please try again!", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String extractFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        return url.substring(url.lastIndexOf('/') + 1);
    }
}