package com.melodix.app.Model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.PlaybackRepository;
import com.melodix.app.Service.AudioPlayerService;
import com.melodix.app.Utils.PlaybackUtils;

public class MiniPlayerController {
    private final AppCompatActivity activity;
    private final LinearLayout miniPlayer;
    private ImageView miniCover;
    private TextView miniTitle;
    private TextView miniSubtitle;
    private ImageButton miniPlayPause;

    // Bộ đếm giờ để cập nhật nút Play/Pause
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable miniPlayerWatcher = new Runnable() {
        @Override
        public void run() {
            if (miniPlayer != null && miniPlayer.getVisibility() == View.VISIBLE) {
                miniPlayPause.setImageResource(AudioPlayerService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
            }
            mainHandler.postDelayed(this, 500);
        }
    };

    // Bộ lắng nghe sóng Broadcast
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMiniPlayer();
        }
    };

    // Constructor: Nạp Activity và tìm các thành phần giao diện
    public MiniPlayerController(AppCompatActivity activity) {
        this.activity = activity;
        this.miniPlayer = activity.findViewById(R.id.mini_player_root); // Đảm bảo ID này khớp với file XML include

        if (this.miniPlayer != null) {
            miniCover = miniPlayer.findViewById(R.id.mini_cover);
            miniTitle = miniPlayer.findViewById(R.id.mini_title);
            miniSubtitle = miniPlayer.findViewById(R.id.mini_subtitle);
            miniPlayPause = miniPlayer.findViewById(R.id.mini_play_pause);

            miniPlayer.setOnClickListener(v -> {
                String currentSongId = AudioPlayerService.getCurrentSongId();
                if (currentSongId != null) {
                    PlaybackUtils.openPlayer(activity, currentSongId);
                }
            });

            miniPlayPause.setOnClickListener(v -> {
                boolean isNowPlaying = !AudioPlayerService.isPlaying();
                miniPlayPause.setImageResource(isNowPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
                PlaybackUtils.sendAction(activity, AudioPlayerService.ACTION_TOGGLE_PLAY);
            });
        }
    }

    // Cập nhật giao diện
    private void updateMiniPlayer() {
        String currentSongId = AudioPlayerService.getCurrentSongId();
        Song song = PlaybackRepository.getInstance().getCurrentSong();

        if (currentSongId == null || song == null || miniPlayer == null) {
            if (miniPlayer != null) miniPlayer.setVisibility(View.GONE);
            return;
        }

        miniPlayer.setVisibility(View.VISIBLE);
        miniTitle.setText(song.getTitle());
        miniSubtitle.setText(song.getArtistName());
        miniPlayPause.setImageResource(AudioPlayerService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);

        if (song.getCoverUrl() != null && !song.getCoverUrl().isEmpty()) {
            Glide.with(activity).load(song.getCoverUrl())
                    .placeholder(R.drawable.ic_logo).error(R.drawable.ic_logo).into(miniCover);
        } else {
            miniCover.setImageResource(R.drawable.ic_logo);
        }
    }

    // Bật radar (Gọi trong onResume của Activity)
    public void onResume() {
        if (miniPlayer == null) return;
        ContextCompat.registerReceiver(activity, stateReceiver,
                new IntentFilter(AudioPlayerService.ACTION_STATE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        updateMiniPlayer();
        mainHandler.post(miniPlayerWatcher);
    }

    // Tắt radar (Gọi trong onPause của Activity)
    public void onPause() {
        if (miniPlayer == null) return;
        try { activity.unregisterReceiver(stateReceiver); } catch (Exception ignored) {}
        mainHandler.removeCallbacks(miniPlayerWatcher);
    }
}