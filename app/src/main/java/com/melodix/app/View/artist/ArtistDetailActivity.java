package com.melodix.app.View.artist;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.melodix.app.Data.MockDataStore;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.ArtistRepository;
import com.melodix.app.View.album.AlbumDetailActivity;
import com.melodix.app.View.artist.adapter.ArtistAlbumAdapter;
import com.melodix.app.View.artist.adapter.ArtistSongAdapter;
import com.melodix.app.View.artist.adapter.RelatedArtistAdapter;
import com.melodix.app.ViewModel.ArtistViewModel;
import com.melodix.app.ViewModel.ArtistViewModelFactory;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArtistDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ARTIST_ID = "extra_artist_id";

    private AppBarLayout appBarLayout;
    private MaterialToolbar toolbarArtistDetail;
    private ImageView ivArtistCover;
    private LinearLayout layoutArtistInfo;
    private TextView tvArtistBadge;
    private TextView tvArtistNameLarge;
    private TextView tvArtistGenre;
    private TextView tvArtistStats;
    private TextView tvArtistBio;
    private TextView tvAlbumHeader;
    private TextView tvRelatedHeader;

    private RecyclerView rvPopularSongs;
    private RecyclerView rvArtistAlbums;
    private RecyclerView rvRelatedArtists;
    private FloatingActionButton fabPlayArtist;

    private ArtistViewModel artistViewModel;
    private ArtistSongAdapter artistSongAdapter;
    private ArtistAlbumAdapter artistAlbumAdapter;
    private RelatedArtistAdapter relatedArtistAdapter;

    private final List<Song> currentSongs = new ArrayList<>();
    private String currentArtistName = "";
    private String currentPlayingSongId = ""; // Thêm biến lưu ID bài đang phát

    public static Intent newIntent(Context context, String artistId) {
        Intent intent = new Intent(context, ArtistDetailActivity.class);
        intent.putExtra(EXTRA_ARTIST_ID, artistId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_detail);

        initViews();
        setupSystemBars();
        setupToolbar();
        setupRecyclerViews();
        setupCollapsingMotion();
        setupFab();
        setupViewModel();
    }

    private View layoutMiniPlayer;
    private ImageView ivMiniCover;
    private TextView tvMiniTitle;
    private TextView tvMiniArtist;
    private ImageView btnMiniPlayPause;

    private void initViews() {
        appBarLayout = findViewById(R.id.appBarLayout);
        toolbarArtistDetail = findViewById(R.id.toolbarArtistDetail);
        ivArtistCover = findViewById(R.id.ivArtistCover);
        layoutArtistInfo = findViewById(R.id.layoutArtistInfo);
        tvArtistBadge = findViewById(R.id.tvArtistBadge);
        tvArtistNameLarge = findViewById(R.id.tvArtistNameLarge);
        tvArtistGenre = findViewById(R.id.tvArtistGenre);
        tvArtistStats = findViewById(R.id.tvArtistStats);
        tvArtistBio = findViewById(R.id.tvArtistBio);
        tvAlbumHeader = findViewById(R.id.tvAlbumHeader);
        tvRelatedHeader = findViewById(R.id.tvRelatedHeader);
        rvPopularSongs = findViewById(R.id.rvPopularSongs);
        rvArtistAlbums = findViewById(R.id.rvArtistAlbums);
        rvRelatedArtists = findViewById(R.id.rvRelatedArtists);
        fabPlayArtist = findViewById(R.id.fabPlayArtist);

        // Ánh xạ Mini Player
        layoutMiniPlayer = findViewById(R.id.layoutMiniPlayer);
        ivMiniCover = findViewById(R.id.ivMiniCover);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);



    }

    private void setupSystemBars() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }
    }

    private void setupToolbar() {
        toolbarArtistDetail.setNavigationIcon(R.drawable.ic_arrow_back_24);
        toolbarArtistDetail.setTitle("");
        toolbarArtistDetail.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupRecyclerViews() {
        // Bài hát
        artistSongAdapter = new ArtistSongAdapter(song -> {
            int startIndex = findSongIndexById(song.getId());
        });
        rvPopularSongs.setLayoutManager(new LinearLayoutManager(this));
        rvPopularSongs.setAdapter(artistSongAdapter);
        rvPopularSongs.setHasFixedSize(true);
        rvPopularSongs.setItemAnimator(null);

        // Album (Cuộn ngang)
        artistAlbumAdapter = new ArtistAlbumAdapter(album -> {
            startActivity(AlbumDetailActivity.newIntent(this, album.getId()));
        });
        rvArtistAlbums.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvArtistAlbums.setAdapter(artistAlbumAdapter);

        // Nghệ sĩ tương tự (Cuộn ngang)
        relatedArtistAdapter = new RelatedArtistAdapter(artist -> {
            startActivity(ArtistDetailActivity.newIntent(this, artist.getId()));
            finish(); // Đóng trang hiện tại để không bị đè quá nhiều Activity
        });
        rvRelatedArtists.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRelatedArtists.setAdapter(relatedArtistAdapter);
    }

    private int findSongIndexById(String songId) {
        if (songId == null) return 0;
        for (int i = 0; i < currentSongs.size(); i++) {
            if (songId.equals(currentSongs.get(i).getId())) return i;
        }
        return 0;
    }

    private void setupCollapsingMotion() {
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            int totalScrollRange = appBarLayout.getTotalScrollRange();
            float collapseRatio = totalScrollRange == 0 ? 0f : Math.min(1f, Math.abs(verticalOffset) / (float) totalScrollRange);
            float headerAlpha = 1f - Math.min(1f, collapseRatio * 1.35f);
            layoutArtistInfo.setAlpha(headerAlpha);
            boolean collapsed = Math.abs(verticalOffset) >= (totalScrollRange - toolbarArtistDetail.getHeight());
            toolbarArtistDetail.setTitle(collapsed ? currentArtistName : "");
        });
    }

    private void setupFab() {
        fabPlayArtist.setOnClickListener(v -> {
            if (currentSongs.isEmpty()) {
                Snackbar.make(v, "Nghệ sĩ này chưa có bài hát để phát.", Snackbar.LENGTH_SHORT).show();
                return;
            }

        });
    }

    private void setupViewModel() {
        String artistId = getIntent().getStringExtra(EXTRA_ARTIST_ID);
        if (TextUtils.isEmpty(artistId)) {
            Toast.makeText(this, "Thiếu artistId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ArtistRepository artistRepository = new ArtistRepository();
        ArtistViewModelFactory factory = new ArtistViewModelFactory(artistRepository, artistId);
        artistViewModel = new ViewModelProvider(this, factory).get(ArtistViewModel.class);

        artistViewModel.getArtistLiveData().observe(this, this::bindArtist);

        artistViewModel.getPopularSongsLiveData().observe(this, songs -> {
            currentSongs.clear();
            if (songs != null) currentSongs.addAll(songs);
            artistSongAdapter.submitList(new ArrayList<>(currentSongs));
        });

        artistViewModel.getAlbumsLiveData().observe(this, albums -> {
            artistAlbumAdapter.submitList(albums);
            tvAlbumHeader.setVisibility(albums != null && !albums.isEmpty() ? View.VISIBLE : View.GONE);
            rvArtistAlbums.setVisibility(albums != null && !albums.isEmpty() ? View.VISIBLE : View.GONE);
        });

        artistViewModel.getRelatedArtistsLiveData().observe(this, artists -> {
            relatedArtistAdapter.submitList(artists);
            tvRelatedHeader.setVisibility(artists != null && !artists.isEmpty() ? View.VISIBLE : View.GONE);
            rvRelatedArtists.setVisibility(artists != null && !artists.isEmpty() ? View.VISIBLE : View.GONE);
        });

        artistViewModel.getNotFoundLiveData().observe(this, notFound -> {
            if (Boolean.TRUE.equals(notFound)) {
                Toast.makeText(this, "Không tìm thấy nghệ sĩ", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // ---------------------------------------------------------
        // KẾT NỐI VỚI PLAYER MANAGER ĐỂ ĐỔI GIAO DIỆN & HIỆN MINI PLAYER
        // ---------------------------------------------------------

    }
    private void bindArtist(Artist artist) {
        if (artist == null) return;
        currentArtistName = safe(artist.getName());
        tvArtistNameLarge.setText(currentArtistName);
        tvArtistGenre.setText(!TextUtils.isEmpty(artist.getGenre()) ? artist.getGenre() : "Nghệ sĩ");
        tvArtistBadge.setVisibility(artist.isVerified() ? View.VISIBLE : View.GONE);

        // Đổ dữ liệu Tiểu sử
        tvArtistBio.setText(!TextUtils.isEmpty(artist.getBio()) ? artist.getBio() : "Chưa có thông tin tiểu sử.");

        // Format số liệu
        int totalSongs = currentSongs.size() > 0 ? currentSongs.size() : artist.getTotalTracks();
        String stats = formatCompactNumber(artist.getMonthlyListeners()) + " người nghe • "
                + formatCompactNumber(artist.getFollowers()) + " theo dõi • "
                + totalSongs + " bài hát";
        tvArtistStats.setText(stats);

        Glide.with(this)
                .load(!TextUtils.isEmpty(artist.getHeaderImageUrl()) ? artist.getHeaderImageUrl() : safe(artist.getImageUrl()))
                .thumbnail(0.15f).centerCrop()
                .placeholder(new ColorDrawable(Color.parseColor("#20312B")))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivArtistCover);
    }

    private String formatCompactNumber(long value) {
        String result;
        if (value >= 1_000_000_000L) result = String.format(Locale.getDefault(), "%.1fB", value / 1_000_000_000f);
        else if (value >= 1_000_000L) result = String.format(Locale.getDefault(), "%.1fM", value / 1_000_000f);
        else if (value >= 1_000L) result = String.format(Locale.getDefault(), "%.1fK", value / 1_000f);
        else result = String.valueOf(value);
        return result.replace(".0B", "B").replace(".0M", "M").replace(".0K", "K");
    }

    private String safe(String value) { return value == null ? "" : value; }
}