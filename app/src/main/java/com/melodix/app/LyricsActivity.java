package com.melodix.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Repository.PlaybackRepository;
import com.melodix.app.Service.AudioPlayerService;
import com.melodix.app.Utils.ThemeUtils;
import com.melodix.app.View.adapters.LyricAdapter;

public class LyricsActivity extends AppCompatActivity {
    private PlaybackRepository playbackRepository;
    private AppRepository appRepository;
    private Song currentSong;
    private LyricAdapter lyricAdapter;
    private RecyclerView rvLyrics;
    private int currentLyricIndex = -1;

    
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentSong != null && AudioPlayerService.isPlaying()) {
                int position = AudioPlayerService.getCurrentPosition();
                syncLyrics(position);
            }
            
            progressHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            
            windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            
            windowInsetsController.setSystemBarsBehavior(
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
        playbackRepository = PlaybackRepository.getInstance();
        appRepository = AppRepository.getInstance(this);

        super.onCreate(savedInstanceState);

        
        setContentView(R.layout.activity_lyrics);

        String songId = getIntent().getStringExtra(PlayerActivity.EXTRA_SONG_ID);
        if (songId == null) {
            finish();
            return;
        }

        currentSong = playbackRepository.getCurrentSong();
        if (currentSong == null) {
            finish();
            return;
        }

        
        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvTitle = findViewById(R.id.tv_title);
        if (tvTitle != null) {
            tvTitle.setText(currentSong.getTitle());
        }

        
        rvLyrics = findViewById(R.id.rv_lyrics_full);

        
        rvLyrics.setItemAnimator(null);

        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this) {
            @Override
            public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
                LinearSmoothScroller scroller = new LinearSmoothScroller(recyclerView.getContext()) {
                    @Override
                    public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                        return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
                    }

                    @Override
                    protected float calculateSpeedPerPixel(android.util.DisplayMetrics displayMetrics) {
                        return 150f / displayMetrics.densityDpi; 
                    }

                    @Override
                    protected int calculateTimeForDeceleration(int dx) {
                        return (int) Math.ceil(super.calculateTimeForDeceleration(dx) * 1.5);
                    }
                };
                scroller.setTargetPosition(position);
                startSmoothScroll(scroller);
            }
        };
        rvLyrics.setLayoutManager(layoutManager);

        
        
        lyricAdapter = new LyricAdapter(currentSong.getLyrics(), new LyricAdapter.OnLyricClickListener() {
            @Override
            public void onLyricClick(long timeMs) {
                
                syncLyrics((int) timeMs);

                
                android.content.Intent intent = new android.content.Intent(LyricsActivity.this, AudioPlayerService.class);
                intent.setAction(AudioPlayerService.ACTION_SEEK_TO);
                intent.putExtra(AudioPlayerService.EXTRA_SEEK, (int) timeMs);
                startService(intent);
            }
        });
        rvLyrics.setAdapter(lyricAdapter);
    }

    private void syncLyrics(int position) {
        if (currentSong == null || lyricAdapter == null || currentSong.getLyrics() == null) return;

        int index = 0;
        
        int displayPosition = position + 300;

        for (int i = 0; i < currentSong.getLyrics().size(); i++) {
            if (displayPosition >= currentSong.getLyrics().get(i).timeMs) {
                index = i;
            }
        }

        if (index != currentLyricIndex) {
            currentLyricIndex = index;
            lyricAdapter.setHighlightIndex(index);

            
            if (rvLyrics.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                rvLyrics.smoothScrollToPosition(index);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressHandler.post(progressRunnable);

        
        if (AudioPlayerService.isPlaying()) {
            syncLyrics(AudioPlayerService.getCurrentPosition());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        progressHandler.removeCallbacks(progressRunnable);
    }
}
