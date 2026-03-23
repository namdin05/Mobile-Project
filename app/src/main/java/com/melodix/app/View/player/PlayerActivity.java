package com.melodix.app.View.player;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.melodix.app.Data.Resource;
import com.melodix.app.Model.PlayerUiState;
import com.melodix.app.Model.SongLyrics;
import com.melodix.app.R;
import com.melodix.app.Utils.AppConstants;
import com.melodix.app.Utils.PlaybackTimeUtils;
import com.melodix.app.View.lyrics.LyricsActivity;
import com.melodix.app.ViewModel.LyricsViewModel;
import com.melodix.app.ViewModel.PlayerViewModel;
import com.melodix.app.databinding.ActivityPlayerBinding;

import java.util.Locale;

public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "PlayerActivity";

    private ActivityPlayerBinding binding;
    private PlayerViewModel playerViewModel;
    private LyricsViewModel lyricsViewModel;

    private boolean isUserSeeking = false;
    private String lastArtworkUrl = "";
    private float currentPlaybackSpeed = AppConstants.PLAYER_DEFAULT_SPEED;

    private PlayerUiState latestPlayerState = PlayerUiState.idle();
    private SongLyrics currentLyrics = new SongLyrics();
    private String currentLyricsSongId = "";
    private boolean isLyricsLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            binding = ActivityPlayerBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
            lyricsViewModel = new ViewModelProvider(this).get(LyricsViewModel.class);

            setupActions();
            observeViewModel();
            renderPlayerState(PlayerUiState.idle());
            renderLyricsPreview();
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
            Log.e(TAG, "connectPlayer onStart failed.", e);
        }
    }

    private void setupActions() {
        binding.btnClosePlayer.setOnClickListener(v -> finish());
        binding.btnPlayerPlayPause.setOnClickListener(v -> playerViewModel.togglePlayPause());
        binding.btnPlayerNext.setOnClickListener(v -> playerViewModel.skipToNext());
        binding.btnPlayerPrevious.setOnClickListener(v -> playerViewModel.skipToPrevious());
        binding.btnPlayerRewind.setOnClickListener(v -> playerViewModel.seekBackward());
        binding.btnPlayerForward.setOnClickListener(v -> playerViewModel.seekForward());
        binding.btnPlaybackSpeed.setOnClickListener(v -> showPlaybackSpeedDialog());

        binding.cardLyricsPreview.setOnClickListener(v -> openLyricsScreen());
        binding.btnOpenLyrics.setOnClickListener(v -> openLyricsScreen());

        binding.seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                try {
                    if (fromUser) {
                        binding.tvCurrentTime.setText(PlaybackTimeUtils.formatDuration(progress));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onProgressChanged failed.", e);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try {
                    isUserSeeking = false;
                    playerViewModel.seekTo(seekBar.getProgress());
                } catch (Exception e) {
                    Log.e(TAG, "onStopTrackingTouch failed.", e);
                }
            }
        });
    }

    private void observeViewModel() {
        playerViewModel.getPlayerState().observe(this, state -> {
            latestPlayerState = state == null ? PlayerUiState.idle() : state;
            renderPlayerState(latestPlayerState);
            handleLyricsSongChange(latestPlayerState);
            renderLyricsPreview();
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
                    renderLyricsPreview();
                    break;

                case LOADING:
                    isLyricsLoading = true;
                    renderLyricsPreview();
                    break;

                case SUCCESS:
                    isLyricsLoading = false;
                    currentLyrics = resource.getData() == null ? new SongLyrics() : resource.getData();
                    renderLyricsPreview();
                    break;

                case ERROR:
                    isLyricsLoading = false;
                    currentLyrics = new SongLyrics();
                    renderLyricsPreview();
                    showMessage(safeMessage(resource.getMessage()));
                    break;
            }
        });
    }

    private void handleLyricsSongChange(PlayerUiState state) {
        try {
            if (state == null || !state.isVisible() || TextUtils.isEmpty(state.getMediaId())) {
                if (!TextUtils.isEmpty(currentLyricsSongId)) {
                    currentLyricsSongId = "";
                    currentLyrics = new SongLyrics();
                    lyricsViewModel.clearLyrics();
                }
                return;
            }

            String newSongId = state.getMediaId();
            if (!TextUtils.equals(newSongId, currentLyricsSongId)) {
                currentLyricsSongId = newSongId;
                currentLyrics = new SongLyrics();
                lyricsViewModel.loadLyrics(newSongId, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "handleLyricsSongChange failed.", e);
        }
    }

    private void renderPlayerState(PlayerUiState state) {
        try {
            if (state == null) {
                state = PlayerUiState.idle();
            }

            boolean hasTrack = state.isVisible();
            currentPlaybackSpeed = state.getPlaybackSpeed();

            binding.tvPlayerEmptyState.setVisibility(hasTrack ? View.GONE : View.VISIBLE);
            binding.progressPlayerBuffering.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

            binding.tvPlayerTitle.setText(hasTrack
                    ? state.getDisplayTitle()
                    : getString(R.string.player_empty_title));

            binding.tvPlayerSubtitle.setText(hasTrack
                    ? state.getDisplaySubtitle()
                    : getString(R.string.player_empty_subtitle));

            if (hasTrack && state.getQueueSize() > 0) {
                binding.tvPlayerQueueInfo.setText(
                        getString(
                                R.string.player_queue_info_format,
                                state.getCurrentIndex() + 1,
                                state.getQueueSize()
                        )
                );
            } else {
                binding.tvPlayerQueueInfo.setText(getString(R.string.player_queue_idle));
            }

            binding.btnPlaybackSpeed.setText(formatSpeedLabel(currentPlaybackSpeed));
            binding.btnPlaybackSpeed.setEnabled(hasTrack);
            binding.btnPlaybackSpeed.setAlpha(hasTrack ? 1f : 0.5f);

            if (!isUserSeeking) {
                int max = safeSeekBarValue(state.getDuration());
                int progress = safeSeekBarValue(state.getCurrentPosition());

                binding.seekBarProgress.setMax(Math.max(max, 1));
                binding.seekBarProgress.setProgress(Math.min(progress, Math.max(max, 1)));

                binding.tvCurrentTime.setText(PlaybackTimeUtils.formatDuration(state.getCurrentPosition()));
                binding.tvDuration.setText(PlaybackTimeUtils.formatDuration(state.getDuration()));
            }

            binding.btnPlayerPlayPause.setEnabled(hasTrack);
            binding.btnPlayerPlayPause.setImageResource(
                    state.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play
            );
            binding.btnPlayerPlayPause.setAlpha(hasTrack ? 1f : 0.5f);

            boolean canPreviousTrack = hasTrack && state.isHasPrevious();
            binding.btnPlayerPrevious.setEnabled(canPreviousTrack);
            binding.btnPlayerPrevious.setAlpha(canPreviousTrack ? 1f : 0.4f);

            boolean canNextTrack = hasTrack && state.isHasNext();
            binding.btnPlayerNext.setEnabled(canNextTrack);
            binding.btnPlayerNext.setAlpha(canNextTrack ? 1f : 0.4f);

            boolean canSeekBackward = hasTrack && state.getCurrentPosition() > 0L;
            binding.btnPlayerRewind.setEnabled(canSeekBackward);
            binding.btnPlayerRewind.setAlpha(canSeekBackward ? 1f : 0.4f);

            boolean canSeekForward = hasTrack
                    && state.getDuration() > 0L
                    && state.getCurrentPosition() < state.getDuration();
            binding.btnPlayerForward.setEnabled(canSeekForward);
            binding.btnPlayerForward.setAlpha(canSeekForward ? 1f : 0.4f);

            binding.seekBarProgress.setEnabled(hasTrack);

            String coverUrl = state.getCoverUrl();
            if (!TextUtils.equals(lastArtworkUrl, coverUrl)) {
                lastArtworkUrl = coverUrl == null ? "" : coverUrl;

                try {
                    Glide.with(this)
                            .load(coverUrl)
                            .placeholder(R.drawable.bg_image_placeholder)
                            .error(R.drawable.bg_image_placeholder)
                            .centerCrop()
                            .into(binding.ivPlayerArtwork);
                } catch (Exception e) {
                    Log.e(TAG, "Artwork load failed.", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "renderPlayerState failed.", e);
        }
    }

    private void renderLyricsPreview() {
        try {
            boolean hasTrack = latestPlayerState != null && latestPlayerState.isVisible();

            if (!hasTrack) {
                binding.cardLyricsPreview.setVisibility(View.GONE);
                return;
            }

            binding.cardLyricsPreview.setVisibility(View.VISIBLE);
            binding.progressLyricsPreview.setVisibility(isLyricsLoading ? View.VISIBLE : View.GONE);
            binding.btnOpenLyrics.setEnabled(true);
            binding.tvLyricsPreviewHint.setText(getString(R.string.lyrics_open_fullscreen_hint));

            if (isLyricsLoading) {
                binding.tvLyricsPreviewMode.setText(getString(R.string.lyrics_loading_label));
                binding.tvLyricsPreviewContent.setText(getString(R.string.lyrics_loading_message));
                return;
            }

            if (currentLyrics != null && currentLyrics.hasAnyLyrics()) {
                binding.tvLyricsPreviewMode.setText(currentLyrics.hasSyncedLyrics()
                        ? getString(R.string.lyrics_mode_synced_preview)
                        : getString(R.string.lyrics_mode_static_preview));

                String previewText = currentLyrics.getPreviewText(latestPlayerState.getCurrentPosition());
                if (TextUtils.isEmpty(previewText)) {
                    previewText = getString(R.string.lyrics_unavailable_message);
                }
                binding.tvLyricsPreviewContent.setText(previewText);
            } else {
                binding.tvLyricsPreviewMode.setText(getString(R.string.lyrics_mode_unavailable));
                binding.tvLyricsPreviewContent.setText(getString(R.string.lyrics_unavailable_message));
            }
        } catch (Exception e) {
            Log.e(TAG, "renderLyricsPreview failed.", e);
        }
    }

    private void openLyricsScreen() {
        try {
            if (latestPlayerState == null || !latestPlayerState.isVisible()) {
                showMessage(getString(R.string.lyrics_no_track_message));
                return;
            }

            Intent intent = new Intent(this, LyricsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "openLyricsScreen failed.", e);
            showMessage(getString(R.string.lyrics_open_failed));
        }
    }

    private void showPlaybackSpeedDialog() {
        try {
            if (binding.btnPlaybackSpeed == null || !binding.btnPlaybackSpeed.isEnabled()) {
                return;
            }

            final float[] speedValues = new float[]{
                    0.5f,
                    1.0f,
                    1.25f,
                    1.5f,
                    2.0f
            };

            final String[] labels = new String[]{
                    "0.5x",
                    "1.0x",
                    "1.25x",
                    "1.5x",
                    "2.0x"
            };

            int checkedItem = findNearestSpeedIndex(speedValues, currentPlaybackSpeed);

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.player_speed_dialog_title)
                    .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                        try {
                            if (which >= 0 && which < speedValues.length) {
                                playerViewModel.setPlaybackSpeed(speedValues[which]);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to apply playback speed.", e);
                        }

                        try {
                            dialog.dismiss();
                        } catch (Exception ignored) {
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        try {
                            dialog.dismiss();
                        } catch (Exception ignored) {
                        }
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "showPlaybackSpeedDialog failed.", e);
            showMessage(getString(R.string.player_speed_dialog_error));
        }
    }

    private int findNearestSpeedIndex(float[] speedValues, float currentSpeed) {
        try {
            if (speedValues == null || speedValues.length == 0) {
                return 0;
            }

            int nearestIndex = 0;
            float nearestDistance = Math.abs(speedValues[0] - currentSpeed);

            for (int i = 1; i < speedValues.length; i++) {
                float distance = Math.abs(speedValues[i] - currentSpeed);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestIndex = i;
                }
            }

            return nearestIndex;
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatSpeedLabel(float speed) {
        try {
            if (Math.abs(speed - Math.round(speed)) < 0.001f) {
                return String.format(Locale.getDefault(), "%.1fx", speed);
            }

            if (Math.abs((speed * 10f) - Math.round(speed * 10f)) < 0.001f) {
                return String.format(Locale.getDefault(), "%.1fx", speed);
            }

            return String.format(Locale.getDefault(), "%.2fx", speed);
        } catch (Exception e) {
            return "1.0x";
        }
    }

    private int safeSeekBarValue(long value) {
        try {
            if (value < 0L) {
                return 0;
            }

            if (value > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }

            return (int) value;
        } catch (Exception e) {
            return 0;
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