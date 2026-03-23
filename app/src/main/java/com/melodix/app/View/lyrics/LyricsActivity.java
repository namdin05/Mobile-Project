package com.melodix.app.View.lyrics;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.melodix.app.Model.PlayerUiState;
import com.melodix.app.Model.SongLyrics;
import com.melodix.app.R;
import com.melodix.app.View.lyrics.adapter.LyricLineAdapter;
import com.melodix.app.ViewModel.LyricsViewModel;
import com.melodix.app.ViewModel.PlayerViewModel;
import com.melodix.app.databinding.ActivityLyricsBinding;

import java.util.ArrayList;

public class LyricsActivity extends AppCompatActivity {

    private static final String TAG = "LyricsActivity";

    private ActivityLyricsBinding binding;
    private PlayerViewModel playerViewModel;
    private LyricsViewModel lyricsViewModel;
    private LyricLineAdapter lyricLineAdapter;

    private PlayerUiState latestPlayerState = PlayerUiState.idle();
    private SongLyrics currentLyrics = new SongLyrics();
    private String currentSongId = "";
    private int currentActiveIndex = -2;
    private boolean isLyricsLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            binding = ActivityLyricsBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
            lyricsViewModel = new ViewModelProvider(this).get(LyricsViewModel.class);

            setupActions();
            setupRecyclerView();
            observeViewModels();
            renderLyricsHeader(PlayerUiState.idle());
            renderLyricsBody();
        } catch (Exception e) {
            Log.e(TAG, "onCreate failed.", e);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            playerViewModel.connectPlayer();
        } catch (Exception e) {
            Log.e(TAG, "connectPlayer failed.", e);
        }
    }

    private void setupActions() {
        binding.btnBackLyrics.setOnClickListener(v -> finish());

        binding.swipeRefreshLyrics.setColorSchemeResources(R.color.spotify_green);
        binding.swipeRefreshLyrics.setProgressBackgroundColorSchemeResource(R.color.spotify_dark_surface);
        binding.swipeRefreshLyrics.setOnRefreshListener(() -> {
            if (!TextUtils.isEmpty(currentSongId)) {
                lyricsViewModel.loadLyrics(currentSongId, true);
            } else {
                binding.swipeRefreshLyrics.setRefreshing(false);
            }
        });
    }

    private void setupRecyclerView() {
        lyricLineAdapter = new LyricLineAdapter();
        binding.rvLyrics.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLyrics.setAdapter(lyricLineAdapter);
        binding.rvLyrics.setHasFixedSize(false);
    }

    private void observeViewModels() {
        playerViewModel.getPlayerState().observe(this, state -> {
            latestPlayerState = state == null ? PlayerUiState.idle() : state;
            renderLyricsHeader(latestPlayerState);
            handleSongChange(latestPlayerState);
            updateActiveLyricHighlight();
        });

        playerViewModel.getPlayerMessage().observe(this, message -> {
            if (!TextUtils.isEmpty(message)) {
                showMessage(message);
                playerViewModel.clearPlayerMessage();
            }
        });

        lyricsViewModel.getLyricsState().observe(this, resource -> {
            if (resource == null || resource.getStatus() == null) {
                return;
            }

            switch (resource.getStatus()) {
                case IDLE:
                    isLyricsLoading = false;
                    renderLyricsLoading();
                    break;

                case LOADING:
                    isLyricsLoading = true;
                    renderLyricsLoading();
                    renderLyricsBody();
                    break;

                case SUCCESS:
                    isLyricsLoading = false;
                    currentLyrics = resource.getData() == null ? new SongLyrics() : resource.getData();
                    currentActiveIndex = -2;
                    renderLyricsLoading();
                    renderLyricsBody();
                    break;

                case ERROR:
                    isLyricsLoading = false;
                    currentLyrics = new SongLyrics();
                    currentActiveIndex = -2;
                    renderLyricsLoading();
                    renderLyricsBody();
                    showMessage(safeMessage(resource.getMessage()));
                    break;
            }
        });
    }

    private void handleSongChange(PlayerUiState state) {
        try {
            if (state == null || !state.isVisible() || TextUtils.isEmpty(state.getMediaId())) {
                if (!TextUtils.isEmpty(currentSongId)) {
                    currentSongId = "";
                    currentLyrics = new SongLyrics();
                    currentActiveIndex = -2;
                    lyricsViewModel.clearLyrics();
                    renderLyricsBody();
                }
                return;
            }

            String newSongId = state.getMediaId();
            if (!TextUtils.equals(newSongId, currentSongId)) {
                currentSongId = newSongId;
                currentLyrics = new SongLyrics();
                currentActiveIndex = -2;
                renderLyricsBody();
                lyricsViewModel.loadLyrics(newSongId, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "handleSongChange failed.", e);
        }
    }

    private void renderLyricsHeader(PlayerUiState state) {
        try {
            if (state == null || !state.isVisible()) {
                binding.tvLyricsTrackTitle.setText(getString(R.string.lyrics_track_title_fallback));
                binding.tvLyricsTrackSubtitle.setText(getString(R.string.lyrics_track_subtitle_fallback));
                return;
            }

            binding.tvLyricsTrackTitle.setText(state.getDisplayTitle());
            binding.tvLyricsTrackSubtitle.setText(state.getDisplaySubtitle());
        } catch (Exception e) {
            Log.e(TAG, "renderLyricsHeader failed.", e);
        }
    }

    private void renderLyricsBody() {
        try {
            boolean hasTrack = latestPlayerState != null && latestPlayerState.isVisible();

            if (!hasTrack) {
                lyricLineAdapter.submitLyrics(new ArrayList<>(), false);
                binding.tvLyricsMode.setText(getString(R.string.lyrics_mode_no_data));
                binding.tvLyricsEmptyState.setText(getString(R.string.lyrics_empty_no_track));
                binding.tvLyricsEmptyState.setVisibility(View.VISIBLE);
                currentActiveIndex = -1;
                return;
            }

            if (currentLyrics != null && currentLyrics.hasAnyLyrics()) {
                lyricLineAdapter.submitLyrics(currentLyrics.getDisplayLines(), currentLyrics.hasSyncedLyrics());
                binding.tvLyricsMode.setText(currentLyrics.hasSyncedLyrics()
                        ? getString(R.string.lyrics_mode_synced_full)
                        : getString(R.string.lyrics_mode_static_full));
                binding.tvLyricsEmptyState.setVisibility(View.GONE);
                updateActiveLyricHighlight();
            } else {
                lyricLineAdapter.submitLyrics(new ArrayList<>(), false);
                binding.tvLyricsMode.setText(isLyricsLoading
                        ? getString(R.string.lyrics_loading_label)
                        : getString(R.string.lyrics_mode_no_data));
                binding.tvLyricsEmptyState.setText(isLyricsLoading
                        ? getString(R.string.lyrics_loading_message)
                        : getString(R.string.lyrics_empty_unavailable));
                binding.tvLyricsEmptyState.setVisibility(View.VISIBLE);
                currentActiveIndex = -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "renderLyricsBody failed.", e);
            binding.tvLyricsEmptyState.setVisibility(View.VISIBLE);
        }
    }

    private void updateActiveLyricHighlight() {
        try {
            if (currentLyrics == null || !currentLyrics.hasSyncedLyrics()) {
                currentActiveIndex = -1;
                lyricLineAdapter.setActiveIndex(-1);
                return;
            }

            int activeIndex = currentLyrics.getActiveSyncedLineIndex(latestPlayerState.getCurrentPosition());
            lyricLineAdapter.setActiveIndex(activeIndex);

            if (activeIndex != currentActiveIndex) {
                currentActiveIndex = activeIndex;

                if (activeIndex >= 0) {
                    final int targetPosition = Math.max(activeIndex - 1, 0);
                    binding.rvLyrics.post(() -> {
                        try {
                            binding.rvLyrics.smoothScrollToPosition(targetPosition);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to auto scroll lyrics.", e);
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "updateActiveLyricHighlight failed.", e);
        }
    }

    private void renderLyricsLoading() {
        try {
            boolean hasExistingLyrics = currentLyrics != null && currentLyrics.hasAnyLyrics();

            binding.layoutLoadingOverlayLyrics.setVisibility(isLyricsLoading && !hasExistingLyrics ? View.VISIBLE : View.GONE);
            binding.swipeRefreshLyrics.setRefreshing(isLyricsLoading && hasExistingLyrics);
        } catch (Exception e) {
            Log.e(TAG, "renderLyricsLoading failed.", e);
        }
    }

    private void showMessage(String message) {
        try {
            Snackbar.make(binding.getRoot(), safeMessage(message), Snackbar.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "showMessage failed.", e);
        }
    }

    private String safeMessage(String message) {
        return TextUtils.isEmpty(message) ? getString(R.string.home_unknown_error) : message;
    }
}