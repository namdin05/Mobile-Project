package com.melodix.app.View.artist;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Service.AlbumAPIService;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.View.adapters.ManageSongAdapter; // Import Adapter của bạn

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageAlbumDetailActivity extends AppCompatActivity {

    private String albumId, albumTitle, albumCover, albumStatus;

    private ImageView imgCover;
    private TextView tvTitle, tvStatus, tvEmptyTracks;
    private ProgressBar progressBar;

    // Đã đổi sang RecyclerView
    private RecyclerView rvTracklist;
    private ManageSongAdapter adapter;

    private AlbumAPIService albumApiService;
    private ArtistAPIService artistApiService;
    private List<Song> songList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_album_detail);

        albumApiService = RetrofitClient.getSupabaseClient().create(AlbumAPIService.class);
        artistApiService = RetrofitClient.getSupabaseClient().create(ArtistAPIService.class);

        // Lấy dữ liệu từ Intent gửi sang
        albumId = getIntent().getStringExtra("ALBUM_ID");
        albumTitle = getIntent().getStringExtra("ALBUM_TITLE");
        albumCover = getIntent().getStringExtra("ALBUM_COVER");
        albumStatus = getIntent().getStringExtra("ALBUM_STATUS");

        // Ánh xạ View
        imgCover = findViewById(R.id.img_cover_detail);
        tvTitle = findViewById(R.id.tv_title_detail);
        tvStatus = findViewById(R.id.tv_status_detail);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyTracks = findViewById(R.id.tv_empty_tracks);
        rvTracklist = findViewById(R.id.rv_tracklist);

        // Hiển thị thông tin Header
        tvTitle.setText(albumTitle);
        if (albumCover != null && !albumCover.isEmpty()) {
            Glide.with(this).load(albumCover).into(imgCover);
        }
        setupStatusColor(albumStatus);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // ==========================================
        // 1. SỰ KIỆN NÚT CHỈNH SỬA ALBUM (ĐÃ MỞ KHÓA)
        // ==========================================
        findViewById(R.id.btn_edit_album).setOnClickListener(v -> {
            Intent intent = new Intent(ManageAlbumDetailActivity.this, CreateAlbumActivity.class);
            intent.putExtra("IS_EDIT_MODE", true);
            intent.putExtra("EDIT_ALBUM_ID", albumId);
            intent.putExtra("EDIT_ALBUM_TITLE", albumTitle);
            intent.putExtra("EDIT_ALBUM_COVER", albumCover);
            startActivity(intent);
            finish(); // Đóng trang này, sửa xong ở CreateAlbumActivity nó sẽ về trang Quản lý
        });

        // ==========================================
        // 2. SETUP RECYCLERVIEW & ADAPTER
        // ==========================================
        rvTracklist.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ManageSongAdapter(this, songList, new ManageSongAdapter.OnSongOptionClickListener() {
            @Override
            public void onOptionClick(Song song) {
                // Mở BottomSheet khi ấn nút 3 chấm
                showAlbumSongOptions(song);
            }

            @Override
            public void onSongClick(Song song) {
                // Phát nhạc khi bấm vào thẻ bài hát
                com.melodix.app.Utils.PlaybackUtils.playSong(ManageAlbumDetailActivity.this, new ArrayList<>(songList), song.getId());
            }
        });
        rvTracklist.setAdapter(adapter);

        // Load dữ liệu
        loadTracks();
    }

    private void setupStatusColor(String status) {
        if (status == null) status = "pending";
        switch (status.toLowerCase()) {
            case "approved":
                tvStatus.setText("Đã duyệt");
                tvStatus.setTextColor(Color.parseColor("#1DB954"));
                break;
            case "rejected":
                tvStatus.setText("Bị từ chối");
                tvStatus.setTextColor(Color.parseColor("#FF453A"));
                break;
            default:
                tvStatus.setText("Đang chờ duyệt");
                tvStatus.setTextColor(Color.parseColor("#FF9F0A"));
                break;
        }
    }

    private void loadTracks() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyTracks.setVisibility(View.GONE);
        rvTracklist.setVisibility(View.GONE);

        albumApiService.getSongsByAlbumIdForArtist("eq." + albumId).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    songList.clear();
                    songList.addAll(response.body());
                    adapter.notifyDataSetChanged(); // Yêu cầu Adapter vẽ lại dữ liệu

                    if (songList.isEmpty()) {
                        tvEmptyTracks.setVisibility(View.VISIBLE);
                    } else {
                        rvTracklist.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageAlbumDetailActivity.this, "Lỗi tải bài hát", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==========================================
    // 3. BOTTOM SHEET LỰA CHỌN (GỠ KHỎI ALBUM)
    // ==========================================
    private void showAlbumSongOptions(Song song) {
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

        // Header: Tên bài hát
        TextView title = new TextView(this);
        title.setText(song.getTitle());
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(textColor);
        title.setPadding(60, 20, 60, 30);
        container.addView(title);

        // Nút Gỡ khỏi Album
        container.addView(createOptionItem("🗑️", "Gỡ khỏi Album này", Color.parseColor("#FF453A"), v -> {
            dialog.dismiss();
            new AlertDialog.Builder(ManageAlbumDetailActivity.this)
                    .setTitle("Gỡ khỏi Album")
                    .setMessage("Đưa bài hát '" + song.getTitle() + "' trở thành Single?")
                    .setPositiveButton("Gỡ", (d, which) -> removeSong(song.getId()))
                    .setNegativeButton("Hủy", null)
                    .show();
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

    private void removeSong(String songId) {
        // 1. Tự viết cứng một chuỗi JSON chuẩn xác
        String jsonString = "{\"album_id\": null}";

        // 2. Ép kiểu nó thành RequestBody (định dạng application/json)
        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"), jsonString);

        // 3. Gọi hàm API Raw mới tạo
        artistApiService.removeSongFromAlbumRaw("eq." + songId, requestBody).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ManageAlbumDetailActivity.this, "Đã gỡ bài hát vĩnh viễn!", Toast.LENGTH_SHORT).show();
                    loadTracks(); // Tải lại danh sách, thề luôn là bay màu!
                } else {
                    Toast.makeText(ManageAlbumDetailActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                Toast.makeText(ManageAlbumDetailActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}