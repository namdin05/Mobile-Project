package com.melodix.app.View;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.melodix.app.Model.Album;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.Utils.ShareUtils;
import com.melodix.app.View.adapters.SongAdapter;

import java.util.ArrayList;

public class AlbumDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ALBUM_ID = "extra_album_id";
    private AppRepository repository;

    private RecyclerView rvTracks;
    private SongAdapter trackAdapter;
    private TextView tvTrackCount;
    private com.melodix.app.Model.MiniPlayerController miniPlayerController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        repository = AppRepository.getInstance(this);
        String albumId = getIntent().getStringExtra(EXTRA_ALBUM_ID);

        if (albumId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID Album", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        tvTrackCount = findViewById(R.id.tv_track_count);

        ImageView btnShare = findViewById(R.id.btn_share);
        btnShare.setOnClickListener(v ->{
            if(repository != null){
                TextView tvTitle = findViewById(R.id.tv_title);
                String albumName = tvTitle.getText().toString();
                if (!albumName.equals("Đang tải...")){
                    com.melodix.app.Utils.ShareUtils.shareContent(
                            AlbumDetailActivity.this,
                            "album",
                            albumId,
                            albumName
                    );
                }else{
                    Toast.makeText(AlbumDetailActivity.this, "Vui lòng đợi tải xong thông tin album", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 1. Cài đặt RecyclerView cho Track List
        rvTracks = findViewById(R.id.rv_tracks);
        rvTracks.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo Adapter
        trackAdapter = new SongAdapter(this, new ArrayList<>(), new SongAdapter.OnSongActionListener() {
            @Override
            public void onSongClick(Song song, int position) {
                // Nhóm 6 sẽ viết logic MediaPlayer ở đây sau nhé
                PlaybackUtils.playSong(AlbumDetailActivity.this, new ArrayList<>(trackAdapter.getSongs()), song.getId());            }

            @Override
            public void onMenuClick(Song song, int position, String actionId) {
                // Xử lý sự kiện Menu 3 chấm
                switch (actionId) {
                    case "share":
                        // 👇 XÓA DÒNG NÀY ĐI 👇
                        // ShareUtils.shareSongToFriends(AlbumDetailActivity.this, song);

                        // 👇 THAY BẰNG ĐOẠN NÀY ĐỂ ĐỒNG BỘ VỚI PLAYER ACTIVITY 👇
                        if (song != null && song.getId() != null) {
                            com.melodix.app.Utils.ShareUtils.shareContent(
                                    AlbumDetailActivity.this,
                                    "song",           // Type là bài hát
                                    song.getId(),     // ID của bài hát người dùng vừa bấm
                                    song.getTitle()   // Tên bài hát
                            );
                        } else {
                            Toast.makeText(AlbumDetailActivity.this, "Lỗi dữ liệu bài hát", Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case "play":
                        // Xử lý nút play trong menu
                        java.util.ArrayList<Song> singleList = new java.util.ArrayList<>();
                        singleList.add(song);
                        PlaybackUtils.playSong(AlbumDetailActivity.this, singleList, song.getId());
                        break;

                    case "like":
                        Toast.makeText(AlbumDetailActivity.this, "Đã thích " + song.getTitle(), Toast.LENGTH_SHORT).show();
                        break;

                    default:
                        Toast.makeText(AlbumDetailActivity.this, "Đang phát triển: " + actionId, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
        rvTracks.setAdapter(trackAdapter);

        // XỬ LÝ SỰ KIỆN NÚT PHÁT TẤT CẢ
        View btnPlayAll = findViewById(R.id.btn_play_all);
        btnPlayAll.setOnClickListener(v -> {
            if (trackAdapter != null && trackAdapter.getSongs() != null && !trackAdapter.getSongs().isEmpty()) {
                // Lấy toàn bộ danh sách bài hát trong Album
                ArrayList<Song> allSongs = new ArrayList<>(trackAdapter.getSongs());

                // Gọi hàm PlaybackUtils, truyền list vào và bắt đầu phát từ bài đầu tiên (index 0)
                PlaybackUtils.playSong(AlbumDetailActivity.this, allSongs, allSongs.get(0).getId());
            } else {
                Toast.makeText(AlbumDetailActivity.this, "Album chưa có bài hát nào để phát", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. GỌI API LẤY CHI TIẾT ALBUM
        repository.getAlbumById(albumId, new AppRepository.AlbumCallback() {
            @Override
            public void onSuccess(Album album) {
                // Lớp giáp 1: Chống crash nếu người dùng đã thoát màn hình
                if (isFinishing() || isDestroyed()) return;

                ImageView imgCover = findViewById(R.id.img_cover);
                TextView tvTitle = findViewById(R.id.tv_title);
                TextView tvSubtitle = findViewById(R.id.tv_subtitle);
                TextView tvDescription = findViewById(R.id.tv_description);

                tvTitle.setText(album.title);
                String artist = album.artistName != null ? album.artistName : "Nghệ sĩ ẩn danh";
                tvSubtitle.setText(artist + " • " + album.year);

                // UX Đỉnh cao: Nếu album không có mô tả, giấu luôn cái khung đi cho đẹp
                if (TextUtils.isEmpty(album.description)) {
                    tvDescription.setVisibility(View.GONE);
                } else {
                    tvDescription.setVisibility(View.VISIBLE);
                    tvDescription.setText(album.description);
                }

                // Nâng cấp Glide: Thêm hiệu ứng fade-in mượt mà khi load ảnh mạng
                Glide.with(AlbumDetailActivity.this)
                        .load(album.coverRes)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(imgCover);

                // 3. LẤY ALBUM XONG THÌ GỌI TIẾP API LẤY BÀI HÁT
                repository.getSongsByAlbum(albumId, new AppRepository.SongListCallback() {
                    @Override
                    public void onSuccess(ArrayList<Song> songs) {
                        // Lớp giáp 2
                        if (isFinishing() || isDestroyed()) return;

                        trackAdapter.update(songs);
                        // Cập nhật số lượng bài hát lên tiêu đề
                        tvTrackCount.setText("Danh sách bài hát (" + songs.size() + ")");
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(AlbumDetailActivity.this, "Lỗi lấy bài hát: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                // Lớp giáp 3
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(AlbumDetailActivity.this, "Album không tồn tại hoặc lỗi mạng", Toast.LENGTH_SHORT).show();
                finish();
            }

        });
        miniPlayerController = new com.melodix.app.Model.MiniPlayerController(this);
    }
    // THÊM 2 HÀM NÀY VÀO TRONG CLASS AlbumDetailActivity
    @Override
    protected void onResume() {
        super.onResume();
        if (miniPlayerController != null) {
            miniPlayerController.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miniPlayerController != null) {
            miniPlayerController.onPause();
        }
    }
}