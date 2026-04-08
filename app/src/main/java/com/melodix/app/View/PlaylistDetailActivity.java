package com.melodix.app.View;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.PlaylistRepository;
import com.melodix.app.View.adapters.PlaylistSongAdapter;
import com.melodix.app.Utils.PlaybackUtils;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYLIST_ID = "extra_playlist_id";

    private PlaylistRepository playlistRepository;
    private RecyclerView rvSongs;
    private PlaylistSongAdapter songAdapter;
    private List<PlaylistSong> playlistSongList = new ArrayList<>();
    private List<Song> songListForPlayback = new ArrayList<>();
    private String playlistId;
    private TextView tvMeta;
    private TextView tvTitle;
    private ImageView imgCover;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistRepository = new PlaylistRepository(this);
        playlistId = getIntent().getStringExtra(EXTRA_PLAYLIST_ID);

        if (playlistId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID Playlist", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadPlaylistData();
        setupDragAndDrop();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        imgCover = findViewById(R.id.img_cover);
        tvTitle = findViewById(R.id.tv_title);
        tvMeta = findViewById(R.id.tv_meta);
        rvSongs = findViewById(R.id.rv_songs);

        btnBack.setOnClickListener(v -> finish());

        rvSongs.setLayoutManager(new LinearLayoutManager(this));

        songAdapter = new PlaylistSongAdapter(this, playlistSongList,
                new PlaylistSongAdapter.OnSongActionListener() {
//                    @Override
//                    public void onSongRemove(PlaylistSong playlistSong) {
//                        removeSongFromPlaylist(playlistSong);
//                    }

                    @Override
                    public void onSongClick(PlaylistSong playlistSong) {
                        playSongFromPlaylist(playlistSong);
                    }
                });

        rvSongs.setAdapter(songAdapter);
    }

    private void loadPlaylistData() {
        tvMeta.setText("Đang tải...");

        // Tải thông tin playlist
        playlistRepository.getPlaylistById(playlistId, new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Playlist p = response.body().get(0);
                    runOnUiThread(() -> {
                        tvTitle.setText(p.name != null ? p.name : "Playlist");
                        if (p.coverRes != null && !p.coverRes.isEmpty()) {
                            Glide.with(PlaylistDetailActivity.this)
                                    .load(p.coverRes)
                                    .placeholder(R.drawable.ic_music_placeholder)
                                    .into(imgCover);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                android.util.Log.e("PLAYLIST_ERROR", "Lỗi: " + t.getMessage());
            }
        });

        // Tải danh sách bài hát
        playlistRepository.getPlaylistSongs(playlistId, new Callback<List<PlaylistSong>>() {
            @Override
            public void onResponse(Call<List<PlaylistSong>> call, Response<List<PlaylistSong>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    playlistSongList.clear();
                    songListForPlayback.clear();

                    for (PlaylistSong ps : response.body()) {
                        if (ps != null && ps.song != null) {
                            // Debug: In ra artistname từ view
                            android.util.Log.d("PLAYLIST_DEBUG", "Song: " + ps.song.getTitle() +
                                    " | Artist from view: " + ps.artistname);

                            playlistSongList.add(ps);
                            songListForPlayback.add(ps.song);
                        }
                    }

                    runOnUiThread(() -> {
                        songAdapter.notifyDataSetChanged();
                        tvMeta.setText(playlistSongList.size() + " bài hát");
                    });
                } else {
                    runOnUiThread(() -> {
                        tvMeta.setText("0 bài hát");
                        Toast.makeText(PlaylistDetailActivity.this,
                                "Không thể tải danh sách bài hát", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(Call<List<PlaylistSong>> call, Throwable t) {
                runOnUiThread(() -> {
                    tvMeta.setText("0 bài hát");
                    Toast.makeText(PlaylistDetailActivity.this,
                            "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void playSongFromPlaylist(PlaylistSong playlistSong) {
        if (playlistSong == null || playlistSong.song == null) {
            Toast.makeText(this, "Không thể phát bài hát này", Toast.LENGTH_SHORT).show();
            return;
        }

        if (songListForPlayback.isEmpty()) {
            Toast.makeText(this, "Playlist không có bài hát nào", Toast.LENGTH_SHORT).show();
            return;
        }

        PlaybackUtils.playSong(this, new ArrayList<>(songListForPlayback),
                playlistSong.song.getId());
    }

    private void removeSongFromPlaylist(PlaylistSong playlistSong) {
        playlistRepository.removeSongFromPlaylist(playlistId, playlistSong.song.getId(),
                new Callback<okhttp3.ResponseBody>() {
                    @Override
                    public void onResponse(Call<okhttp3.ResponseBody> call,
                                           Response<okhttp3.ResponseBody> response) {
                        if (response.isSuccessful()) {
                            int position = playlistSongList.indexOf(playlistSong);
                            if (position != -1) {
                                playlistSongList.remove(position);
                                songListForPlayback.remove(playlistSong.song);
                                runOnUiThread(() -> {
                                    songAdapter.notifyItemRemoved(position);
                                    tvMeta.setText(playlistSongList.size() + " bài hát");
                                });
                            }
                            Toast.makeText(PlaylistDetailActivity.this,
                                    "Đã xóa bài hát", Toast.LENGTH_SHORT).show();
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(PlaylistDetailActivity.this,
                                            "Xóa thất bại", Toast.LENGTH_SHORT).show()
                            );
                        }
                    }

                    @Override
                    public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                        runOnUiThread(() ->
                                Toast.makeText(PlaylistDetailActivity.this,
                                        "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                });
    }

    private void setupDragAndDrop() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();

                if (from < 0 || to < 0 || from >= playlistSongList.size() ||
                        to >= playlistSongList.size()) {
                    return false;
                }

                // Cập nhật UI
                PlaylistSong moved = playlistSongList.remove(from);
                playlistSongList.add(to, moved);

                Song movedSong = songListForPlayback.remove(from);
                songListForPlayback.add(to, movedSong);

                songAdapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Không sử dụng
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(rvSongs);
    }
}