package com.melodix.app.Utils;

import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.PlaybackRepository;
import com.melodix.app.Service.AudioPlayerService;
import com.melodix.app.PlayerActivity;
import java.util.ArrayList;

public class PlaybackUtils {
    public static void playSong(Context context, ArrayList<Song> queue, String songId) {
        playSong(context, queue, songId, false);
    }

    public static void playSong(Context context, ArrayList<Song> queue, String songId, boolean fromPlaylist) {
        PlaybackRepository.getInstance().setQueue(queue, songId);

        // CHỈ mở PlayerActivity và truyền cờ "start_playback"
        Intent playerIntent = new Intent(context, PlayerActivity.class);
        playerIntent.putExtra(PlayerActivity.EXTRA_SONG_ID, songId);
        playerIntent.putExtra("start_playback", true);
        context.startActivity(playerIntent);
    }

    public static void openPlayer(Context context, String songId) {
        Intent playerIntent = new Intent(context, PlayerActivity.class);
        playerIntent.putExtra(PlayerActivity.EXTRA_SONG_ID, songId);
        context.startActivity(playerIntent);
    }

    public static void sendAction(Context context, String action) {
        Intent intent = new Intent(context, AudioPlayerService.class);
        intent.setAction(action);
        context.startService(intent);
    }

    public static void setSpeed(Context context, float speed) {
        Intent intent = new Intent(context, AudioPlayerService.class);
        intent.setAction(AudioPlayerService.ACTION_SET_SPEED);
        intent.putExtra(AudioPlayerService.EXTRA_SPEED, speed);
        context.startService(intent);
    }

    public static void setSleepTimer(Context context, int minutes) {
        Intent intent = new Intent(context, AudioPlayerService.class);
//        intent.setAction(AudioPlayerService.ACTION_SET_SLEEP_TIMER);
        intent.putExtra(AudioPlayerService.EXTRA_TIMER_MINUTES, minutes);
        context.startService(intent);
    }

    public static void seekTo(Context context, int position) {
        Intent intent = new Intent(context, AudioPlayerService.class);
        intent.setAction(AudioPlayerService.ACTION_SEEK_TO);
        intent.putExtra(AudioPlayerService.EXTRA_SEEK, position);
        context.startService(intent);
    }
}
