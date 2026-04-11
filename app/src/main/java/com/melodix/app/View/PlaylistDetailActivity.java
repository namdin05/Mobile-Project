package com.melodix.app.View;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.melodix.app.View.dialogs.EditPlaylistDialog;   // ← Import dialog mới
import com.melodix.app.Utils.PlaybackUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.ResponseBody;
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
    private Playlist currentPlaylist;

    private TextView tvMeta;
    private TextView tvTitle;
    private ImageView imgCover;

    private EditPlaylistDialog currentEditDialog;
    private ActivityResultLauncher<String> editImagePickerLauncher;

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

        // Khởi tạo launcher chọn ảnh cho Edit Dialog
        editImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && currentEditDialog != null) {
                        currentEditDialog.setSelectedCoverUri(uri);
                    }
                });

        initViews();
        loadPlaylistData();
        setupDragAndDrop();
        setupMoreMenu();
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
                    @Override
                    public void onSongClick(PlaylistSong playlistSong) {
                        playSongFromPlaylist(playlistSong);
                    }
                    @Override
                    public void onMoreClick(PlaylistSong playlistSong, int position) {
                        showSongMenu(playlistSong, position);
                    }
                });

        rvSongs.setAdapter(songAdapter);
    }

    private void setupMoreMenu() {
        ImageButton btnMore = findViewById(R.id.btn_more);
        btnMore.setOnClickListener(v -> showPlaylistOptionsMenu());
    }

    private void showPlaylistOptionsMenu() {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, findViewById(R.id.btn_more));
        popup.getMenuInflater().inflate(R.menu.menu_playlist_options, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                showEditPlaylistDialog();
                return true;
            } else if (id == R.id.action_delete) {
                showDeleteConfirmationDialog();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void loadPlaylistData() {
        tvMeta.setText("Đang tải...");

        // Tải thông tin playlist
        playlistRepository.getPlaylistById(playlistId, new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    currentPlaylist = response.body().get(0);   // ← Lưu playlist hiện tại

                    runOnUiThread(() -> {
                        tvTitle.setText(currentPlaylist.name != null ? currentPlaylist.name : "Playlist");
                        if (currentPlaylist.coverRes != null && !currentPlaylist.coverRes.isEmpty()) {
                            Glide.with(PlaylistDetailActivity.this)
                                    .load(currentPlaylist.coverRes)
                                    .placeholder(R.drawable.ic_music_placeholder)
                                    .into(imgCover);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                Log.e("PLAYLIST_ERROR", "Lỗi tải playlist: " + t.getMessage());
            }
        });

        // Tải danh sách bài hát
        playlistRepository.getPlaylistSongs(playlistId, new Callback<List<PlaylistSong>>() {
            @Override
            public void onResponse(Call<List<PlaylistSong>> call, Response<List<PlaylistSong>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    playlistSongList.clear();
                    songListForPlayback.clear();

                    List<PlaylistSong> loaded = response.body();
                    Collections.sort(loaded, (a, b) -> Integer.compare(a.orderIndex, b.orderIndex));

                    for (PlaylistSong ps : loaded) {
                        if (ps != null && ps.song != null) {
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
                        Toast.makeText(PlaylistDetailActivity.this, "Không thể tải danh sách bài hát", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(Call<List<PlaylistSong>> call, Throwable t) {
                runOnUiThread(() -> {
                    tvMeta.setText("0 bài hát");
                    Toast.makeText(PlaylistDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

        PlaybackUtils.playSong(this, new ArrayList<>(songListForPlayback), playlistSong.song.getId());
    }

    // Drag and drop song in playlist
    private void setupDragAndDrop() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {

                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();

                if (fromPos < 0 || toPos < 0 ||
                        fromPos >= playlistSongList.size() || toPos >= playlistSongList.size()) {
                    return false;
                }

                // Hoán đổi vị trí
                PlaylistSong moved = playlistSongList.remove(fromPos);
                playlistSongList.add(toPos, moved);

                Song movedPlayback = songListForPlayback.remove(fromPos);
                songListForPlayback.add(toPos, movedPlayback);

                songAdapter.notifyItemMoved(fromPos, toPos);
                return true;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // Khi thả tay → lưu thứ tự mới
                saveNewOrderToDatabase();
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(rvSongs);
    }

    //Lưu thứ tự mới vào database
    private void saveNewOrderToDatabase() {
        if (playlistSongList == null || playlistSongList.isEmpty()) return;

        Log.d("DRAG_DROP", "Đang lưu thứ tự mới cho " + playlistSongList.size() + " bài hát");

        for (int i = 0; i < playlistSongList.size(); i++) {
            PlaylistSong ps = playlistSongList.get(i);
            if (ps == null || ps.song == null || ps.song.getId() == null) continue;

            final int newOrder = i;

            playlistRepository.updatePlaylistSongOrder(
                    playlistId,
                    ps.song.getId(),
                    newOrder,
                    new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Log.d("DRAG_DROP", "Cập nhật order_index thành công tại vị trí " + newOrder);
                            } else {
                                Log.e("DRAG_DROP", "Cập nhật order thất bại: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.e("DRAG_DROP", "Lỗi mạng khi cập nhật order: " + t.getMessage());
                        }
                    });
        }
    }

    // Hiển thị menu song trong playlist
    private void showSongMenu(PlaylistSong playlistSong, int position) {
        if (playlistSong == null || playlistSong.song == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetTheme);

        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.dialog_song_menu, null);
        bottomSheet.setContentView(bottomSheetView);

        View parent = (View) bottomSheetView.getParent();
        if (parent != null) parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        bottomSheetView.findViewById(R.id.menu_play).setOnClickListener(v -> {
            playSongFromPlaylist(playlistSong);
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_like).setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng Like đang phát triển", Toast.LENGTH_SHORT).show();
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_add_playlist).setOnClickListener(v -> {
            // Thêm vào playlist khác
            com.melodix.app.View.dialogs.PlaylistSelectionDialog dialog =
                    com.melodix.app.View.dialogs.PlaylistSelectionDialog.newInstance(playlistSong.song.getId());
            dialog.show(getSupportFragmentManager(), "playlist_selection");
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_share).setOnClickListener(v -> {
            // Share bài hát
            Toast.makeText(this, "Đang chia sẻ...", Toast.LENGTH_SHORT).show();
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_download).setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng Download đang phát triển", Toast.LENGTH_SHORT).show();
            bottomSheet.dismiss();
        });

        // Xóa khỏi playlist hiện tại
        TextView menuRemove = bottomSheetView.findViewById(R.id.menu_remove_playlist);
        menuRemove.setVisibility(View.VISIBLE);
        menuRemove.setOnClickListener(v -> {
            removeSongFromCurrentPlaylist(playlistSong, position);
            bottomSheet.dismiss();
        });

        bottomSheet.show();
    }
    private void removeSongFromCurrentPlaylist(PlaylistSong playlistSong, int position) {
        if (playlistSong == null || playlistSong.song == null) return;

        playlistRepository.removeSongFromPlaylist(playlistId, playlistSong.song.getId(),
                new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(PlaylistDetailActivity.this, "Đã xóa bài hát khỏi playlist", Toast.LENGTH_SHORT).show();

                            // Xóa khỏi danh sách hiển thị
                            playlistSongList.remove(position);
                            songListForPlayback.remove(position);
                            songAdapter.notifyItemRemoved(position);
                            songAdapter.notifyItemRangeChanged(position, playlistSongList.size());

                            tvMeta.setText(playlistSongList.size() + " bài hát");
                        } else {
                            Toast.makeText(PlaylistDetailActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(PlaylistDetailActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // Edit playlist
    private void showEditPlaylistDialog() {
        if (currentPlaylist == null) {
            Toast.makeText(this, "Chưa tải xong thông tin playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        currentEditDialog = new EditPlaylistDialog(
                this,
                currentPlaylist,
                updatedPlaylist -> {
                    // Refresh UI sau khi chỉnh sửa thành công
                    tvTitle.setText(updatedPlaylist.name);
                    if (updatedPlaylist.coverRes != null && !updatedPlaylist.coverRes.isEmpty()) {
                        Glide.with(PlaylistDetailActivity.this)
                                .load(updatedPlaylist.coverRes)
                                .placeholder(R.drawable.ic_music_placeholder)
                                .into(imgCover);
                    }

                },
                editImagePickerLauncher
        );

        currentEditDialog.show();
    }

    // Delete Playlist
    private void showDeleteConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa playlist")
                .setMessage("Bạn có chắc muốn xóa playlist này? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> deletePlaylist())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deletePlaylist() {
        playlistRepository.deletePlaylist(playlistId, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PlaylistDetailActivity.this, "Đã xóa playlist", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PlaylistDetailActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(PlaylistDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}