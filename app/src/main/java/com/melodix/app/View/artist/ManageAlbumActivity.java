package com.melodix.app.View.artist;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.melodix.app.Model.Album;
import com.melodix.app.Model.SessionManager;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.AlbumAPIService; // Nhớ import Service mới của bạn
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.View.adapters.ManageAlbumAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageAlbumActivity extends AppCompatActivity {

    private RecyclerView rvAlbums;
    private ManageAlbumAdapter adapter;
    private List<Album> albumList;
    private SwipeRefreshLayout swipeRefresh;
    private ArtistAPIService artistApiService;
    private AlbumAPIService albumApiService;
    private String myArtistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_album); // File giao diện của bạn

        // Khởi tạo API
        artistApiService = RetrofitClient.getSupabaseClient().create(ArtistAPIService.class);
        albumApiService = RetrofitClient.getSupabaseClient().create(AlbumAPIService.class);

        myArtistId = SessionManager.getInstance(this).getCurrentUser().getId();

        rvAlbums = findViewById(R.id.rv_manage_albums);
        swipeRefresh = findViewById(R.id.swipe_refresh);

        albumList = new ArrayList<>();
        // Đổi chỗ này
        adapter = new ManageAlbumAdapter(this, albumList, album -> {
            Intent intent = new Intent(ManageAlbumActivity.this, ManageAlbumDetailActivity.class);
            intent.putExtra("ALBUM_ID", album.id);
            intent.putExtra("ALBUM_TITLE", album.title);
            intent.putExtra("ALBUM_COVER", album.coverRes);
            intent.putExtra("ALBUM_STATUS", album.status);
            startActivity(intent);
        });
        rvAlbums.setLayoutManager(new LinearLayoutManager(this));
        rvAlbums.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadMyAlbums);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        // Chuyển sang trang Tạo Album khi bấm nút Dấu Cộng
        findViewById(R.id.btn_create_new_album).setOnClickListener(v -> {
            startActivity(new android.content.Intent(ManageAlbumActivity.this, CreateAlbumActivity.class));
        });

        loadMyAlbums();
    }

    private void loadMyAlbums() {
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
                    Toast.makeText(ManageAlbumActivity.this, "Không thể tải danh sách Album", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Album>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ManageAlbumActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // HIỂN THỊ BOTTOM SHEET DANH SÁCH BÀI HÁT
    private void showAlbumDetailsBottomSheet(Album album) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetTheme);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 40, 0, 40);
        scrollView.addView(container);
        dialog.setContentView(scrollView);

        boolean isNightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int bgColor = isNightMode ? Color.parseColor("#1E1E1E") : Color.WHITE;
        int textColor = isNightMode ? Color.WHITE : Color.parseColor("#1C1C1E");

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(bgColor);
        bgShape.setCornerRadii(new float[]{60, 60, 60, 60, 0, 0, 0, 0});
        ((View) scrollView.getParent()).setBackgroundColor(Color.TRANSPARENT);
        container.setBackground(bgShape);

        // Header Title
        TextView title = new TextView(this);
        title.setText("Tracklist: " + album.title);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(textColor);
        title.setPadding(60, 20, 60, 30);
        container.addView(title);

        // Loading spinner
        ProgressBar progressBar = new ProgressBar(this);
        container.addView(progressBar);

        // Gọi API lấy bài hát trong Album
        albumApiService.getSongsByAlbumId("eq." + album.id).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Song> songs = response.body();

                    if (songs.isEmpty()) {
                        TextView tvEmpty = new TextView(ManageAlbumActivity.this);
                        tvEmpty.setText("Album này chưa có bài hát nào.");
                        tvEmpty.setPadding(60, 20, 60, 20);
                        tvEmpty.setTextColor(Color.GRAY);
                        container.addView(tvEmpty);
                    } else {
                        for (int i = 0; i < songs.size(); i++) {
                            Song song = songs.get(i);
                            container.addView(createSongItemView(song, i + 1, textColor, dialog, songs));
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageAlbumActivity.this, "Lỗi lấy danh sách bài hát", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    // TẠO GIAO DIỆN TỪNG DÒNG BÀI HÁT TRONG BOTTOM SHEET
    private View createSongItemView(Song song, int index, int textColor, BottomSheetDialog parentDialog, List<Song> allSongs) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(60, 30, 60, 30);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        // Số thứ tự track
        TextView tvIndex = new TextView(this);
        tvIndex.setText(String.valueOf(index));
        tvIndex.setTextColor(Color.GRAY);
        tvIndex.setTextSize(16);
        tvIndex.setPadding(0, 0, 40, 0);
        layout.addView(tvIndex);

        // Tên bài hát
        TextView tvTitle = new TextView(this);
        tvTitle.setText(song.getTitle());
        tvTitle.setTextColor(textColor);
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvTitle.setLayoutParams(params);
        layout.addView(tvTitle);

        // Nút Gỡ
        TextView btnRemove = new TextView(this);
        btnRemove.setText("Gỡ");
        btnRemove.setTextColor(Color.parseColor("#FF453A")); // Đỏ
        btnRemove.setTextSize(14);
        btnRemove.setTypeface(null, Typeface.BOLD);
        btnRemove.setPadding(30, 10, 0, 10);

        btnRemove.setOnClickListener(v -> {
            new AlertDialog.Builder(ManageAlbumActivity.this)
                    .setTitle("Gỡ khỏi Album")
                    .setMessage("Bạn muốn đưa bài '" + song.getTitle() + "' ra khỏi album này (trở thành Single)?")
                    .setPositiveButton("Gỡ", (dialog, which) -> {
                        removeSongFromAlbum(song.getId(), parentDialog);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        layout.addView(btnRemove);

        android.util.TypedValue outValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        layout.setBackgroundResource(outValue.resourceId);
        layout.setClickable(true);

        layout.setOnClickListener(v -> {
            // Gọi tiện ích phát nhạc y chang như ManageSongActivity!
            com.melodix.app.Utils.PlaybackUtils.playSong(ManageAlbumActivity.this, new ArrayList<>(allSongs), song.getId());
        });
        return layout;
    }

    // LOGIC CẬP NHẬT DATABASE ĐỂ GỠ BÀI HÁT
    private void removeSongFromAlbum(String songId, BottomSheetDialog parentDialog) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("album_id", null); // Tống ra khỏi album

        artistApiService.updateSong("eq." + songId, updateData).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ManageAlbumActivity.this, "Đã gỡ bài hát khỏi Album", Toast.LENGTH_SHORT).show();
                    parentDialog.dismiss(); // Đóng sheet
                    // Tùy chọn: Gọi lại loadMyAlbums nếu cần cập nhật giao diện
                } else {
                    Toast.makeText(ManageAlbumActivity.this, "Lỗi khi gỡ bài hát", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                Toast.makeText(ManageAlbumActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}