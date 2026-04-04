package com.melodix.app.Repository;

import com.melodix.app.Model.Song;
import java.util.ArrayList;
import java.util.List;

public class PlaybackRepository {
    private static PlaybackRepository instance;
    private List<Song> currentQueue;
    private int currentIndex;

   // block default constructor
    private PlaybackRepository() {
        currentQueue = new ArrayList<>();
        currentIndex = -1;
    }

    // singleton
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

    // Lấy bài hát hiện tại
    public Song getCurrentSong() {
        if (currentQueue.isEmpty() || currentIndex < 0 || currentIndex >= currentQueue.size()) return null;
        return currentQueue.get(currentIndex);
    }

    // Chuyển bài tiếp theo
    public Song moveNext() {
        if (currentQueue.isEmpty()) return null;
        currentIndex = (currentIndex + 1) % currentQueue.size(); // neu currentIndex la cuoi bai thi ve dau bai
        return getCurrentSong();
    }

    // Lùi bài trước đó
    public Song movePrevious() {
        if (currentQueue.isEmpty()) return null;
        currentIndex = (currentIndex - 1 + currentQueue.size()) % currentQueue.size();
        return getCurrentSong();
    }

    private int findSongIndex(String songId) {
        for (int i = 0; i < currentQueue.size(); i++) {
            if (currentQueue.get(i).getId().equals(songId)) return i; // Thay getId() bằng tên biến tương ứng của bạn
        }
        return 0; // Mặc định phát bài đầu tiên nếu không tìm thấy
    }
}
