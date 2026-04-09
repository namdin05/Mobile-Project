package com.melodix.app.View.artist; // Lưu ý package name của bạn (có thể là com.melodix.app.View tuỳ thư mục hiện tại)

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.ArtistStats;
import com.melodix.app.Model.Song;
import com.melodix.app.PlayerActivity;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.View.ArtistDetailActivity;
import com.melodix.app.View.ArtistSongsActivity;
import com.melodix.app.View.adapters.SongAdapter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ArtistAnalyticsActivity extends AppCompatActivity {

    public static final String EXTRA_ARTIST_ID = "extra_artist_id";

    // 1. Nhóm UI Components
    private TextView tvStreams, tvLikes, tvSongs, tvTotalFollowers;
    private TextView tvSeeAllStreams, tvSeeAllLikes;
    private RecyclerView rvTopStreams, rvTopLikes;

    // 2. Nhóm Adapters
    private SongAdapter topStreamsAdapter, topLikesAdapter;

    // 3. Tối ưu hóa: Khởi tạo NumberFormat 1 lần duy nhất để tiết kiệm RAM
    private final NumberFormat numberFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    private String currentArtistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_analytics);

        initViews();
        setupListeners();

        // 4. Kiểm tra dữ liệu đầu vào cực kỳ chặt chẽ
        currentArtistId = getIntent().getStringExtra(EXTRA_ARTIST_ID);

        if (currentArtistId != null && !currentArtistId.trim().isEmpty()) {
            loadAnalyticsData(currentArtistId);
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID nghệ sĩ", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Tách riêng hàm ánh xạ View cho code sạch sẽ
     */
    private void initViews() {
        tvStreams = findViewById(R.id.tv_stat_streams);
        tvLikes = findViewById(R.id.tv_stat_likes);
        tvSongs = findViewById(R.id.tv_stat_songs);
        tvTotalFollowers = findViewById(R.id.tv_total_followers);

        tvSeeAllStreams = findViewById(R.id.tv_see_all_streams);
        tvSeeAllLikes = findViewById(R.id.tv_see_all_likes);

        rvTopStreams = findViewById(R.id.rv_top_streams);
        rvTopLikes = findViewById(R.id.rv_top_likes);

        // Tắt NestedScrolling để cuộn mượt hơn khi nằm trong NestedScrollView
        rvTopStreams.setLayoutManager(new LinearLayoutManager(this));
        rvTopStreams.setNestedScrollingEnabled(false);

        rvTopLikes.setLayoutManager(new LinearLayoutManager(this));
        rvTopLikes.setNestedScrollingEnabled(false);
    }

    /**
     * Tách riêng hàm gắn sự kiện Click
     */
    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvSeeAllStreams.setOnClickListener(v -> openAllSongs(currentArtistId));
        tvSeeAllLikes.setOnClickListener(v -> openAllSongs(currentArtistId));
    }

    /**
     * Hàm gọi tổng hợp dữ liệu
     */
    private void loadAnalyticsData(String artistId) {
        fetchAnalyticsStats(artistId);
        fetchTopSongs(artistId);
    }

    /**
     * Gọi API lấy 3 Thẻ số liệu và Số người theo dõi
     * Gọi API lấy 3 Thẻ số liệu và Số người theo dõi
     */
    private void fetchAnalyticsStats(String artistId) {
        AppRepository.getInstance(this).getArtistStats(artistId, new AppRepository.ArtistStatsCallback() {
            @Override
            public void onSuccess(ArtistStats stats) {
                // Kiểm tra Activity còn sống không trước khi cập nhật UI (Chống crash rò rỉ bộ nhớ)
                if (isFinishing() || isDestroyed() || stats == null) return;

                tvStreams.setText(numberFormat.format(stats.totalStreams));
                tvLikes.setText(numberFormat.format(stats.totalLikes));
                tvSongs.setText(numberFormat.format(stats.totalSongs));
                tvTotalFollowers.setText(numberFormat.format(stats.totalListeners) + " Người theo dõi");
            }

            @Override
            public void onError(String message) {
                // Log lỗi ngầm hoặc hiển thị thông báo nhẹ nhàng
            }
        });
    }

    /**
     * Gọi API lấy Danh sách bài hát và Lọc Top 5
     */
    private void fetchTopSongs(String artistId) {
        AppRepository.getInstance(this).getSongsByArtist(artistId, new AppRepository.SongListCallback() {
            @Override
            public void onSuccess(ArrayList<Song> allSongs) {
                if (isFinishing() || isDestroyed() || allSongs == null) return;

                if (allSongs.isEmpty()) {
                    // Xử lý UI khi nghệ sĩ chưa có bài hát nào (Giấu các nút xem tất cả đi)
                    tvSeeAllStreams.setVisibility(View.GONE);
                    tvSeeAllLikes.setVisibility(View.GONE);
                    return;
                }

                // ==========================================
                // 1. LỌC TOP 5 LƯỢT NGHE
                // ==========================================
                ArrayList<Song> byStreams = new ArrayList<>(allSongs);
                // Dùng Integer.compare vì hàm getPlays() trả về kiểu int
                Collections.sort(byStreams, (s1, s2) -> Integer.compare(s2.getPlays(), s1.getPlays()));

                // Cắt mảng an toàn (Math.min chống lỗi văng app khi list có ít hơn 5 bài)
                int streamLimit = Math.min(5, byStreams.size());
                List<Song> top5Streams = byStreams.subList(0, streamLimit);

                topStreamsAdapter = new SongAdapter(ArtistAnalyticsActivity.this, new ArrayList<>(top5Streams), new SongAdapter.OnSongActionListener() {
                    @Override public void onSongClick(Song song, int position) { playSong(song, top5Streams); }
                    @Override public void onMenuClick(Song song, int position, String actionId) { /* Có thể bổ sung sau */ }
                });
                // THÊM DÒNG NÀY VÀO: Bật chế độ hiển thị số liệu
                topStreamsAdapter.setAnalyticsMode(true);
                rvTopStreams.setAdapter(topStreamsAdapter);
                tvSeeAllStreams.setVisibility(byStreams.size() > 5 ? View.VISIBLE : View.GONE);


                // ==========================================
                // 2. LỌC TOP 5 LƯỢT THÍCH
                // ==========================================
                ArrayList<Song> byLikes = new ArrayList<>(allSongs);
                Collections.sort(byLikes, (s1, s2) -> Integer.compare(s2.getLikes(), s1.getLikes()));

                int likeLimit = Math.min(5, byLikes.size());
                List<Song> top5Likes = byLikes.subList(0, likeLimit);

                topLikesAdapter = new SongAdapter(ArtistAnalyticsActivity.this, new ArrayList<>(top5Likes), new SongAdapter.OnSongActionListener() {
                    @Override public void onSongClick(Song song, int position) { playSong(song, top5Likes); }
                    @Override public void onMenuClick(Song song, int position, String actionId) { /* Có thể bổ sung sau */ }
                });
                topLikesAdapter.setAnalyticsMode(true);
                rvTopLikes.setAdapter(topLikesAdapter);
                tvSeeAllLikes.setVisibility(byLikes.size() > 5 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(ArtistAnalyticsActivity.this, "Lỗi tải bài hát: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Hàm tiện ích: Phát nhạc và đưa danh sách Top 5 vào Queue
     */
    private void playSong(Song selectedSong, List<Song> currentList) {
        AppRepository.getInstance(this).setCurrentQueue(new ArrayList<>(currentList), selectedSong.getId());
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_SONG_ID, selectedSong.getId());
        intent.putExtra("start_playback", true);
        startActivity(intent);
    }

    /**
     * Hàm tiện ích: Mở trang Danh sách toàn bộ bài hát
     */
    private void openAllSongs(String artistId) {
        Intent intent = new Intent(this, ArtistSongsActivity.class);
        intent.putExtra(ArtistDetailActivity.EXTRA_ARTIST_ID, artistId);
        startActivity(intent);
    }
}