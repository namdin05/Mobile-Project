package com.melodix.app.Utils;

import android.content.Context;
import android.widget.Toast;

import com.melodix.app.Model.Song;

import java.util.ArrayList;
import java.util.List;

public class SongActionHelper {
    public static void playSongAndSetQueue(Context context, Song selectedSong, List<Song> currentList) {
        PlaybackUtils.playSong(context, (ArrayList<Song>) currentList, selectedSong.getId());
    }

    public static void handleMenuClick(Context context, Song song, String action, List<Song> currentList) {
        switch (action) {
            case "play":
                List<Song> singleList = new ArrayList<>();
                singleList.add(song);
                playSongAndSetQueue(context, song, singleList);
                break;
            case "like":
                Toast.makeText(context, "LIKE " + song.getTitle(), Toast.LENGTH_SHORT).show();
                break;
            case "playlist":
                Toast.makeText(context, "Thêm " + song.getTitle() + " vào PLAYLIST", Toast.LENGTH_SHORT).show();
                break;
            case "comment":
                Toast.makeText(context, "COMMENT " + song.getTitle(), Toast.LENGTH_SHORT).show();
                break;
            case "share":
                Toast.makeText(context, "SHARE " + song.getTitle(), Toast.LENGTH_SHORT).show();
                break;
            case "download":
                Toast.makeText(context, "DOWNLOAD " + song.getTitle(), Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
