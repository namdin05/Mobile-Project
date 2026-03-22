package com.melodix.app.View.album;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.melodix.app.Data.MockDataStore;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.melodix.app.Service.PlayerManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.melodix.app.Model.Album;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.AlbumRepository;
import com.melodix.app.View.artist.adapter.ArtistSongAdapter;
import com.melodix.app.ViewModel.AlbumViewModel;
import com.melodix.app.ViewModel.AlbumViewModelFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlbumDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ALBUM_ID = "extra_album_id";

    private AppBarLayout appBarLayout;
    private MaterialToolbar toolbarAlbumDetail;
    private ImageView ivAlbumBackdrop;
    private ImageView ivAlbumCover;
    private LinearLayout layoutAlbumInfo;
    private TextView tvAlbumTypeBadge;
    private TextView tvAlbumTitleLarge;
    private TextView tvAlbumMeta;
    private TextView tvAlbumStats;
    private RecyclerView rvAlbumSongs;
    private FloatingActionButton fabPlayAlbum;

    private AlbumViewModel albumViewModel;
    private ArtistSongAdapter songAdapter;

    private final List<Song> currentSongs = new ArrayList<>();
    private String currentAlbumTitle = "";
    private String currentPlayingSongId = ""; // Thêm biến theo dõi bài đang phát
    private int toolbarBaseHeight;
    private int toolbarBasePaddingLeft;
    private int toolbarBasePaddingTop;
    private int toolbarBasePaddingRight;
    private int toolbarBasePaddingBottom;

    private int headerBasePaddingLeft;
    private int headerBasePaddingTop;
    private int headerBasePaddingRight;
    private int headerBasePaddingBottom;

    private int recyclerBasePaddingLeft;
    private int recyclerBasePaddingTop;
    private int recyclerBasePaddingRight;
    private int recyclerBasePaddingBottom;

    private int fabBaseMarginStart;
    private int fabBaseMarginEnd;
    private int fabBaseMarginBottom;

    public static Intent newIntent(Context context, String albumId) {
        Intent intent = new Intent(context, AlbumDetailActivity.class);
        intent.putExtra(EXTRA_ALBUM_ID, albumId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        initViews();
        captureBaseUiMetrics();
        setupSystemBars();
        setupToolbar();
        setupRecyclerView();
        setupInsets();
        setupCollapsingMotion();
        setupFab();
        setupViewModel();
    }
    // --- Các biến cho Mini Player ---
    private View layoutMiniPlayer;
    private ImageView ivMiniCover;
    private TextView tvMiniTitle;
    private TextView tvMiniArtist;
    private ImageView btnMiniPlayPause;
    private void initViews() {
        appBarLayout = findViewById(R.id.appBarLayout);
        toolbarAlbumDetail = findViewById(R.id.toolbarAlbumDetail);
        ivAlbumBackdrop = findViewById(R.id.ivAlbumBackdrop);
        ivAlbumCover = findViewById(R.id.ivAlbumCover);
        layoutAlbumInfo = findViewById(R.id.layoutAlbumInfo);
        tvAlbumTypeBadge = findViewById(R.id.tvAlbumTypeBadge);
        tvAlbumTitleLarge = findViewById(R.id.tvAlbumTitleLarge);
        tvAlbumMeta = findViewById(R.id.tvAlbumMeta);
        tvAlbumStats = findViewById(R.id.tvAlbumStats);
        rvAlbumSongs = findViewById(R.id.rvAlbumSongs);
        fabPlayAlbum = findViewById(R.id.fabPlayAlbum);

        // Ánh xạ Mini Player
        layoutMiniPlayer = findViewById(R.id.layoutMiniPlayer);
        ivMiniCover = findViewById(R.id.ivMiniCover);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);

        // Bắt sự kiện click nút Play/Pause trên Mini Player
        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setOnClickListener(v -> {
                PlayerManager.getInstance(getApplicationContext()).togglePlayPause();
            });
        }
    }

    private void captureBaseUiMetrics() {
        // ... (Giữ nguyên code của bạn)
        toolbarBaseHeight = toolbarAlbumDetail.getLayoutParams().height;
        toolbarBasePaddingLeft = toolbarAlbumDetail.getPaddingLeft();
        toolbarBasePaddingTop = toolbarAlbumDetail.getPaddingTop();
        toolbarBasePaddingRight = toolbarAlbumDetail.getPaddingRight();
        toolbarBasePaddingBottom = toolbarAlbumDetail.getPaddingBottom();

        headerBasePaddingLeft = layoutAlbumInfo.getPaddingLeft();
        headerBasePaddingTop = layoutAlbumInfo.getPaddingTop();
        headerBasePaddingRight = layoutAlbumInfo.getPaddingRight();
        headerBasePaddingBottom = layoutAlbumInfo.getPaddingBottom();

        recyclerBasePaddingLeft = rvAlbumSongs.getPaddingLeft();
        recyclerBasePaddingTop = rvAlbumSongs.getPaddingTop();
        recyclerBasePaddingRight = rvAlbumSongs.getPaddingRight();
        recyclerBasePaddingBottom = rvAlbumSongs.getPaddingBottom();

        ViewGroup.MarginLayoutParams fabLayoutParams =
                (ViewGroup.MarginLayoutParams) fabPlayAlbum.getLayoutParams();
        fabBaseMarginStart = fabLayoutParams.getMarginStart();
        fabBaseMarginEnd = fabLayoutParams.getMarginEnd();
        fabBaseMarginBottom = fabLayoutParams.bottomMargin;
    }

    private void setupSystemBars() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }
    }

    private void setupToolbar() {
        toolbarAlbumDetail.setNavigationIcon(R.drawable.ic_arrow_back_24);
        toolbarAlbumDetail.setTitle("");
        toolbarAlbumDetail.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupRecyclerView() {
        songAdapter = new ArtistSongAdapter(
                ArtistSongAdapter.MODE_ALBUM_DETAIL,
                song -> {
                    int startIndex = findSongIndexById(song.getId());
                    PlayerManager.getInstance(getApplicationContext())
                            .playSongs(currentSongs, startIndex);
                }
        );

        rvAlbumSongs.setLayoutManager(new LinearLayoutManager(this));
        rvAlbumSongs.setAdapter(songAdapter);
        rvAlbumSongs.setHasFixedSize(true);
        rvAlbumSongs.setItemAnimator(null);
    }

    private int findSongIndexById(String songId) {
        if (songId == null) {
            return 0;
        }

        for (int i = 0; i < currentSongs.size(); i++) {
            Song item = currentSongs.get(i);
            if (songId.equals(item.getId())) {
                return i;
            }
        }
        return 0;
    }

    private void setupInsets() {
        // ... (Giữ nguyên code insets của bạn)
        ViewCompat.setOnApplyWindowInsetsListener(toolbarAlbumDetail, (v, insets) -> {
            int insetType = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
            Insets systemInsets = insets.getInsets(insetType);
            v.setPadding(toolbarBasePaddingLeft + systemInsets.left, toolbarBasePaddingTop + systemInsets.top,
                    toolbarBasePaddingRight + systemInsets.right, toolbarBasePaddingBottom);
            ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
            layoutParams.height = toolbarBaseHeight + systemInsets.top;
            v.setLayoutParams(layoutParams);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(layoutAlbumInfo, (v, insets) -> {
            int insetType = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
            Insets systemInsets = insets.getInsets(insetType);
            v.setPadding(headerBasePaddingLeft + systemInsets.left, headerBasePaddingTop,
                    headerBasePaddingRight + systemInsets.right, headerBasePaddingBottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(rvAlbumSongs, (v, insets) -> {
            int insetType = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
            Insets systemInsets = insets.getInsets(insetType);
            v.setPadding(recyclerBasePaddingLeft + systemInsets.left, recyclerBasePaddingTop,
                    recyclerBasePaddingRight + systemInsets.right, recyclerBasePaddingBottom + systemInsets.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(fabPlayAlbum, (v, insets) -> {
            int insetType = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
            Insets systemInsets = insets.getInsets(insetType);
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            layoutParams.setMarginStart(fabBaseMarginStart + systemInsets.left);
            layoutParams.setMarginEnd(fabBaseMarginEnd + systemInsets.right);
            layoutParams.bottomMargin = fabBaseMarginBottom;
            v.setLayoutParams(layoutParams);
            return insets;
        });

        ViewCompat.requestApplyInsets(findViewById(android.R.id.content));
    }

    private void setupCollapsingMotion() {
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            int totalScrollRange = appBarLayout.getTotalScrollRange();
            float collapseRatio = totalScrollRange == 0 ? 0f : Math.min(1f, Math.abs(verticalOffset) / (float) totalScrollRange);
            float headerAlpha = 1f - Math.min(1f, collapseRatio * 1.35f);
            layoutAlbumInfo.setAlpha(headerAlpha);
            boolean collapsed = Math.abs(verticalOffset) >= (totalScrollRange - toolbarAlbumDetail.getHeight());
            toolbarAlbumDetail.setTitle(collapsed ? currentAlbumTitle : "");
        });
    }

    private void setupFab() {
        fabPlayAlbum.setOnClickListener(v -> {
            if (currentSongs.isEmpty()) {
                Snackbar.make(v, "Album này chưa có bài hát để phát.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            PlayerManager playerManager = PlayerManager.getInstance(getApplicationContext());

            // Kiểm tra xem bài hát đang phát có nằm trong Album này không
            boolean isPlayingThisAlbum = false;
            for (Song song : currentSongs) {
                if (song.getId().equals(currentPlayingSongId)) {
                    isPlayingThisAlbum = true;
                    break;
                }
            }

            if (isPlayingThisAlbum) {
                playerManager.togglePlayPause(); // Dừng / Phát tiếp
            } else {
                playerManager.playSongs(currentSongs, 0); // Phát từ đầu
            }
        });
    }

    private void setupViewModel() {
        String albumId = getIntent().getStringExtra(EXTRA_ALBUM_ID);

        if (TextUtils.isEmpty(albumId)) {
            Toast.makeText(this, "Thiếu albumId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        AlbumRepository albumRepository = new AlbumRepository();
        AlbumViewModelFactory factory = new AlbumViewModelFactory(albumRepository, albumId);
        albumViewModel = new ViewModelProvider(this, factory).get(AlbumViewModel.class);

        albumViewModel.getAlbumLiveData().observe(this, this::bindAlbum);

        albumViewModel.getAlbumSongsLiveData().observe(this, songs -> {
            bindSongs(songs);
        });

        albumViewModel.getNotFoundLiveData().observe(this, notFound -> {
            if (Boolean.TRUE.equals(notFound)) {
                Toast.makeText(AlbumDetailActivity.this, "Không tìm thấy album", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // ---------------------------------------------------------
        // KẾT NỐI VỚI PLAYER MANAGER ĐỂ ĐỔI GIAO DIỆN & HIỆN MINI PLAYER
        // ---------------------------------------------------------
        PlayerManager playerManager = PlayerManager.getInstance(this);

        playerManager.getIsPlayingLiveData().observe(this, isPlaying -> {
            // Đổi icon ở nút Play bự (FAB)
            fabPlayAlbum.setImageResource(isPlaying ? R.drawable.ic_pause_24 : R.drawable.ic_play_arrow_24);

            // Đổi icon ở nút Play nhỏ (Mini Player)
            if (btnMiniPlayPause != null) {
                btnMiniPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause_24 : R.drawable.ic_play_arrow_24);
            }
        });

        playerManager.getCurrentSongIdLiveData().observe(this, songId -> {
            currentPlayingSongId = songId == null ? "" : songId;

            // 1. Đổi màu chữ bài hát trong danh sách
            if (songAdapter != null) {
                songAdapter.setCurrentPlayingId(currentPlayingSongId);
            }

            // 2. Hiển thị thông tin lên Mini Player
            if (!currentPlayingSongId.isEmpty()) {
                Song playingSong = MockDataStore.getSongById(currentPlayingSongId);
                if (playingSong != null && layoutMiniPlayer != null) {
                    layoutMiniPlayer.setVisibility(View.VISIBLE); // Hiện thanh Mini Player
                    tvMiniTitle.setText(playingSong.getTitle());
                    tvMiniArtist.setText(playingSong.getArtistName());
                    Glide.with(this)
                            .load(playingSong.getCoverUrl())
                            .placeholder(new ColorDrawable(Color.parseColor("#20312B")))
                            .into(ivMiniCover);
                }
            } else {
                // Nếu không có nhạc, giấu Mini Player đi
                if (layoutMiniPlayer != null) {
                    layoutMiniPlayer.setVisibility(View.GONE);
                }
            }
        });
    }

    private void bindAlbum(Album album) {
        if (album == null) {
            return;
        }

        currentAlbumTitle = safe(album.getTitle());

        tvAlbumTypeBadge.setText(album.isSingle() ? "SINGLE" : "ALBUM");
        tvAlbumTitleLarge.setText(currentAlbumTitle);
        tvAlbumMeta.setText(buildMetaText(album));
        tvAlbumStats.setText(buildStatsText(album));

        Glide.with(this)
                .load(resolveBackdropUrl(album))
                .thumbnail(0.15f)
                .centerCrop()
                .placeholder(new ColorDrawable(Color.parseColor("#20312B")))
                .error(new ColorDrawable(Color.parseColor("#20312B")))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivAlbumBackdrop);

        Glide.with(this)
                .load(resolveCoverUrl(album))
                .thumbnail(0.2f)
                .centerCrop()
                .placeholder(new ColorDrawable(Color.parseColor("#20312B")))
                .error(new ColorDrawable(Color.parseColor("#20312B")))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivAlbumCover);
    }

    private void bindSongs(List<Song> songs) {
        currentSongs.clear();

        if (songs != null) {
            currentSongs.addAll(songs);
        }

        songAdapter.submitList(new ArrayList<>(currentSongs));

        Album currentAlbum = albumViewModel.getAlbumLiveData().getValue();
        if (currentAlbum != null) {
            tvAlbumStats.setText(buildStatsText(currentAlbum));
        }
    }

    private String resolveBackdropUrl(Album album) {
        if (!TextUtils.isEmpty(album.getHeaderImageUrl())) {
            return album.getHeaderImageUrl();
        }
        return safe(album.getCoverUrl());
    }

    private String resolveCoverUrl(Album album) {
        if (!TextUtils.isEmpty(album.getCoverUrl())) {
            return album.getCoverUrl();
        }
        return safe(album.getHeaderImageUrl());
    }

    private String buildMetaText(Album album) {
        String artistName = safe(album.getArtistName());
        String year = extractYear(album.getReleaseDate());

        if (!artistName.isEmpty() && !year.isEmpty()) {
            return artistName + " • " + year;
        }

        if (!artistName.isEmpty()) {
            return artistName;
        }

        return year;
    }

    private String buildStatsText(Album album) {
        int totalTracks = currentSongs.isEmpty() ? album.getTotalTracks() : currentSongs.size();
        long totalDurationMs = currentSongs.isEmpty()
                ? album.getTotalDurationMs()
                : calculateCurrentSongsDurationMs();

        String trackText = totalTracks + " bài hát";
        String durationText = formatTotalDuration(totalDurationMs);

        if (!durationText.isEmpty()) {
            return trackText + " • " + durationText;
        }

        return trackText;
    }

    private long calculateCurrentSongsDurationMs() {
        long total = 0L;
        for (Song song : currentSongs) {
            total += Math.max(0L, song.getDurationMs());
        }
        return total;
    }

    private String formatTotalDuration(long durationMs) {
        if (durationMs <= 0L) {
            return "";
        }

        long totalSeconds = durationMs / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0L) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }

        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private String extractYear(String releaseDate) {
        if (TextUtils.isEmpty(releaseDate)) {
            return "";
        }

        if (releaseDate.length() >= 4) {
            return releaseDate.substring(0, 4);
        }

        return releaseDate;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}