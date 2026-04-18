package com.melodix.app.View;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.ShareUtils;
import com.melodix.app.View.adapters.SongAdapter;
import java.util.ArrayList;

public class ArtistSongsActivity extends AppCompatActivity {
    private SongAdapter songAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_songs);

        String artistId = getIntent().getStringExtra(ArtistDetailActivity.EXTRA_ARTIST_ID);
        if (artistId == null) { finish(); return; }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_play_all_songs).setOnClickListener(v ->
                Toast.makeText(this, "Đang phát triển", Toast.LENGTH_SHORT).show());

        RecyclerView rvAllSongs = findViewById(R.id.rv_all_songs);
        rvAllSongs.setLayoutManager(new LinearLayoutManager(this));

        // KHỞI TẠO ADAPTER VÀ BẮT SỰ KIỆN TỪ MENU 3 CHẤM
        songAdapter = new SongAdapter(this, new ArrayList<>(), new SongAdapter.OnSongActionListener() {
            @Override
            public void onSongClick(Song song, int position) {
                // Thêm code phát nhạc ở đây sau nhé
                Toast.makeText(ArtistSongsActivity.this, "Phát bài: " + song.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMenuClick(Song song, int position, String actionId) {
                // Đón lõng tín hiệu "share" từ SongAdapter
                if ("share".equalsIgnoreCase(actionId)) {
                        if (song != null && song.getId() != null) {
                            com.melodix.app.Utils.ShareUtils.shareContent(
                                    ArtistSongsActivity.this,
                                    "song",           // Type là bài hát
                                    song.getId(),     // ID của bài hát người dùng vừa bấm
                                    song.getTitle()   // Tên bài hát
                            );
                        } else {
                            Toast.makeText(ArtistSongsActivity.this, "Lỗi dữ liệu bài hát", Toast.LENGTH_SHORT).show();
                        }

                }
                else if ("play".equalsIgnoreCase(actionId)) {
                    Toast.makeText(ArtistSongsActivity.this, "Phát bài: " + song.getTitle(), Toast.LENGTH_SHORT).show();
                }
                // Bạn có thể xử lý thêm các nút like, playlist, download... ở đây
            }
        });
        rvAllSongs.setAdapter(songAdapter);

        AppRepository.getInstance(this).getSongsByArtist(artistId, new AppRepository.SongListCallback() {
            @Override public void onSuccess(ArrayList<Song> songs) {
                if (!isFinishing() && !isDestroyed()) songAdapter.update(songs);
            }
            @Override public void onError(String message) {
                Toast.makeText(ArtistSongsActivity.this, "Lỗi tải nhạc: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================
    // TÍNH NĂNG CHIA SẺ TỪ DANH SÁCH BÀI HÁT
    // =========================================================

}