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
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Repository.PlaybackRepository;
import com.melodix.app.Service.AudioPlayerService;
import com.melodix.app.Utils.AppUiUtils;
import com.melodix.app.Utils.LyricUtils;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.Utils.ResourceUtils;
import com.melodix.app.Utils.ShareUtils;
import com.melodix.app.Utils.ThemeUtils;
import com.melodix.app.Utils.TimeUtils;
import com.melodix.app.View.music.CommentsBottomSheet;
import com.melodix.app.View.adapters.LyricAdapter;
//import com.melodix.app.View.adapters.LyricAdapter;

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

    // tao bo lap lich
    private final android.os.Handler progressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    // gan Runnable vao 1 bien (type Runnable) de sau nay tieu huy duoc no
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            // 1. NHIỆM VỤ MỚI: Tự động soi xem ID bài hát dưới Service có khác với ID trên màn hình không
            String activeSongId = AudioPlayerService.getCurrentSongId();
            if (activeSongId != null && (currentSong == null || !activeSongId.equals(currentSong.getId()))) {
                // Nếu thấy khác -> Bắt buộc tải lại ảnh và tên bài mới ngay lập tức!
                loadSong(activeSongId);
            }
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

        // 1. LẤY ID BÀI HÁT TỪ INTENT HOẶC TỪ LINK TRÌNH DUYỆT
        String intentSongId = getIntent().getStringExtra(EXTRA_SONG_ID);
        Uri data = getIntent().getData();

        String deepLinkSongId = null;
        if (data != null) {
            // Không thèm check Scheme là melodix hay http nữa, cứ có link là lấy khúc đuôi (ID)
            deepLinkSongId = data.getLastPathSegment();
        }

        // Chốt lại ID cuối cùng mà Activity này phải mở
        String targetSongId = deepLinkSongId != null ? deepLinkSongId : intentSongId;

        // 📸 LẮP CAMERA SỐ 0 NGAY CỬA VÀO:
        android.util.Log.e("TEST_NEXT", "0. Vừa mở App! Link nhận được: [" + data + "] | ID chốt lại: [" + targetSongId + "]");

        // 2. KIỂM TRA VÀ ĐIỀU HƯỚNG
        if (targetSongId != null) {
            Song currentRepoSong = PlaybackRepository.getInstance().getCurrentSong();

            // 👇 CHÌA KHÓA SỬA LỖI SHARE:
            // Nếu App đang nhớ bài A, nhưng Intent yêu cầu bài B (Khác nhau) -> Gọi API tải bài B!
            if (currentRepoSong == null || !targetSongId.equals(currentRepoSong.getId())) {
                fetchSongFromDbAndPlay(targetSongId);
                return; // Dừng lại chờ API tải xong
            }
            // Nếu yêu cầu đúng bài đang nằm trong Queue (Mở từ trong App bình thường)
            else {
                boolean startPlayback = getIntent().getBooleanExtra("start_playback", false);
                if (startPlayback) {
                    Intent serviceIntent = new Intent(this, AudioPlayerService.class);
                    serviceIntent.setAction(AudioPlayerService.ACTION_PLAY_SONG);
                    serviceIntent.putExtra(AudioPlayerService.EXTRA_SONG_ID, targetSongId);
                    androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);
                }
                loadSong(targetSongId);
                setupUIEvents();
                return;
            }
        }

        // 3. MỞ TỪ THANH MINI PLAYER (Chỉ mở lại giao diện của bài đang nghe sẵn)
        String currentId = AudioPlayerService.getCurrentSongId();
        if (currentId == null && PlaybackRepository.getInstance().getCurrentSong() != null) {
            currentId = PlaybackRepository.getInstance().getCurrentSong().getId();
        }

        if (currentId == null) {
            finish();
            return;
        }

        loadSong(currentId);
        setupUIEvents();
    }

    // =========================================================
    // HÀM GOM CÁC SỰ KIỆN GIAO DIỆN (GIỮ NGUYÊN CODE CỦA BẠN)
    // =========================================================
    private void setupUIEvents() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // COMMENT BUTTON
        findViewById(R.id.btn_comments).setOnClickListener(v -> {
            if (currentSong != null) {
                CommentsBottomSheet bottomSheet = CommentsBottomSheet.newInstance(currentSong.getId());
                bottomSheet.show(getSupportFragmentManager(), "CommentsBottomSheet");
            }
        });

        findViewById(R.id.btn_share).setOnClickListener(v -> {
            // Lớp bảo vệ: Chỉ cho share khi đã có dữ liệu bài hát
            if (currentSong != null && currentSong.getId() != null) {
                com.melodix.app.Utils.ShareUtils.shareContent(
                        PlayerActivity.this,
                        "song",                // Type là bài hát để MainActivity biết đường đón
                        currentSong.getId(),   // Lấy ID bài hát
                        currentSong.getTitle() // Lấy Tên bài hát
                );
            } else {
                Toast.makeText(PlayerActivity.this, "Đang tải dữ liệu, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            }
        });
//        findViewById(R.id.btn_lyrics).setOnClickListener(v -> openFullLyrics());

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
                        // Ép item nằm đúng tâm màn hình
                        return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
                    }

                    // THÊM HÀM NÀY: Làm chậm tốc độ cuộn (Số càng to cuộn càng chậm)
                    @Override
                    protected float calculateSpeedPerPixel(android.util.DisplayMetrics displayMetrics) {
                        return 150f / displayMetrics.densityDpi;
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
        if (overlay != null) {
            overlay.setOnClickListener(v -> openFullLyrics());
        }
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

    private void openFullLyrics() {
        // Chỉ cho phép mở trang LyricsActivity khi bài hát CÓ tồn tại danh sách lời
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

        // 1. CHUẨN BỊ GIAO DIỆN LỜI BÀI HÁT
        RecyclerView rvLyrics = findViewById(R.id.rv_lyrics);

        // Hiện thông báo đang tải (Tùy chọn)
        ((TextView) findViewById(R.id.tv_ai_summary)).setText("Đang tải lời bài hát...");

        // 2. GỌI HÀM TẢI FILE LRC TỪ MẠNG
        LyricUtils.downloadAndParseLrc(currentSong.getLyricsUrl(), new LyricUtils.LyricCallback() {
            @Override
            public void onLyricsLoaded(ArrayList<LyricLine> lyrics) {
                // Khi tải xong, lưu vào bài hát hiện tại (để cache lại nếu muốn)
                // và nạp vào Adapter
                currentSong.getLyrics().clear();
                currentSong.getLyrics().addAll(lyrics);

                // BẮT LẤY CÁI KHUNG FRAMELAYOUT BÊN NGOÀI
                View lyricsContainer = (View) rvLyrics.getParent();
                android.view.ViewGroup.LayoutParams params = lyricsContainer.getLayoutParams();

                if (lyrics.isEmpty()) {
                    // 1. CHƯA CÓ LỜI: Báo lỗi và khóa bấm
                    ArrayList<LyricLine> emptyMessage = new ArrayList<>();
                    emptyMessage.add(new LyricLine(0, "Chưa có lời bài hát"));
                    rvLyrics.setAdapter(new LyricAdapter(emptyMessage, null));

                    View overlayView = findViewById(R.id.view_lyrics_click_overlay);
                    if (overlayView != null) overlayView.setClickable(false);

                    // ÉP KHUNG THU NHỎ LẠI VỪA KHÍT DÒNG CHỮ
                    params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                    lyricsContainer.setLayoutParams(params);
                } else {
                    // 2. CÓ LỜI: Nạp adapter và mở khóa bấm
                    lyricAdapter = new LyricAdapter(lyrics, null);
                    rvLyrics.setAdapter(lyricAdapter);

                    View overlayView = findViewById(R.id.view_lyrics_click_overlay);
                    if (overlayView != null) overlayView.setClickable(true);

                    // PHỤC HỒI LẠI CHIỀU CAO 200dp NHƯ THIẾT KẾ BAN ĐẦU
                    // (Phải nhân với density để đổi từ dp sang pixel cho chuẩn mọi màn hình)
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

    // =========================================================
    // TÍNH NĂNG CHIA SẺ BÀI HÁT CHO NGƯỜI NGHE
    // =========================================================


    // =========================================================
    // LẤY DỮ LIỆU TỪ DATABASE KHI VÀO TỪ LINK CHIA SẺ
    // =========================================================
    // =========================================================
    // HÀM CHUYÊN DÙNG CHO DEEP LINK (LINK CHIA SẺ)
    // =========================================================
    private void fetchSongFromDbAndPlay(String songId) {
        repository.getSongByIdAsync(songId, new AppRepository.SingleSongCallback() {
            @Override
            public void onSuccess(Song sharedSong) {
                String artistId = sharedSong.getArtistId();

                if (artistId != null && !artistId.isEmpty()) {
                    // 🔔 THÔNG BÁO 1: Đã thấy ID
                    android.widget.Toast.makeText(PlayerActivity.this, "✅ Đã thấy ID Nghệ sĩ: " + artistId, android.widget.Toast.LENGTH_LONG).show();

                    repository.getSongsByArtist(artistId, new AppRepository.SongListCallback() {
                        @Override
                        public void onSuccess(ArrayList<Song> artistSongs) {
                            ArrayList<Song> finalQueue = new ArrayList<>();
                            finalQueue.add(sharedSong);

                            for (Song s : artistSongs) {
                                if (!s.getId().equals(sharedSong.getId())) {
                                    finalQueue.add(s);
                                }
                            }

                            // 🔔 THÔNG BÁO 2: Tìm thấy nhạc
                            android.widget.Toast.makeText(PlayerActivity.this, "🎉 Thành công! Hàng đợi có " + finalQueue.size() + " bài!", android.widget.Toast.LENGTH_LONG).show();

                            PlaybackRepository.getInstance().setQueue(finalQueue, sharedSong.getId());
                            startServiceAndLoadUI(sharedSong.getId());
                        }

                        @Override
                        public void onError(String message) {
                            android.widget.Toast.makeText(PlayerActivity.this, "❌ Lỗi tìm bài liên quan!", android.widget.Toast.LENGTH_SHORT).show();
                            playSoloSong(sharedSong);
                        }
                    });
                } else {
                    // 🔔 THÔNG BÁO 3: LỖI CHÍ MẠNG BỊ NULL
                    android.widget.Toast.makeText(PlayerActivity.this, "❌ LỖI: artistId BỊ NULL! Phát 1 bài...", android.widget.Toast.LENGTH_LONG).show();
                    playSoloSong(sharedSong);
                }
            }

            @Override
            public void onError(String message) {
                android.widget.Toast.makeText(PlayerActivity.this, "Lỗi: " + message, android.widget.Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }    private void playSoloSong(Song song) {
        ArrayList<Song> soloQueue = new ArrayList<>();
        soloQueue.add(song);
        PlaybackRepository.getInstance().setQueue(soloQueue, song.getId());
        startServiceAndLoadUI(song.getId());
    }

    private void startServiceAndLoadUI(String songId) {
        Intent serviceIntent = new Intent(PlayerActivity.this, AudioPlayerService.class);
        serviceIntent.setAction(AudioPlayerService.ACTION_PLAY_SONG);
        serviceIntent.putExtra(AudioPlayerService.EXTRA_SONG_ID, songId);
        androidx.core.content.ContextCompat.startForegroundService(PlayerActivity.this, serviceIntent);

        loadSong(songId);
        setupUIEvents();
    }

    // =========================================================
    // BẮT LINK CHIA SẺ KHI APP ĐANG MỞ SẴN (VƯỢT BẪY SỐ 2)
    // =========================================================
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Cập nhật để getIntent() sau này không bị lỗi

        Uri data = intent.getData();
        String deepLinkSongId = null;

        if (data != null) {
            deepLinkSongId = data.getLastPathSegment(); // Bắt cái ID nằm ở đuôi Link
        }

        // Lấy từ Intent bình thường (nếu có)
        String intentSongId = intent.getStringExtra(EXTRA_SONG_ID);
        String targetSongId = deepLinkSongId != null ? deepLinkSongId : intentSongId;

        android.util.Log.e("TEST_NEXT", "🔥 VÀO BẪY! onNewIntent ĐÃ CHỚP ĐƯỢC LINK MỚI! ID là: [" + targetSongId + "]");

        if (targetSongId != null) {
            Song currentRepoSong = PlaybackRepository.getInstance().getCurrentSong();

            // Nếu bài mới khác bài đang nghe thì tải liền!
            if (currentRepoSong == null || !targetSongId.equals(currentRepoSong.getId())) {
                fetchSongFromDbAndPlay(targetSongId);
            } else {
                android.util.Log.e("TEST_NEXT", "⚠️ Bài hát giống y hệt bài đang nghe, bỏ qua tải lại!");
            }
        }
    }
}