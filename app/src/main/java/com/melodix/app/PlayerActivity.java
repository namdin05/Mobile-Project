package com.melodix.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Repository.PlaybackRepository;
import com.melodix.app.Service.AudioPlayerService;
import com.melodix.app.Utils.AppUiUtils;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.Utils.ResourceUtils;
import com.melodix.app.Utils.ShareUtils;
import com.melodix.app.Utils.ThemeUtils;
import com.melodix.app.Utils.TimeUtils;
//import com.melodix.app.View.adapters.LyricAdapter;

import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {
    public static final String EXTRA_SONG_ID = "extra_song_id";

    private AppRepository repository;
    private Song currentSong;
//    private LyricAdapter lyricAdapter;
    private SeekBar seekBar;
    private TextView tvElapsed;
    private TextView tvTotal;
    private ImageButton btnPlayPause;
    private boolean isUserSeeking = false;
    private int currentLyricIndex = -1;

    // tao bo lap lich
    private final android.os.Handler progressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    // gan Runnable vao 1 bien (type Runnable) de sau nay tieu huy duoc no
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentSong != null && !isUserSeeking) {
                int position = AudioPlayerService.getCurrentPosition();
                int duration = AudioPlayerService.getDuration();
                boolean playing = AudioPlayerService.isPlaying();
                updatePlaybackUi(position, duration, playing);
            }
            progressHandler.postDelayed(this, 100); // dong nay lap vo tan, k huy tao memory leak
        }
    };

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String songId = intent.getStringExtra(AudioPlayerService.EXTRA_SONG_ID);
            if (songId != null && (currentSong == null || !songId.equals(currentSong.id))) {
                loadSong(songId);
            }
            updateLoopButton();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        repository = AppRepository.getInstance(this);
        ThemeUtils.applyNightMode(repository.getCurrentUser() == null || repository.getCurrentUser().darkMode);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        seekBar = findViewById(R.id.seek_bar);
        tvElapsed = findViewById(R.id.tv_elapsed);
        tvTotal = findViewById(R.id.tv_total);
        btnPlayPause = findViewById(R.id.btn_play_pause);

        String songId = getIntent().getStringExtra(EXTRA_SONG_ID);
        if (songId == null) songId = AudioPlayerService.getCurrentSongId();
        if (songId == null && repository.getCurrentQueueSong() != null) songId = repository.getCurrentQueueSong().id;

        if (songId == null) {
            finish();
            return;
        }

        boolean startPlayback = getIntent().getBooleanExtra("start_playback", false);

        if (startPlayback || AudioPlayerService.getCurrentSongId() == null || !songId.equals(AudioPlayerService.getCurrentSongId())) {
            Intent serviceIntent = new Intent(this, AudioPlayerService.class);
            serviceIntent.setAction(AudioPlayerService.ACTION_PLAY_SONG);
            serviceIntent.putExtra(AudioPlayerService.EXTRA_SONG_ID, songId);
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);
        }

        loadSong(songId);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_share).setOnClickListener(v -> {
            if (currentSong != null) ShareUtils.share(this, currentSong.title, getString(R.string.app_share_prefix) + "song/" + currentSong.id);
        });
//        findViewById(R.id.btn_comments).setOnClickListener(v -> {
//            if (currentSong != null) {
//                Intent intent = new Intent(this, CommentsActivity.class);
//                intent.putExtra(CommentsActivity.EXTRA_SONG_ID, currentSong.id);
//                startActivity(intent);
//            }
//        });

//        findViewById(R.id.btn_lyrics).setOnClickListener(v -> openFullLyrics());

