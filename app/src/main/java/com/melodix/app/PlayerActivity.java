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
import com.melodix.app.Repository.DownloadRepository;
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
import com.melodix.app.Model.AppDatabase;
import com.melodix.app.Model.DownloadedSong;
import android.provider.MediaStore;
import android.util.Log;
import java.util.List;
import android.database.Cursor;


import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {
    public static final String EXTRA_SONG_ID = "extra_song_id";
    public static final String EXTRA_AUTO_PLAY = "auto_play";


    private AppRepository repository;
    private Song currentSong;
    private LyricAdapter lyricAdapter;
    private SeekBar seekBar;
    private TextView tvElapsed;
    private TextView tvTotal;
    private ImageButton btnPlayPause;
    private boolean isUserSeeking = false;
    private int currentLyricIndex = -1;
    private boolean isAutoPlay = false;


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

        isAutoPlay = getIntent().getBooleanExtra(EXTRA_AUTO_PLAY, false);
        seekBar = findViewById(R.id.seek_bar);
        tvElapsed = findViewById(R.id.tv_elapsed);
        tvTotal = findViewById(R.id.tv_total);
        btnPlayPause = findViewById(R.id.btn_play_pause);

        // ====================== 1. ƯU TIÊN CAO NHẤT: Mở từ File Manager (My Files) ======================
        Uri fileUri = getIntent().getData();
        if (fileUri != null && "content".equals(fileUri.getScheme())) {
            Log.d("PlayerActivity", "Mở từ My Files - URI: " + fileUri);
            handleOpenDownloadedFile(fileUri);
            setupUIEvents();        // Quan trọng: vẫn cần khởi tạo nút bấm
            return;
        }

        // ====================== 2. Các trường hợp mở từ trong app ======================
        String songId = getIntent().getStringExtra(EXTRA_SONG_ID);

        // Kiểm tra offline song đang có trong repository (dùng cho trường hợp mở từ Mini Player, Library...)
        Song currentInRepo = PlaybackRepository.getInstance().getCurrentSong();

        if (currentInRepo != null && currentInRepo.getAudioUrl() != null &&
                (currentInRepo.getAudioUrl().startsWith("/") ||
                        currentInRepo.getAudioUrl().startsWith("content://") ||
                        currentInRepo.getAudioUrl().startsWith("file://"))) {

            // Đây là bài hát offline → không fetch từ database
            currentSong = currentInRepo;
            loadSong(currentSong.getId());
            setupUIEvents();

            Intent serviceIntent = new Intent(this, AudioPlayerService.class);
            serviceIntent.setAction(AudioPlayerService.ACTION_PLAY_SONG);
            serviceIntent.putExtra(AudioPlayerService.EXTRA_SONG_ID, currentSong.getId());
            ContextCompat.startForegroundService(this, serviceIntent);

            return;
        }

        // === 3. Deep link từ browser ===
        Uri data = getIntent().getData();
        if (data != null && "melodix".equals(data.getScheme())) {
            songId = data.getLastPathSegment();
            fetchSongFromDbAndPlay(songId);
            return;
        }

        // === 4. Intent từ MainActivity hoặc Mini Player ===
        if (songId != null) {
            String cachedSongId = AudioPlayerService.getCurrentSongId();
            if (cachedSongId == null && repository.getCurrentQueueSong() != null) {
                cachedSongId = repository.getCurrentQueueSong().getId();
            }

            if (cachedSongId == null || !songId.equals(cachedSongId)) {
                fetchSongFromDbAndPlay(songId);
                return;
            }
        }

        // === 5. Mở lại bài đang nghe ===
        if (songId == null) songId = AudioPlayerService.getCurrentSongId();
        if (songId == null && repository.getCurrentQueueSong() != null) {
            songId = repository.getCurrentQueueSong().getId();
        }

        if (songId == null) {
            finish();
            return;
        }

        // Khởi động service nếu cần
        boolean startPlayback = getIntent().getBooleanExtra("start_playback", false);
        if (isAutoPlay || startPlayback || AudioPlayerService.getCurrentSongId() == null ||
                !songId.equals(AudioPlayerService.getCurrentSongId())) {

            Intent serviceIntent = new Intent(this, AudioPlayerService.class);
            serviceIntent.setAction(AudioPlayerService.ACTION_PLAY_SONG);
            serviceIntent.putExtra(AudioPlayerService.EXTRA_SONG_ID, songId);
            ContextCompat.startForegroundService(this, serviceIntent);
        }

        loadSong(songId);
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
                DownloadRepository downloadRepo = new DownloadRepository(this);
                downloadRepo.enqueueDownload(currentSong);

                Toast.makeText(this, "Đang tải: " + currentSong.getTitle(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không có bài hát để tải", Toast.LENGTH_SHORT).show();
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

    private void handleOpenDownloadedFile(Uri uri) {
        if (uri == null) {
            finish();
            return;
        }

        String uriString = uri.toString();
        Log.d("PlayerActivity", "Opening file with URI: " + uriString);

        new Thread(() -> {
            DownloadedSong downloaded = null;

            // Tìm theo tên file từ URI
            String fileName = getFileNameFromUri(uri);
            if (fileName != null && fileName.endsWith(".mp3")) {
                String titleFromFile = fileName.replace(".mp3", "");
                List<DownloadedSong> allSongs = AppDatabase.getInstance(this)
                        .downloadedSongDao()
                        .getAllDownloadedSync();

                for (DownloadedSong song : allSongs) {
                    if (song.title != null && song.title.equals(titleFromFile)) {
                        downloaded = song;
                        Log.d("PlayerActivity", "Found by title: " + song.title);
                        break;
                    }
                }
            }

            // Nếu không tìm thấy, thử lấy đường dẫn thực từ URI
            if (downloaded == null) {
                String realPath = getRealPathFromUri(uri);
                if (realPath != null) {
                    downloaded = AppDatabase.getInstance(this)
                            .downloadedSongDao()
                            .getByLocalPath(realPath);
                    if (downloaded != null) {
                        Log.d("PlayerActivity", "Found by realPath: " + downloaded.title);
                    }
                }
            }

            if (downloaded != null) {
                final DownloadedSong finalSong = downloaded;
                runOnUiThread(() -> {
                    // Tạo Song offline với đầy đủ thông tin
                    Song offlineSong = new Song(
                            finalSong.songId,
                            finalSong.title != null ? finalSong.title : "Unknown Title",
                            null,
                            finalSong.artistName != null && !finalSong.artistName.isEmpty()
                                    ? finalSong.artistName : "Unknown Artist",
                            null, null,
                            finalSong.coverUrl != null ? finalSong.coverUrl : "",
                            finalSong.localAudioPath,
                            null,
                            "Bài hát đã tải về",
                            finalSong.durationSeconds,
                            0, 0
                    );

                    // *** QUAN TRỌNG: Cập nhật PlaybackRepository với bài hát mới ***
                    PlaybackRepository.getInstance().setCurrentSong(offlineSong);

                    // Phát bài hát mới
                    Intent serviceIntent = new Intent(this, AudioPlayerService.class);
                    serviceIntent.setAction(AudioPlayerService.ACTION_PLAY_SONG);
                    serviceIntent.putExtra(AudioPlayerService.EXTRA_SONG_ID, finalSong.songId);
                    ContextCompat.startForegroundService(this, serviceIntent);

                    loadSong(finalSong.songId);
                    setupUIEvents();
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Không tìm thấy bài hát đã tải: " + fileName, Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    // Helper: Trích xuất ID số từ URI content (ví dụ: content://.../1847 -> 1847)
    private long extractMediaIdFromUri(Uri uri) {
        if (uri == null) return -1;

        String scheme = uri.getScheme();
        if (!"content".equals(scheme)) return -1;

        String path = uri.getPath();
        if (path == null) return -1;

        try {
            // Lấy phần cuối của path (số ID)
            String lastSegment = path.substring(path.lastIndexOf('/') + 1);
            return Long.parseLong(lastSegment);
        } catch (Exception e) {
            Log.e("PlayerActivity", "Cannot extract media ID", e);
            return -1;
        }
    }

    // Helper method: Lấy đường dẫn thực từ URI (cho Android 9 trở xuống)
    private String getRealPathFromUri(Uri uri) {
        if (uri == null) return null;

        String scheme = uri.getScheme();
        if (scheme == null) return null;

        if ("file".equals(scheme)) {
            return uri.getPath();
        }

        if ("content".equals(scheme)) {
            String[] projection = {MediaStore.MediaColumns.DATA};
            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                    return cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                Log.e("PlayerActivity", "Error getting real path", e);
            }
        }
        return null;
    }

    // Helper method: Lấy tên file từ URI
    private String getFileNameFromUri(Uri uri) {
        if (uri == null) return null;

        String scheme = uri.getScheme();
        if ("file".equals(scheme)) {
            String path = uri.getPath();
            if (path != null) {
                return path.substring(path.lastIndexOf('/') + 1);
            }
        }

        if ("content".equals(scheme)) {
            String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                    return cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                Log.e("PlayerActivity", "Error getting file name", e);
            }
        }
        return null;
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
    private void fetchSongFromDbAndPlay(String songId) {
        AppRepository.getInstance(this).getSongByIdAsync(songId, new AppRepository.SingleSongCallback() {
            @Override
            public void onSuccess(Song song) {
                if (isFinishing() || isDestroyed()) return;

                // 1. Nhét bài hát vừa lấy được vào kho lưu trữ cục bộ
                PlaybackRepository.getInstance().setCurrentSong(song);

                // 2. Lúc này Data đã có đủ, ta tự tin ra lệnh cho Service phát nhạc
                Intent serviceIntent = new Intent(PlayerActivity.this, AudioPlayerService.class);
                serviceIntent.setAction(AudioPlayerService.ACTION_PLAY_SONG);
                serviceIntent.putExtra(AudioPlayerService.EXTRA_SONG_ID, song.getId());
                androidx.core.content.ContextCompat.startForegroundService(PlayerActivity.this, serviceIntent);

                // 3. Gọi hàm vẽ Giao diện (Tên bài, ảnh bìa...)
                loadSong(song.getId());

                // 4. (Quan trọng) Gọi lại hàm cài đặt sự kiện để các nút không bị đơ
                setupUIEvents();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(PlayerActivity.this, "Lỗi tải bài hát: " + message, Toast.LENGTH_SHORT).show();
                finish(); // Lỗi thì đóng luôn màn hình
            }
        });
    }
}