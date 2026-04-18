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
import com.melodix.app.Model.ArtistStats;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.View.adapters.ManageSongAdapter;
import com.melodix.app.Utils.PlaybackUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageSongActivity extends AppCompatActivity {

    private RecyclerView rvSongs;
    private ManageSongAdapter adapter;
    private List<Song> songList;
    private ArtistAPIService apiService;
    private TextView tvTotalListens;
    private TextView tvTotalFans;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmptyState;

    private final NumberFormat numberFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_song);

        apiService = RetrofitClient.getSupabaseClient().create(ArtistAPIService.class);
        rvSongs = findViewById(R.id.rv_manage_songs);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        tvTotalListens = findViewById(R.id.tv_total_listens);
        tvTotalFans = findViewById(R.id.tv_total_fans);
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
        // ĐÃ SỬA: Lấy USER_ID từ SharedPreferences thay vì SessionManager
        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        String myArtistId = prefs.getString("USER_ID", null);

        if (myArtistId == null) {
            Toast.makeText(this, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ManageSongActivity.this, "Không thể tải danh sách tác phẩm", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ManageSongActivity.this, "Lỗi mạng: Vui lòng kiểm tra kết nối", Toast.LENGTH_SHORT).show();
            }
        });

        AppRepository.getInstance(this).getArtistStats(myArtistId, new AppRepository.ArtistStatsCallback() {
            @Override
            public void onSuccess(ArtistStats stats) {
                if (isFinishing() || isDestroyed()) return;
                tvTotalListens.setText(numberFormat.format(stats.totalStreams));
                tvTotalFans.setText(numberFormat.format(stats.totalListeners));
            }

            @Override
            public void onError(String message) { }
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

        TextView title = new TextView(this);
        title.setText(song.getTitle());
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(textColor);
        title.setPadding(60, 20, 60, 30);
        container.addView(title);

        container.addView(createDynamicOptionItem("▶️", "Nghe thử bài hát", textColor, v -> {
            dialog.dismiss();
            PlaybackUtils.playSong(ManageSongActivity.this, new ArrayList<>(songList), song.getId());
        }));

        container.addView(createDynamicOptionItem("✏️", "Chỉnh sửa thông tin", textColor, v -> {
            dialog.dismiss();
            Intent intent = new Intent(ManageSongActivity.this, UploadSongActivity.class);
            intent.putExtra("IS_EDIT_MODE", true);
            intent.putExtra("EDIT_SONG_ID", song.getId());
            intent.putExtra("EDIT_SONG_TITLE", song.getTitle());
            intent.putExtra("EDIT_SONG_COVER", song.getCoverUrl());
            startActivity(intent);
        }));

        container.addView(createDynamicOptionItem("🔗", "Chia sẻ bài hát", textColor, v -> {
            dialog.dismiss();
            com.melodix.app.Utils.ShareUtils.shareContent(ManageSongActivity.this, "song", song.getId(), song.getTitle());
        }));

        container.addView(createDynamicOptionItem("🗑️", "Xóa tác phẩm", Color.parseColor("#FF453A"), v -> {
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

        TextView tvIcon = new TextView(this);
        tvIcon.setText(icon);
        tvIcon.setTextSize(20);
        tvIcon.setPadding(0, 0, 40, 0);

        TextView tvText = new TextView(this);
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
                .setTitle("Cảnh báo xóa")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn bài hát '" + song.getTitle() + "'? Hành động này sẽ xóa toàn bộ lượt nghe và không thể khôi phục.")
                .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> {
                    apiService.deleteSong("eq." + song.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(ManageSongActivity.this, "Đã xóa tác phẩm thành công!", Toast.LENGTH_SHORT).show();
                                loadMySongsAndStats();
                            } else {
                                Toast.makeText(ManageSongActivity.this, "Không thể xóa tác phẩm: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(ManageSongActivity.this, "Lỗi kết nối. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}