//        findViewById(R.id.btn_speed).setOnClickListener(v -> AppUiUtils.showSpeedDialog(this));
//        findViewById(R.id.btn_timer).setOnClickListener(v -> AppUiUtils.showSleepTimerDialog(this));
        findViewById(R.id.btn_download).setOnClickListener(v -> {
            if (currentSong != null) {
                boolean downloaded = repository.toggleDownloadSong(currentSong.id);
                AppUiUtils.toast(this, downloaded ? "Downloaded for offline" : "Removed offline file");
            }
        });

        findViewById(R.id.btn_prev).setOnClickListener(v -> PlaybackUtils.sendAction(this, AudioPlayerService.ACTION_PREVIOUS));
        findViewById(R.id.btn_rewind).setOnClickListener(v -> PlaybackUtils.sendAction(this, AudioPlayerService.ACTION_REWIND));
        findViewById(R.id.btn_forward).setOnClickListener(v -> PlaybackUtils.sendAction(this, AudioPlayerService.ACTION_FORWARD));
        findViewById(R.id.btn_next).setOnClickListener(v -> PlaybackUtils.sendAction(this, AudioPlayerService.ACTION_NEXT));
        btnPlayPause.setOnClickListener(v -> PlaybackUtils.sendAction(this, AudioPlayerService.ACTION_TOGGLE_PLAY));

        findViewById(R.id.btn_ai_summary).setOnClickListener(v -> {
            if (currentSong != null) {
                ((TextView) findViewById(R.id.tv_ai_summary)).setText(repository.getAiSummaryForSong(currentSong.id));
            }
        });

        ImageButton btnLoop = findViewById(R.id.btn_loop);
        btnLoop.setOnClickListener(v -> {
            AudioPlayerService.toggleLoop();
            updateLoopButton();
        });
        updateLoopButton();

        androidx.recyclerview.widget.RecyclerView rvLyrics = findViewById(R.id.rv_lyrics);
        rvLyrics.setItemAnimator(null);
        androidx.recyclerview.widget.LinearLayoutManager layoutManager = new androidx.recyclerview.widget.LinearLayoutManager(this) {
            @Override
            public void smoothScrollToPosition(androidx.recyclerview.widget.RecyclerView recyclerView, androidx.recyclerview.widget.RecyclerView.State state, int position) {
                androidx.recyclerview.widget.LinearSmoothScroller scroller = new androidx.recyclerview.widget.LinearSmoothScroller(recyclerView.getContext()) {

                    @Override
                    public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                        // Ép item nằm đúng tâm màn hình
                        return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
                    }

                    // THÊM HÀM NÀY: Làm chậm tốc độ cuộn (Số càng to cuộn càng chậm)
                    @Override
                    protected float calculateSpeedPerPixel(android.util.DisplayMetrics displayMetrics) {
                        return 150f / displayMetrics.densityDpi; // Mặc định của Android là 25f
                    }

                    // THÊM HÀM NÀY: Kéo dài thời gian hãm phanh lúc gần đến nơi để tạo độ mượt
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

        View overlay = findViewById(R.id.view_lyrics_click_overlay);
//        if (overlay != null) {
//            overlay.setOnClickListener(v -> openFullLyrics());
//        }
        // ------------------------------------------

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) tvElapsed.setText(TimeUtils.formatMillis(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                PlaybackUtils.seekTo(PlayerActivity.this, seekBar.getProgress());
                isUserSeeking = false;
            }
        });
    }

//    private void openFullLyrics() {
//        if (currentSong != null) {
//            Intent intent = new Intent(this, LyricsActivity.class);
//            intent.putExtra(EXTRA_SONG_ID, currentSong.id);
//            startActivity(intent);
//        }
//    }

    private void loadSong(String songId) {
        currentSong = PlaybackRepository.getInstance().getCurrentSong();
        if (currentSong == null) return;
        currentLyricIndex = -1;

        ((ImageView) findViewById(R.id.img_cover)).setImageResource(ResourceUtils.anyDrawable(this, currentSong.coverRes));
        ((TextView) findViewById(R.id.tv_title)).setText(currentSong.title);
        ((TextView) findViewById(R.id.tv_subtitle)).setText(currentSong.artistName + " • " + currentSong.albumName);
        tvTotal.setText(TimeUtils.formatDuration(currentSong.durationSec));

//        lyricAdapter = new LyricAdapter(currentSong.lyrics);
//        androidx.recyclerview.widget.RecyclerView rvLyrics = findViewById(R.id.rv_lyrics);
//        rvLyrics.setAdapter(lyricAdapter);
        ((TextView) findViewById(R.id.tv_ai_summary)).setText("Tap the button above to generate AI summary from listeners' comments.");
    }

    private void updatePlaybackUi(int position, int duration, boolean playing) {
        int displayPosition = position + 500;
        if (displayPosition > duration) displayPosition = duration;

        if (duration > 0) {
            seekBar.setMax(duration);
            seekBar.setProgress(position);
            tvElapsed.setText(TimeUtils.formatMillis(displayPosition));
            tvTotal.setText(TimeUtils.formatMillis(duration));
        }
        btnPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);

//        if (currentSong != null && lyricAdapter != null) {
//            int index = 0;
//            for (int i = 0; i < currentSong.lyrics.size(); i++) {
//                if (position >= currentSong.lyrics.get(i).timeMs) index = i;
//            }
//            if (index != currentLyricIndex) {
//                currentLyricIndex = index;
//                lyricAdapter.setHighlightIndex(index);
//
//                androidx.recyclerview.widget.RecyclerView rvLyrics = findViewById(R.id.rv_lyrics);
//                if (rvLyrics.getScrollState() == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE) {
//                    rvLyrics.smoothScrollToPosition(index);
//                }
//            }
//        }
    }

    private void updateLoopButton() {
        ImageButton btnLoop = findViewById(R.id.btn_loop);
        boolean looping = AudioPlayerService.isLoopMode();

        int tint = ContextCompat.getColor(
                this,
                looping ? R.color.mdx_primary : R.color.mdx_text
        );

        btnLoop.setColorFilter(tint);
        btnLoop.setAlpha(looping ? 1f : 0.65f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextCompat.registerReceiver(this, stateReceiver, new IntentFilter(AudioPlayerService.ACTION_STATE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED);
        progressHandler.post(progressRunnable);
        updateLoopButton();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(stateReceiver); } catch (Exception ignored) {}
        progressHandler.removeCallbacks(progressRunnable);
    }
}