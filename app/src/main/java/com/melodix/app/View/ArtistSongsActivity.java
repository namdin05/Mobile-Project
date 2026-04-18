package com.melodix.app.View;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.Utils.ShareUtils;
import com.melodix.app.View.adapters.SongAdapter;
import com.melodix.app.View.dialogs.PlaylistSelectionDialog;

import java.util.ArrayList;
import java.util.List;

public class ArtistSongsActivity extends AppCompatActivity {
    private SongAdapter songAdapter;

    // Lưu trữ toàn bộ danh sách bài hát của nghệ sĩ để truyền vào trình phát (giúp Next/Prev hoạt động)
    private List<Song> currentSongList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_songs);

        String artistId = getIntent().getStringExtra(ArtistDetailActivity.EXTRA_ARTIST_ID);
        if (artistId == null) { finish(); return; }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Kích hoạt nút Phát tất cả (Phát từ bài đầu tiên trong danh sách)
        findViewById(R.id.btn_play_all_songs).setOnClickListener(v -> {
            if (!currentSongList.isEmpty()) {
                playSongAndSetQueue(currentSongList.get(0), currentSongList);
            } else {
                Toast.makeText(this, "Chưa có bài hát nào", Toast.LENGTH_SHORT).show();
            }
        });

        RecyclerView rvAllSongs = findViewById(R.id.rv_all_songs);
        rvAllSongs.setLayoutManager(new LinearLayoutManager(this));

        // KHỞI TẠO ADAPTER VÀ BẮT SỰ KIỆN TỪ MENU 3 CHẤM
        songAdapter = new SongAdapter(this, new ArrayList<>(), new SongAdapter.OnSongActionListener() {
            @Override
            public void onSongClick(Song song, int position) {
                // Click trực tiếp vào bài hát -> Phát nhạc với toàn bộ danh sách
                playSongAndSetQueue(song, currentSongList);
            }

            @Override
            public void onMenuClick(Song song, int position, String actionId) {
                // Điều hướng sang hàm xử lý menu (Truyền kèm danh sách gốc)
                handleMenuClick(song, actionId, currentSongList);
            }
        });
        rvAllSongs.setAdapter(songAdapter);

        // Fetch API
        AppRepository.getInstance(this).getSongsByArtist(artistId, new AppRepository.SongListCallback() {
            @Override public void onSuccess(ArrayList<Song> songs) {
                if (!isFinishing() && !isDestroyed()) {
                    currentSongList = songs; // Lưu dữ liệu vào biến toàn cục
                    songAdapter.update(songs);
                }
            }
            @Override public void onError(String message) {
                Toast.makeText(ArtistSongsActivity.this, "Lỗi tải nhạc: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================
    // CÁC HÀM XỬ LÝ LOGIC PHÁT NHẠC VÀ MENU
    // =========================================================

    private void playSongAndSetQueue(Song selectedSong, List<Song> currentList) {
        // Đẩy list nhạc vào bộ phát
        PlaybackUtils.playSong(this, new ArrayList<>(currentList), selectedSong.getId());
    }

    private void handleMenuClick(Song song, String action, List<Song> listSongs) {
        switch (action) {
            case "play":
                playSongAndSetQueue(song, listSongs);
                break;
            case "like":
                Toast.makeText(this, "LIKE " + song.getTitle(), Toast.LENGTH_SHORT).show();
                break;
            case "playlist":
                showPlaylistSelectionDialog(song);
                break;
            case "comment":
                Toast.makeText(this, "COMMENT " + song.getTitle(), Toast.LENGTH_SHORT).show();
                break;
            case "share":
                if (song != null && song.getId() != null) {
                    ShareUtils.shareContent(this, "song", song.getId(), song.getTitle());
                } else {
                    Toast.makeText(this, "Lỗi dữ liệu bài hát", Toast.LENGTH_SHORT).show();
                }
                break;
            case "download":
                Toast.makeText(this, "DOWNLOAD " + song.getTitle(), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    // ĐÃ SỬA: Lấy trạng thái đăng nhập từ SharedPreferences thay vì SessionManager
    private void showPlaylistSelectionDialog(Song song) {
        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false);

        if (!isLoggedIn) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        PlaylistSelectionDialog dialog = PlaylistSelectionDialog.newInstance(song.getId());
        dialog.setOnPlaylistActionListener(() -> {
            Toast.makeText(this, "Playlist đã được cập nhật", Toast.LENGTH_SHORT).show();
        });

        dialog.show(getSupportFragmentManager(), "playlist_selection");
    }
}