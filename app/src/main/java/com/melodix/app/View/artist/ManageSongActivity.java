package com.melodix.app.View.artist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.SessionManager;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.RetrofitClient;
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
    private android.widget.TextView tvTotalListens;
    private android.widget.TextView tvTotalFans;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private android.view.View layoutEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_song);

        // Khởi tạo
        apiService = RetrofitClient.getSupabaseClient().create(ArtistAPIService.class);
        rvSongs = findViewById(R.id.rv_manage_songs);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        tvTotalListens = findViewById(R.id.tv_total_listens); // Nhớ đặt ID này cho 2 số "0" trong XML nhé
        tvTotalFans = findViewById(R.id.tv_total_fans);
        songList = new ArrayList<>();
        // Khởi tạo Adapter với Listener nút 3 chấm
        adapter = new ManageSongAdapter(this, songList, new ManageSongAdapter.OnSongOptionClickListener() {
            @Override
            public void onOptionClick(Song song) {
                showSongOptionsBottomSheet(song); // Gọi hàm mở Menu
            }
        });
        rvSongs.setAdapter(adapter);


        swipeRefresh = findViewById(R.id.swipe_refresh);
        layoutEmptyState = findViewById(R.id.layout_empty_state);

        // Đổi màu vòng xoay loading cho "tone sur tone" với app
        swipeRefresh.setColorSchemeResources(R.color.mdx_primary);

        // Bắt sự kiện người dùng vuốt xuống
        swipeRefresh.setOnRefreshListener(() -> {
            loadMySongs(); // Gọi lại API
        });

        // Bắt sự kiện click
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_upload_new).setOnClickListener(v -> {
            // Mở màn hình đăng bài hát đã làm
            startActivity(new Intent(this, UploadSongActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load lại danh sách mỗi khi quay lại màn hình này (ví dụ sau khi up bài mới xong)
        loadMySongs();
    }

    private void loadMySongs() {
        String myArtistId = SessionManager.getInstance(this).getCurrentUser().getId();

        // Bật vòng xoay nếu gọi lần đầu (không phải do vuốt)
        swipeRefresh.setRefreshing(true);

        apiService.getMyUploadSongs("eq." + myArtistId).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                swipeRefresh.setRefreshing(false); // Tắt vòng xoay loading

                if (response.isSuccessful() && response.body() != null) {
                    songList.clear();
                    songList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    int totalListens = 0;
                    for (Song s : songList) {
                        totalListens += s.getPlays();
                    }
                    tvTotalListens.setText(String.valueOf(totalListens));
                    // Giả lập số fan dựa trên lượt nghe (hoặc lấy từ DB nếu bạn có)
                    tvTotalFans.setText(String.valueOf(totalListens / 5));
                    // LOGIC EMPTY STATE: Kiểm tra rỗng
                    if (songList.isEmpty()) {
                        layoutEmptyState.setVisibility(android.view.View.VISIBLE);
                        rvSongs.setVisibility(android.view.View.GONE);
                    } else {
                        layoutEmptyState.setVisibility(android.view.View.GONE);
                        rvSongs.setVisibility(android.view.View.VISIBLE);
                    }
                } else {
                    Toast.makeText(ManageSongActivity.this, "Không thể tải danh sách", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                swipeRefresh.setRefreshing(false); // Tắt vòng xoay
                Toast.makeText(ManageSongActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSongOptionsBottomSheet(Song song) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetTheme);

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(0, 40, 0, 40);

        android.graphics.drawable.GradientDrawable bgShape = new android.graphics.drawable.GradientDrawable();
        bgShape.setColor(android.graphics.Color.WHITE);
        bgShape.setCornerRadii(new float[]{60, 60, 60, 60, 0, 0, 0, 0});
        container.setBackground(bgShape);

        // Header: Tên bài hát
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(song.getTitle());
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(android.graphics.Color.BLACK);
        title.setPadding(60, 20, 60, 30);
        container.addView(title);

        // Tính năng 1: Chỉnh sửa
        container.addView(createOptionItem("✏️", "Chỉnh sửa thông tin", v -> {
            dialog.dismiss();
            android.content.Intent intent = new android.content.Intent(ManageSongActivity.this, UploadSongActivity.class);
            // Bật cờ hiệu "Đây là chế độ Edit"
            intent.putExtra("IS_EDIT_MODE", true);
            // Gửi dữ liệu cũ sang
            intent.putExtra("EDIT_SONG_ID", song.getId());
            intent.putExtra("EDIT_SONG_TITLE", song.getTitle());
            intent.putExtra("EDIT_SONG_COVER", song.getCoverUrl());
            startActivity(intent);
        }));

        // Tính năng 2: Chia sẻ
        container.addView(createOptionItem("🔗", "Chia sẻ bài hát", v -> {
            dialog.dismiss();
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Nghe ngay bài hát " + song.getTitle() + " trên Melodix! \nhttps://melodix.app/song/" + song.getId());
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"));
        }));

        // Tính năng 3: Xóa bài hát (Đỏ nguy hiểm)
        android.view.View deleteView = createOptionItem("🗑️", "Xóa tác phẩm", v -> {
            dialog.dismiss();
            confirmDeleteSong(song);
        });
        // Ép màu đỏ cho chữ Xóa
        ((android.widget.TextView) ((android.widget.LinearLayout) deleteView).getChildAt(1)).setTextColor(android.graphics.Color.parseColor("#FF453A"));
        container.addView(deleteView);

        dialog.setContentView(container);
        ((android.view.View) container.getParent()).setBackgroundColor(android.graphics.Color.TRANSPARENT);
        dialog.show();
    }

    // Hàm tạo Item giao diện xịn xò (Tái sử dụng logic cũ)
    private android.view.View createOptionItem(String icon, String text, android.view.View.OnClickListener onClick) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setPadding(60, 45, 60, 45);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        android.util.TypedValue outValue = new android.util.TypedValue();
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
        tvText.setTextColor(android.graphics.Color.parseColor("#1C1C1E"));
        tvText.setTextSize(16);
        tvText.setTypeface(null, android.graphics.Typeface.BOLD);

        layout.addView(tvIcon);
        layout.addView(tvText);
        return layout;
    }

    // Hàm xác nhận Xóa bằng Dialog Alert
    private void confirmDeleteSong(Song song) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa Tác Phẩm")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn bài hát '" + song.getTitle() + "' không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {

                    // Gọi API Xóa
                    apiService.deleteSong("eq." + song.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(ManageSongActivity.this, "Đã xóa bài hát!", Toast.LENGTH_SHORT).show();
                                // Xóa thành công thì tải lại danh sách
                                loadMySongs();
                            } else {
                                Toast.makeText(ManageSongActivity.this, "Lỗi xóa bài hát: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(ManageSongActivity.this, "Lỗi mạng khi xóa!", Toast.LENGTH_SHORT).show();
                        }
                    });

                })
                .setNegativeButton("Hủy", null)
                .show();
    }}