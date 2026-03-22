package com.melodix.app; // Sửa lại package này cho đúng với thư mục của bạn nhé!

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.melodix.app.Data.MockDataStore;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Service.PlayerManager;

import java.util.Locale;

public class NowPlayingActivity extends AppCompatActivity {

    private MaterialToolbar toolbarNowPlaying;
    private ImageView ivNowPlayingCover;
    private TextView tvNowPlayingTitle;
    private TextView tvNowPlayingArtist;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;

    private ImageView btnShuffle;
    private ImageView btnPrev;
    private FloatingActionButton fabPlayPauseBig;
    private ImageView btnNext;
    private ImageView btnRepeat;

    private PlayerManager playerManager;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false; // Biến kiểm tra xem user có đang dùng tay kéo Seekbar không

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false); // Tràn viền
        setContentView(R.layout.activity_now_playing);

        initViews();
        setupToolbar();
        setupPlayer();
        setupControls();
    }

    private void initViews() {
        toolbarNowPlaying = findViewById(R.id.toolbarNowPlaying);
        ivNowPlayingCover = findViewById(R.id.ivNowPlayingCover);
        tvNowPlayingTitle = findViewById(R.id.tvNowPlayingTitle);
        tvNowPlayingArtist = findViewById(R.id.tvNowPlayingArtist);
        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnPrev = findViewById(R.id.btnPrev);
        fabPlayPauseBig = findViewById(R.id.fabPlayPauseBig);
        btnNext = findViewById(R.id.btnNext);
        btnRepeat = findViewById(R.id.btnRepeat);
    }

    private void setupToolbar() {
        toolbarNowPlaying.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupPlayer() {
        playerManager = PlayerManager.getInstance(this);

        // Lắng nghe trạng thái Play/Pause để đổi nút
        playerManager.getIsPlayingLiveData().observe(this, isPlaying -> {
            fabPlayPauseBig.setImageResource(isPlaying ? R.drawable.ic_pause_24 : R.drawable.ic_play_arrow_24);
            if (isPlaying) {
                startProgressUpdate(); // Nhạc chạy thì thanh seekbar chạy
            } else {
                stopProgressUpdate();  // Nhạc dừng thì thanh seekbar dừng
            }
        });

        // Lắng nghe xem đang hát bài nào để đổi Ảnh và Tên
        playerManager.getCurrentSongIdLiveData().observe(this, songId -> {
            if (songId != null && !songId.isEmpty()) {
                Song song = MockDataStore.getSongById(songId);
                if (song != null) {
                    tvNowPlayingTitle.setText(song.getTitle());
                    tvNowPlayingArtist.setText(song.getArtistName());
                    Glide.with(this)
                            .load(song.getCoverUrl())
                            .placeholder(new ColorDrawable(Color.parseColor("#20312B")))
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(ivNowPlayingCover);
                }
            }
        });
    }

    private void setupControls() {
        // Nút Play/Pause, Next, Prev
        fabPlayPauseBig.setOnClickListener(v -> playerManager.togglePlayPause());
        btnNext.setOnClickListener(v -> playerManager.skipToNext());
        btnPrev.setOnClickListener(v -> playerManager.skipToPrevious());

        // Xử lý khi User cầm tay kéo thanh Tua nhạc
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatDuration(progress)); // Hiện thời gian lúc đang kéo
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true; // Báo là user đang cầm, app không được tự động chèn ép update nữa
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                playerManager.seekTo(seekBar.getProgress()); // Thả tay ra thì tua nhạc tới đó
            }
        });
    }

    // Vòng lặp đếm thời gian mỗi giây (1000ms)
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isUserSeeking && playerManager != null) {
                long currentPos = playerManager.getCurrentPosition();
                long duration = playerManager.getDuration();

                if (duration > 0) {
                    seekBar.setMax((int) duration);
                    seekBar.setProgress((int) currentPos);
                    tvCurrentTime.setText(formatDuration(currentPos));
                    tvTotalTime.setText(formatDuration(duration));
                }
            }
            progressHandler.postDelayed(this, 1000); // Lặp lại sau 1 giây
        }
    };

    private void startProgressUpdate() {
        progressHandler.removeCallbacks(progressRunnable);
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdate() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdate(); // Nhớ tắt vòng lặp khi thoát màn hình để chống sập RAM
    }

    private String formatDuration(long durationMs) {
        long totalSeconds = durationMs / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}