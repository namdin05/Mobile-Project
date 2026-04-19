package com.melodix.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.melodix.app.Model.LyricLine;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Repository.PlaybackRepository;
import com.melodix.app.Service.AudioPlayerService;
import com.melodix.app.Utils.AppUiUtils;
import com.melodix.app.Utils.LyricUtils;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.Utils.TimeUtils;
import com.melodix.app.View.music.CommentsBottomSheet;
import com.melodix.app.View.adapters.LyricAdapter;

import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {
    public static final String EXTRA_SONG_ID = "extra_song_id";

    private AppRepository repository;
    private Song currentSong;
    private LyricAdapter lyricAdapter;
    private SeekBar seekBar;
    private TextView tvElapsed;
    private TextView tvTotal;
    private ImageButton btnPlayPause;
    private boolean isUserSeeking = false;
    private int currentLyricIndex = -1;

    // 👇 1. KHAI BÁO KHIÊN BẢO VỆ Ở ĐÂY 👇
    private boolean isLoadingNewSong = false;

    private final android.os.Handler progressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            // 👇 2. CHẶN KHÔNG CHO VẼ KHI ĐANG TẢI API 👇
            if (isLoadingNewSong) {
                progressHandler.postDelayed(this, 100);
                return;
            }

            String activeSongId = AudioPlayerService.getCurrentSongId();
            if (activeSongId != null && (currentSong == null || !activeSongId.equals(currentSong.getId()))) {
                loadSong(activeSongId);
            }
            if (currentSong != null && !isUserSeeking) {
                int position = AudioPlayerService.getCurrentPosition();
                int duration = AudioPlayerService.getDuration();
                boolean playing = AudioPlayerService.isPlaying();
                updatePlaybackUi(position, duration, playing);
            }
            progressHandler.postDelayed(this, 100);
        }
    };

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 👇 3. CHẶN NHẬN TÍN HIỆU CŨ KHI ĐANG TẢI API 👇
            if (isLoadingNewSong) return;

            String songId = intent.getStringExtra(AudioPlayerService.EXTRA_SONG_ID);
            if (songId != null && (currentSong == null || !songId.equals(currentSong.getId()))) {
                loadSong(songId);
            }
            updateLoopButton();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        repository = AppRepository.getInstance(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        seekBar = findViewById(R.id.seek_bar);
        tvElapsed = findViewById(R.id.tv_elapsed);
        tvTotal = findViewById(R.id.tv_total);
        btnPlayPause = findViewById(R.id.btn_play_pause);

        String songId = null;

        // 1. KIỂM TRA DEEP LINK TRỰC TIẾP TỪ BROWSER
        Uri data = getIntent().getData();
        if (data != null && "melodix".equals(data.getScheme())) {
            songId = data.getLastPathSegment();
            isLoadingNewSong = true; // 👈 DỰNG KHIÊN
            preventGhosting(songId); // Chặn bóng ma
            fetchSongFromDbAndPlay(songId);
            return;
        }

        // 2. KIỂM TRA INTENT DO MAINACTIVITY NÉM SANG
        songId = getIntent().getStringExtra(EXTRA_SONG_ID);
        if (songId != null) {
            String cachedSongId = AudioPlayerService.getCurrentSongId();
            if (cachedSongId == null && repository.getCurrentQueueSong() != null) {
                cachedSongId = repository.getCurrentQueueSong().getId();
            }

            if (cachedSongId == null || !songId.equals(cachedSongId)) {
                isLoadingNewSong = true; // 👈 DỰNG KHIÊN
                preventGhosting(songId); // Chặn bóng ma
                fetchSongFromDbAndPlay(songId);
                return;
            }
        }

        // 3. NẾU MỞ APP BÌNH THƯỜNG (Bấm vào thanh Mini Player)
        if (songId == null) songId = AudioPlayerService.getCurrentSongId();
        if (songId == null && repository.getCurrentQueueSong() != null) songId = repository.getCurrentQueueSong().getId();

        if (songId == null) {
            finish();
            return;
        }

        boolean startPlayback = getIntent().getBooleanExtra("start_playback", false);
        if (startPlayback || AudioPlayerService.getCurrentSongId() == null) {
            Intent serviceIntent = new Intent(this, AudioPlayerService.class);
            serviceIntent.setAction(AudioPlayerService.ACTION_PLAY_SONG);
            serviceIntent.putExtra(AudioPlayerService.EXTRA_SONG_ID, songId);
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);
        }

        loadSong(songId);
        setupUIEvents();
    }

    // ========================================================
    // 👇 HÀM BÙA CHÚ: CHẶN BÓNG MA CHỚP GIẬT 👇
    // ========================================================
    private void preventGhosting(String incomingSongId) {
        Song currentCachedSong = PlaybackRepository.getInstance().getCurrentSong();
        if (incomingSongId != null && (currentCachedSong == null || !incomingSongId.equals(currentCachedSong.getId()))) {
            // Ép Service nín thở chờ API gọi nhạc mới về
            PlaybackUtils.sendAction(this, AudioPlayerService.ACTION_PAUSE);

            // Xóa thông tin cũ trên màn hình
            ((TextView) findViewById(R.id.tv_title)).setText("Đang tải bài hát...");
            ((TextView) findViewById(R.id.tv_subtitle)).setText("Vui lòng đợi...");
            ((ImageView) findViewById(R.id.img_cover)).setImageDrawable(null);        }
    }
    // 👆 =================================================== 👆

    private void setupUIEvents() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_comments).setOnClickListener(v -> {
            if (currentSong != null) {
                CommentsBottomSheet bottomSheet = CommentsBottomSheet.newInstance(currentSong.getId());
                bottomSheet.show(getSupportFragmentManager(), "CommentsBottomSheet");
            }
        });

        findViewById(R.id.btn_share).setOnClickListener(v -> {
            if (currentSong != null && currentSong.getId() != null) {
                com.melodix.app.Utils.ShareUtils.shareContent(
                        PlayerActivity.this, "song", currentSong.getId(), currentSong.getTitle()
                );
            } else {
                Toast.makeText(PlayerActivity.this, "Đang tải dữ liệu, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_speed).setOnClickListener(v -> AppUiUtils.showSpeedDialog(this));
        findViewById(R.id.btn_timer).setOnClickListener(v -> AppUiUtils.showSleepTimerDialog(this));
        findViewById(R.id.btn_download).setOnClickListener(v -> {
            if (currentSong != null) {
                boolean downloaded = repository.toggleDownloadSong(currentSong.getId());
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
                ((TextView) findViewById(R.id.tv_ai_summary)).setText(repository.getAiSummaryForSong(currentSong.getId()));
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

        View overlay = findViewById(R.id.view_lyrics_click_overlay);
        if (overlay != null) {
            overlay.setOnClickListener(v -> openFullLyrics());
        }

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

    private void openFullLyrics() {
        if (currentSong != null && currentSong.getLyrics() != null && !currentSong.getLyrics().isEmpty()) {
            Intent intent = new Intent(this, LyricsActivity.class);
            intent.putExtra(EXTRA_SONG_ID, currentSong.getId());
            startActivity(intent);
        }
    }

    private void loadSong(String songId) {
        currentSong = PlaybackRepository.getInstance().getCurrentSong();
        if (currentSong == null) return;
        currentLyricIndex = -1;

        ImageView cover = findViewById(R.id.img_cover);
        ((TextView) findViewById(R.id.tv_title)).setText(currentSong.getTitle());
        ((TextView) findViewById(R.id.tv_subtitle)).setText(currentSong.getArtistName());
        tvTotal.setText(TimeUtils.formatDuration(currentSong.getDurationSeconds()));

        Glide.with(this).load(currentSong.getCoverUrl()).into(cover);

        RecyclerView rvLyrics = findViewById(R.id.rv_lyrics);
        ((TextView) findViewById(R.id.tv_ai_summary)).setText("Đang tải lời bài hát...");

        LyricUtils.downloadAndParseLrc(currentSong.getLyricsUrl(), new LyricUtils.LyricCallback() {
            @Override
            public void onLyricsLoaded(ArrayList<LyricLine> lyrics) {
                currentSong.getLyrics().clear();
                currentSong.getLyrics().addAll(lyrics);

                View lyricsContainer = (View) rvLyrics.getParent();
                android.view.ViewGroup.LayoutParams params = lyricsContainer.getLayoutParams();

                if (lyrics.isEmpty()) {
                    ArrayList<LyricLine> emptyMessage = new ArrayList<>();
                    emptyMessage.add(new LyricLine(0, "Chưa có lời bài hát"));
                    rvLyrics.setAdapter(new LyricAdapter(emptyMessage, null));

                    View overlayView = findViewById(R.id.view_lyrics_click_overlay);
                    if (overlayView != null) overlayView.setClickable(false);

                    params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                    lyricsContainer.setLayoutParams(params);
                } else {
                    lyricAdapter = new LyricAdapter(lyrics, null);
                    rvLyrics.setAdapter(lyricAdapter);

                    View overlayView = findViewById(R.id.view_lyrics_click_overlay);
                    if (overlayView != null) overlayView.setClickable(true);

                    params.height = (int) (200 * getResources().getDisplayMetrics().density);
                    lyricsContainer.setLayoutParams(params);
                }
            }
        });
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

        if (currentSong != null && lyricAdapter != null) {
            int index = 0;
            for (int i = 0; i < currentSong.getLyrics().size(); i++) {
                if (position >= currentSong.getLyrics().get(i).timeMs) index = i;
            }
            if (index != currentLyricIndex) {
                currentLyricIndex = index;
                lyricAdapter.setHighlightIndex(index);

                RecyclerView rvLyrics = findViewById(R.id.rv_lyrics);
                if (rvLyrics.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                    rvLyrics.smoothScrollToPosition(index);
                }
            }
        }
    }

    private void updateLoopButton() {
        ImageButton btnLoop = findViewById(R.id.btn_loop);
        boolean looping = AudioPlayerService.isLoopMode();

        int tint = ContextCompat.getColor(this, looping ? R.color.mdx_primary : R.color.mdx_text);
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

    // =========================================================
    // LẤY DỮ LIỆU TỪ DATABASE KHI VÀO TỪ LINK CHIA SẺ
    // =========================================================
    private void fetchSongFromDbAndPlay(String songId) {
        repository.getSongByIdAsync(songId, new AppRepository.SingleSongCallback() {
            @Override
            public void onSuccess(Song song) {
                if (isFinishing() || isDestroyed()) return;

                // 1. LẤY TOÀN BỘ NHẠC (HÀM NÀY CỦA SẾP PHẢI ĐẢM BẢO CÓ DATA)
                ArrayList<Song> allSongs = repository.getAllApprovedSongs();
                ArrayList<Song> fullQueue = new ArrayList<>();

                // Luôn ưu tiên bài được share lên đầu
                fullQueue.add(song);

                // 2. NẾU CÓ NHẠC TRÊN APP THÌ TRỘN VÀO LÀM AUTO-PLAY
                if (allSongs != null && !allSongs.isEmpty()) {
                    ArrayList<Song> shuffled = new ArrayList<>(allSongs);
                    java.util.Collections.shuffle(shuffled);
                    for (Song s : shuffled) {
                        if (!s.getId().equals(song.getId())) {
                            fullQueue.add(s);
                        }
                    }
                }

                // 3. ĐỔ THẲNG VÀO SET_QUEUE (Hàm này trong Repo của sếp đang chạy ngon)
                PlaybackRepository.getInstance().setQueue(fullQueue, song.getId());

                // 4. PHÁT NHẠC
                Intent serviceIntent = new Intent(PlayerActivity.this, AudioPlayerService.class);
                serviceIntent.setAction(AudioPlayerService.ACTION_PLAY_SONG);
                serviceIntent.putExtra(AudioPlayerService.EXTRA_SONG_ID, song.getId());
                startForegroundService(serviceIntent);

                // 5. MỞ KHÓA UI
                isLoadingNewSong = false;
                loadSong(song.getId());
                setupUIEvents();
            }

            @Override
            public void onError(String message) {
                isLoadingNewSong = false;
                Toast.makeText(PlayerActivity.this, "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}