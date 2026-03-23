package com.melodix.app.View.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.melodix.app.Data.Resource;
import com.melodix.app.Model.HomeBanner;
import com.melodix.app.Model.HomeDashboard;
import com.melodix.app.Model.PlayerUiState;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.View.auth.AuthActivity;
import com.melodix.app.View.home.adapter.GenreAdapter;
import com.melodix.app.View.home.adapter.HomeBannerPagerAdapter;
import com.melodix.app.View.home.adapter.NewReleaseAlbumAdapter;
import com.melodix.app.View.home.adapter.TrendingSongAdapter;
import com.melodix.app.View.player.PlayerActivity;
import com.melodix.app.ViewModel.AuthViewModel;
import com.melodix.app.ViewModel.HomeViewModel;
import com.melodix.app.ViewModel.PlayerViewModel;
import com.melodix.app.databinding.ActivityHomeBinding;

import java.util.Calendar;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private ActivityHomeBinding binding;
    private HomeViewModel homeViewModel;
    private AuthViewModel authViewModel;
    private PlayerViewModel playerViewModel;

    private HomeDashboard latestDashboard = new HomeDashboard();
    private String lastMiniPlayerCoverUrl = "";

    private HomeBannerPagerAdapter bannerAdapter;
    private TrendingSongAdapter trendingSongAdapter;
    private GenreAdapter genreAdapter;
    private NewReleaseAlbumAdapter albumAdapter;

    private final ViewPager2.OnPageChangeCallback bannerPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            updateBannerDots(position);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            binding = ActivityHomeBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
            authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
            playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);

            setupTopBar();
            setupAdapters();
            setupRecyclerViews();
            setupBannerViewPager();
            setupSwipeRefresh();
            setupBottomNavigation();
            setupMiniPlayerActions();
            observeViewModels();

            renderGreeting();
            renderMiniPlayer(PlayerUiState.idle());
        } catch (Exception e) {
            Log.e(TAG, "onCreate failed.", e);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                navigateToAuth();
                return;
            }

            renderGreeting();
            playerViewModel.connectPlayer();

            if (!homeViewModel.hasLoadedOnce()) {
                homeViewModel.loadHomeDashboard(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "onStart failed.", e);
            navigateToAuth();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (binding != null && binding.viewPagerBanners != null) {
                binding.viewPagerBanners.unregisterOnPageChangeCallback(bannerPageChangeCallback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to unregister banner callback.", e);
        }

        super.onDestroy();
    }

    private void setupTopBar() {
        binding.btnTopLogout.setOnClickListener(v -> {
            try {
                playerViewModel.stopPlayback();
            } catch (Exception e) {
                Log.e(TAG, "stopPlayback before logout failed.", e);
            }

            authViewModel.signOut();
        });
    }

    private void setupMiniPlayerActions() {
        binding.cardMiniPlayer.setOnClickListener(v -> openPlayerScreen());
        binding.btnMiniPlayerPlayPause.setOnClickListener(v -> playerViewModel.togglePlayPause());
    }

    private void setupAdapters() {
        bannerAdapter = new HomeBannerPagerAdapter(banner -> {
            if (banner == null) {
                showMessage(getString(R.string.home_placeholder_banner));
                return;
            }
            showMessage(banner.getDisplayTitle() + " - " + getString(R.string.home_placeholder_banner));
        });

        trendingSongAdapter = new TrendingSongAdapter((song, position) -> {
            try {
                List<Song> queue = latestDashboard.getTrendingSongs();
                if (queue == null || queue.isEmpty()) {
                    showMessage(getString(R.string.player_no_queue_message));
                    return;
                }

                playerViewModel.playSongs(queue, position, new PlayerViewModel.PlayerCommandCallback() {
                    @Override
                    public void onSuccess() {
                        openPlayerScreen();
                    }

                    @Override
                    public void onError(String message) {
                        showMessage(safeMessage(message));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Trending song click failed.", e);
                showMessage(getString(R.string.player_start_failed));
            }
        });

        genreAdapter = new GenreAdapter(genre -> {
            if (genre == null) {
                showMessage(getString(R.string.home_placeholder_genre));
                return;
            }
            showMessage(genre.getDisplayName() + " - " + getString(R.string.home_placeholder_genre));
        });

        albumAdapter = new NewReleaseAlbumAdapter(album -> {
            if (album == null) {
                showMessage(getString(R.string.home_placeholder_album));
                return;
            }
            showMessage(album.getDisplayTitle() + " - " + getString(R.string.home_placeholder_album));
        });
    }

    private void setupRecyclerViews() {
        binding.rvTrendingSongs.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        binding.rvTrendingSongs.setAdapter(trendingSongAdapter);
        binding.rvTrendingSongs.setNestedScrollingEnabled(false);
        binding.rvTrendingSongs.setHasFixedSize(true);

        binding.rvGenres.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        binding.rvGenres.setAdapter(genreAdapter);
        binding.rvGenres.setNestedScrollingEnabled(false);
        binding.rvGenres.setHasFixedSize(true);

        binding.rvNewAlbums.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        binding.rvNewAlbums.setAdapter(albumAdapter);
        binding.rvNewAlbums.setNestedScrollingEnabled(false);
        binding.rvNewAlbums.setHasFixedSize(true);
    }

    private void setupBannerViewPager() {
        binding.viewPagerBanners.setAdapter(bannerAdapter);
        binding.viewPagerBanners.setOffscreenPageLimit(3);
        binding.viewPagerBanners.registerOnPageChangeCallback(bannerPageChangeCallback);

        try {
            CompositePageTransformer transformer = new CompositePageTransformer();
            transformer.addTransformer(new MarginPageTransformer(dpToPx(12)));
            transformer.addTransformer((page, position) -> {
                float absPosition = Math.abs(position);
                float scale = 0.90f + (1f - Math.min(absPosition, 1f)) * 0.10f;
                float alpha = 0.75f + (1f - Math.min(absPosition, 1f)) * 0.25f;

                page.setScaleY(scale);
                page.setAlpha(alpha);
            });
            binding.viewPagerBanners.setPageTransformer(transformer);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set banner transformer.", e);
        }
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshHome.setColorSchemeResources(R.color.spotify_green);
        binding.swipeRefreshHome.setProgressBackgroundColorSchemeResource(R.color.spotify_dark_surface);
        binding.swipeRefreshHome.setOnRefreshListener(() -> homeViewModel.loadHomeDashboard(true));
    }

    private void setupBottomNavigation() {
        binding.bottomNav.setSelectedItemId(R.id.menu_home);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_home) {
                return true;
            }

            if (itemId == R.id.menu_search) {
                showMessage(getString(R.string.home_placeholder_search));
                return false;
            }

            if (itemId == R.id.menu_library) {
                showMessage(getString(R.string.home_placeholder_library));
                return false;
            }

            return false;
        });
    }

    private void observeViewModels() {
        homeViewModel.getHomeState().observe(this, resource -> {
            if (resource == null || resource.getStatus() == null) {
                return;
            }

            switch (resource.getStatus()) {
                case IDLE:
                    renderHomeLoading(false);
                    break;

                case LOADING:
                    renderHomeLoading(true);
                    break;

                case SUCCESS:
                    renderHomeLoading(false);
                    renderHomeDashboard(resource.getData());
                    break;

                case ERROR:
                    renderHomeLoading(false);
                    if (!hasAnyDisplayedContent()) {
                        binding.tvHomeEmptyState.setVisibility(View.VISIBLE);
                    }
                    showMessage(safeMessage(resource.getMessage()));
                    break;
            }
        });

        authViewModel.getSignOutState().observe(this, resource -> {
            if (resource == null || resource.getStatus() == null) {
                return;
            }

            switch (resource.getStatus()) {
                case IDLE:
                    break;

                case LOADING:
                    renderBlockingLoading(true);
                    break;

                case SUCCESS:
                    renderBlockingLoading(false);
                    navigateToAuth();
                    break;

                case ERROR:
                    renderBlockingLoading(false);
                    showMessage(safeMessage(resource.getMessage()));
                    break;
            }
        });

        playerViewModel.getPlayerState().observe(this, this::renderMiniPlayer);

        playerViewModel.getPlayerMessage().observe(this, message -> {
            if (!TextUtils.isEmpty(message)) {
                showMessage(message);
                playerViewModel.clearPlayerMessage();
            }
        });
    }

    private void renderHomeDashboard(HomeDashboard dashboard) {
        try {
            if (dashboard == null) {
                dashboard = new HomeDashboard();
            }

            latestDashboard = dashboard;

            bannerAdapter.submitList(dashboard.getBanners());
            trendingSongAdapter.submitList(dashboard.getTrendingSongs());
            genreAdapter.submitList(dashboard.getGenres());
            albumAdapter.submitList(dashboard.getNewAlbums());

            updateBannerSection(dashboard.getBanners());
            updateRecyclerSection(binding.rvTrendingSongs, binding.tvEmptyTrending,
                    dashboard.getTrendingSongs() != null && !dashboard.getTrendingSongs().isEmpty());
            updateRecyclerSection(binding.rvGenres, binding.tvEmptyGenres,
                    dashboard.getGenres() != null && !dashboard.getGenres().isEmpty());
            updateRecyclerSection(binding.rvNewAlbums, binding.tvEmptyAlbums,
                    dashboard.getNewAlbums() != null && !dashboard.getNewAlbums().isEmpty());

            binding.tvHomeEmptyState.setVisibility(dashboard.isEmpty() ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "renderHomeDashboard failed.", e);
            binding.tvHomeEmptyState.setVisibility(View.VISIBLE);
        }
    }

    private void renderMiniPlayer(PlayerUiState state) {
        try {
            if (state == null || !state.isVisible()) {
                binding.cardMiniPlayer.setVisibility(View.GONE);
                lastMiniPlayerCoverUrl = "";
                return;
            }

            binding.cardMiniPlayer.setVisibility(View.VISIBLE);
            binding.tvMiniPlayerTitle.setText(state.getDisplayTitle());
            binding.tvMiniPlayerSubtitle.setText(state.getDisplaySubtitle());

            binding.btnMiniPlayerPlayPause.setImageResource(
                    state.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play
            );

            String coverUrl = state.getCoverUrl();
            if (!TextUtils.equals(lastMiniPlayerCoverUrl, coverUrl)) {
                lastMiniPlayerCoverUrl = coverUrl == null ? "" : coverUrl;

                try {
                    Glide.with(this)
                            .load(coverUrl)
                            .placeholder(R.drawable.bg_image_placeholder)
                            .error(R.drawable.bg_image_placeholder)
                            .centerCrop()
                            .into(binding.ivMiniPlayerCover);
                } catch (Exception e) {
                    Log.e(TAG, "Mini player artwork load failed.", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "renderMiniPlayer failed.", e);
            binding.cardMiniPlayer.setVisibility(View.GONE);
        }
    }

    private void updateBannerSection(List<HomeBanner> banners) {
        boolean hasBanners = banners != null && !banners.isEmpty();

        binding.viewPagerBanners.setVisibility(hasBanners ? View.VISIBLE : View.GONE);
        binding.tvEmptyBanners.setVisibility(hasBanners ? View.GONE : View.VISIBLE);

        if (hasBanners) {
            try {
                binding.viewPagerBanners.setCurrentItem(0, false);
            } catch (Exception e) {
                Log.e(TAG, "Failed to reset banner pager position.", e);
            }
            setupBannerDots(banners.size(), 0);
        } else {
            binding.layoutBannerDots.removeAllViews();
            binding.layoutBannerDots.setVisibility(View.GONE);
        }
    }

    private void updateRecyclerSection(RecyclerView recyclerView, View emptyView, boolean hasData) {
        recyclerView.setVisibility(hasData ? View.VISIBLE : View.GONE);
        emptyView.setVisibility(hasData ? View.GONE : View.VISIBLE);
    }

    private void renderHomeLoading(boolean loading) {
        try {
            boolean hasContent = hasAnyDisplayedContent();

            binding.layoutLoadingOverlayHome.setVisibility(loading && !hasContent ? View.VISIBLE : View.GONE);
            binding.swipeRefreshHome.setRefreshing(loading && hasContent);
            binding.btnTopLogout.setEnabled(!loading);
        } catch (Exception e) {
            Log.e(TAG, "renderHomeLoading failed.", e);
        }
    }

    private void renderBlockingLoading(boolean loading) {
        try {
            binding.layoutLoadingOverlayHome.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.swipeRefreshHome.setRefreshing(false);
            binding.btnTopLogout.setEnabled(!loading);
        } catch (Exception e) {
            Log.e(TAG, "renderBlockingLoading failed.", e);
        }
    }

    private boolean hasAnyDisplayedContent() {
        return (bannerAdapter != null && bannerAdapter.getItemCount() > 0)
                || (trendingSongAdapter != null && trendingSongAdapter.getItemCount() > 0)
                || (genreAdapter != null && genreAdapter.getItemCount() > 0)
                || (albumAdapter != null && albumAdapter.getItemCount() > 0);
    }

    private void renderGreeting() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String friendlyName = extractFriendlyName(user);

            binding.tvGreeting.setText(buildTimeGreeting() + ", " + friendlyName);
            binding.tvGreetingSubtitle.setText(getString(R.string.home_discovery_subtitle));
        } catch (Exception e) {
            Log.e(TAG, "renderGreeting failed.", e);
            binding.tvGreeting.setText(getString(R.string.home_greeting_fallback));
            binding.tvGreetingSubtitle.setText(getString(R.string.home_discovery_subtitle));
        }
    }

    private String extractFriendlyName(FirebaseUser user) {
        try {
            if (user == null) {
                return getString(R.string.home_name_placeholder);
            }

            String rawName = user.getDisplayName();

            if (TextUtils.isEmpty(rawName)) {
                String email = user.getEmail();
                if (!TextUtils.isEmpty(email) && email.contains("@")) {
                    rawName = email.substring(0, email.indexOf("@"));
                }
            }

            if (TextUtils.isEmpty(rawName)) {
                return getString(R.string.home_name_placeholder);
            }

            String trimmed = rawName.trim();
            String[] parts = trimmed.split("\\s+");

            if (parts.length > 0) {
                return parts[parts.length - 1];
            }

            return trimmed;
        } catch (Exception e) {
            Log.e(TAG, "extractFriendlyName failed.", e);
            return getString(R.string.home_name_placeholder);
        }
    }

    private String buildTimeGreeting() {
        try {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

            if (hour >= 5 && hour < 12) {
                return getString(R.string.home_greeting_morning);
            }

            if (hour >= 12 && hour < 18) {
                return getString(R.string.home_greeting_afternoon);
            }

            return getString(R.string.home_greeting_evening);
        } catch (Exception e) {
            Log.e(TAG, "buildTimeGreeting failed.", e);
            return getString(R.string.home_greeting_fallback);
        }
    }

    private void setupBannerDots(int count, int selectedPosition) {
        try {
            binding.layoutBannerDots.removeAllViews();

            if (count <= 1) {
                binding.layoutBannerDots.setVisibility(View.GONE);
                return;
            }

            binding.layoutBannerDots.setVisibility(View.VISIBLE);

            for (int i = 0; i < count; i++) {
                View dotView = new View(this);
                dotView.setLayoutParams(createDotLayoutParams(i == selectedPosition));
                dotView.setBackgroundResource(i == selectedPosition
                        ? R.drawable.bg_banner_dot_selected
                        : R.drawable.bg_banner_dot_unselected);
                binding.layoutBannerDots.addView(dotView);
            }
        } catch (Exception e) {
            Log.e(TAG, "setupBannerDots failed.", e);
        }
    }

    private void updateBannerDots(int selectedPosition) {
        try {
            int childCount = binding.layoutBannerDots.getChildCount();
            if (childCount == 0) {
                return;
            }

            for (int i = 0; i < childCount; i++) {
                View dotView = binding.layoutBannerDots.getChildAt(i);
                if (dotView == null) {
                    continue;
                }

                dotView.setLayoutParams(createDotLayoutParams(i == selectedPosition));
                dotView.setBackgroundResource(i == selectedPosition
                        ? R.drawable.bg_banner_dot_selected
                        : R.drawable.bg_banner_dot_unselected);
            }
        } catch (Exception e) {
            Log.e(TAG, "updateBannerDots failed.", e);
        }
    }

    private LinearLayout.LayoutParams createDotLayoutParams(boolean selected) {
        int width = selected ? dpToPx(18) : dpToPx(8);
        int height = dpToPx(8);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        int horizontalMargin = dpToPx(4);
        params.setMargins(horizontalMargin, 0, horizontalMargin, 0);
        return params;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void openPlayerScreen() {
        try {
            Intent intent = new Intent(this, PlayerActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "openPlayerScreen failed.", e);
        }
    }

    private void navigateToAuth() {
        try {
            Intent intent = new Intent(this, AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "navigateToAuth failed.", e);
        }
    }

    private void showMessage(String message) {
        try {
            Snackbar snackbar = Snackbar.make(binding.getRoot(), safeMessage(message), Snackbar.LENGTH_LONG);

            View anchor = binding.cardMiniPlayer.getVisibility() == View.VISIBLE
                    ? binding.cardMiniPlayer
                    : binding.bottomNav;

            snackbar.setAnchorView(anchor);
            snackbar.show();
        } catch (Exception e) {
            Log.e(TAG, "showMessage failed.", e);
        }
    }

    private String safeMessage(String message) {
        return TextUtils.isEmpty(message) ? getString(R.string.home_unknown_error) : message;
    }
}