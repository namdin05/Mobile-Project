package com.melodix.app.View;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.ListenHistoryItem;
import com.melodix.app.Model.MiniPlayerController;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Service.SongAPIService;
import com.melodix.app.Utils.SessionManager;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.View.adapters.SongAdapter;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecentHistoryActivity extends AppCompatActivity {

    private RecyclerView rvAllRecent;
    private SongAdapter songAdapter;
    private List<Song> allRecentSongs = new ArrayList<>();

    // THÊM: MiniPlayerController giống AlbumDetailActivity
    private MiniPlayerController miniPlayerController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_history);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rvAllRecent = findViewById(R.id.rv_all_recent);
        rvAllRecent.setLayoutManager(new LinearLayoutManager(this));

        songAdapter = new SongAdapter(this, new ArrayList<>(), new SongAdapter.OnSongActionListener(){
            @Override
            public void onSongClick(Song song, int position) {
                ArrayList<Song> queue = new ArrayList<>(allRecentSongs);
                PlaybackUtils.playSong(RecentHistoryActivity.this, queue, song.getId());
            }

            @Override
            public void onMenuClick(Song song, int position, String actionId) {
                // Có thể thêm xử lý menu nếu cần
            }
        });

        rvAllRecent.setAdapter(songAdapter);

        // THÊM: Khởi tạo MiniPlayerController giống AlbumDetailActivity
        miniPlayerController = new MiniPlayerController(this);

        loadAllRecentHistory();
    }

    // THÊM: onResume để cập nhật mini player
    @Override
    protected void onResume() {
        super.onResume();
        if (miniPlayerController != null) {
            miniPlayerController.onResume();
        }
    }

    // THÊM: onPause để dọn dẹp
    @Override
    protected void onPause() {
        super.onPause();
        if (miniPlayerController != null) {
            miniPlayerController.onPause();
        }
    }

    private void loadAllRecentHistory() {
        String userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) {
            finish();
            return;
        }

        SongAPIService api = RetrofitClient.getClient(this).create(SongAPIService.class);

        // Dùng API mới với tên VIEW listen_history_view, limit lớn
        api.getListenHistoryFromView("eq." + userId, 500)
                .enqueue(new Callback<List<ListenHistoryItem>>() {
                    @Override
                    public void onResponse(Call<List<ListenHistoryItem>> call,
                                           Response<List<ListenHistoryItem>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            allRecentSongs.clear();

                            for (ListenHistoryItem item : response.body()) {
                                Song song = item.getSong();
                                allRecentSongs.add(song);

                                Log.d("RECENT_HISTORY", "Bài: " + song.getTitle() +
                                        " - Artist: " + song.getArtistName());
                            }

                            songAdapter.update((ArrayList<Song>) allRecentSongs);

                            Log.d("RECENT_HISTORY", "Tổng số: " + allRecentSongs.size() + " bài");

                            if (allRecentSongs.isEmpty()) {
                                Toast.makeText(RecentHistoryActivity.this,
                                        "Chưa có lịch sử nghe nhạc", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e("RECENT_HISTORY", "Response error: " + response.code());
                            Toast.makeText(RecentHistoryActivity.this,
                                    "Lỗi tải lịch sử", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ListenHistoryItem>> call, Throwable t) {
                        Log.e("RECENT_HISTORY", "Network error: " + t.getMessage());
                        Toast.makeText(RecentHistoryActivity.this,
                                "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}