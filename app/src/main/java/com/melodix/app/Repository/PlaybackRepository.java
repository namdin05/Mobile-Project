package com.melodix.app.Repository;

import com.melodix.app.Model.Song;
import java.util.ArrayList;
import java.util.List;

public class PlaybackRepository {
    private static PlaybackRepository instance;
    private List<Song> currentQueue;
    private int currentIndex;

    private PlaybackRepository() {
        currentQueue = new ArrayList<>();
        currentIndex = -1;
    }

    public static synchronized PlaybackRepository getInstance() {
        if (instance == null) {
            instance = new PlaybackRepository();
        }
        return instance;
    }

    public void setQueue(List<Song> queue, String startSongId) {
        this.currentQueue = new ArrayList<>(queue);
        this.currentIndex = findSongIndex(startSongId);
    }

    public Song getCurrentSong() {
        if (currentQueue.isEmpty() || currentIndex < 0 || currentIndex >= currentQueue.size()) return null;
        return currentQueue.get(currentIndex);
    }

    public Song moveNext() {
        if (currentQueue.isEmpty()) return null;
        currentIndex = (currentIndex + 1) % currentQueue.size(); // neu currentIndex la cuoi bai thi ve dau bai
        return getCurrentSong();
    }

    public Song getSongById(String songId) {
        if (currentQueue == null || currentQueue.isEmpty()) return null;

        for (int i = 0; i < currentQueue.size(); i++) {
            if (currentQueue.get(i).getId().equals(songId)) {
                this.currentIndex = i;
                return currentQueue.get(i);
            }
        }
        return null;
    }


    public Song movePrevious() {
        if (currentQueue.isEmpty()) return null;
        currentIndex = (currentIndex - 1 + currentQueue.size()) % currentQueue.size();
        return getCurrentSong();
    }

    private int findSongIndex(String songId) {
        for (int i = 0; i < currentQueue.size(); i++) {
            if (currentQueue.get(i).getId().equals(songId)) return i;
        }
        return 0;
    }


    public void setCurrentSong(Song song) {
        if (song == null) return;
        this.currentQueue = new ArrayList<>();
        this.currentQueue.add(song);
        this.currentIndex = 0;
    }
}
