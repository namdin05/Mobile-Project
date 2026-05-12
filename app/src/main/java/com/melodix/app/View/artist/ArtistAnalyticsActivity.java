package com.melodix.app.View.artist; 

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

    
    private TextView tvStreams, tvLikes, tvSongs;
    private TextView tvSeeAllStreams, tvSeeAllLikes;
    private RecyclerView rvTopStreams, rvTopLikes;

    
    private SongAdapter topStreamsAdapter, topLikesAdapter;

    
    private final NumberFormat numberFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    private String currentArtistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_analytics);

        initViews();
        setupListeners();

        
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

        tvSeeAllStreams = findViewById(R.id.tv_see_all_streams);
        tvSeeAllLikes = findViewById(R.id.tv_see_all_likes);

        rvTopStreams = findViewById(R.id.rv_top_streams);
        rvTopLikes = findViewById(R.id.rv_top_likes);

        
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
        tvSeeAllStreams.setOnClickListener(v -> openAllSongs(currentArtistId, "streams"));
        tvSeeAllLikes.setOnClickListener(v -> openAllSongs(currentArtistId, "likes"));
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
                
                if (isFinishing() || isDestroyed() || stats == null) return;

                tvStreams.setText(numberFormat.format(stats.totalStreams));
                tvLikes.setText(numberFormat.format(stats.totalLikes));
                tvSongs.setText(numberFormat.format(stats.totalSongs));
            }

            @Override
            public void onError(String message) {
                
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
                    
                    tvSeeAllStreams.setVisibility(View.GONE);
                    tvSeeAllLikes.setVisibility(View.GONE);
                    return;
                }

                ArrayList<Song> byStreams = new ArrayList<>(allSongs);
                
                Collections.sort(byStreams, (s1, s2) -> Integer.compare(s2.getPlays(), s1.getPlays()));

                
                int streamLimit = Math.min(5, byStreams.size());
                List<Song> top5Streams = byStreams.subList(0, streamLimit);

                topStreamsAdapter = new SongAdapter(ArtistAnalyticsActivity.this, new ArrayList<>(top5Streams), new SongAdapter.OnSongActionListener() {
                    @Override public void onSongClick(Song song, int position) { playSong(song, top5Streams); }
                    @Override public void onMenuClick(Song song, int position, String actionId) { /* Có thể bổ sung sau */ }
                });
                
                topStreamsAdapter.setAnalyticsMode(true);
                rvTopStreams.setAdapter(topStreamsAdapter);
                tvSeeAllStreams.setVisibility(byStreams.size() > 5 ? View.VISIBLE : View.GONE);

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
    private void openAllSongs(String artistId, String sortType) {
        Intent intent = new Intent(this, ArtistSongsActivity.class);
        intent.putExtra(ArtistDetailActivity.EXTRA_ARTIST_ID, artistId);
        intent.putExtra("sort_type", sortType);
        startActivity(intent);
    }
}