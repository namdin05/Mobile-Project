package com.melodix.app.View.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.Model.Profile;
import com.melodix.app.R;
import com.melodix.app.Repository.PlaylistRepository;
import com.melodix.app.Repository.ProfileRepository;
import com.melodix.app.View.adapters.PlaylistAdapter;
import com.melodix.app.View.adapters.PlaylistSongAdapter;
// IMPORT ADAPTER CỦA SẾP VÀO ĐÂY (Ví dụ:)
// import com.melodix.app.View.adapters.PlaylistAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "extra_user_id";

    private ImageView imgAvatar;
    private TextView tvName;
    private RecyclerView rvPlaylists;

    // KHAI BÁO ADAPTER: Sếp mở comment và đổi tên cho khớp với Adapter hiển thị Playlist của sếp nhé!
    // private PlaylistAdapter playlistAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        String userId = getIntent().getStringExtra(EXTRA_USER_ID);

        // Lớp giáp 1: Check xem có ID truyền sang không
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadUserInfo(userId);
        loadUserPlaylists(userId);
    }

    private void initViews() {
        imgAvatar = findViewById(R.id.img_user_avatar);
        tvName = findViewById(R.id.tv_user_name);
        rvPlaylists = findViewById(R.id.rv_user_playlists);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadUserInfo(String userId) {
        ProfileRepository profileRepo = new ProfileRepository();
        profileRepo.getProfileById(userId, new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                // Lớp giáp 2: Chống crash nếu user đã thoát ra lúc mạng đang load
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Profile profile = response.body().get(0);

                    // 1. Hiển thị Tên
                    tvName.setText(profile.getDisplayName() != null ? profile.getDisplayName() : "Người dùng Melodix");

                    // 2. Hiển thị Avatar (Dùng thư viện Glide bo tròn)
                    if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isEmpty()) {
                        Glide.with(UserProfileActivity.this)
                                .load(profile.getAvatarUrl())
                                .placeholder(R.drawable.circle_placeholder)
                                .circleCrop()
                                .into(imgAvatar);
                    } else {
                        // Nếu user chưa có avatar thì set ảnh mặc định
                        imgAvatar.setImageResource(R.drawable.circle_placeholder);
                    }
                } else {
                    tvName.setText("Người dùng ẩn danh");
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                tvName.setText("Lỗi kết nối mạng");
            }
        });
    }

    private void loadUserPlaylists(String userId) {
        PlaylistRepository playlistRepo = new PlaylistRepository(this);

        playlistRepo.getUserPlaylists(userId, new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                // Lớp giáp bảo vệ: Chống crash nếu User đã đóng Activity
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Playlist> publicPlaylists = new ArrayList<>();

                    // 1. Lọc danh sách Playlist công khai
                    for (Playlist p : response.body()) {
                        if (p.isPublic) {
                            publicPlaylists.add(p);
                        }
                    }

                    if (!publicPlaylists.isEmpty()) {
                        // 👇 CHIẾN THUẬT MỚI: Gom đủ data mới hiện lên
                        int totalRequests = publicPlaylists.size();

                        // Dùng AtomicInteger để đếm số lượng API đã chạy xong một cách an toàn
                        AtomicInteger requestsCompleted = new AtomicInteger(0);

                        for (int i = 0; i < totalRequests; i++) {
                            final Playlist finalP = publicPlaylists.get(i);

                            // Chạy ngầm đi đếm bài hát cho từng Playlist
                            playlistRepo.getPlaylistSongs(finalP.id, new Callback<List<PlaylistSong>>() {
                                @Override
                                public void onResponse(Call<List<PlaylistSong>> call, Response<List<PlaylistSong>> responseSongs) {
                                    if (responseSongs.isSuccessful() && responseSongs.body() != null) {
                                        finalP.songCount = responseSongs.body().size(); // Gán số lượng thật
                                    }
                                    checkIfAllDone();
                                }

                                @Override
                                public void onFailure(Call<List<PlaylistSong>> call, Throwable t) {
                                    // Dù lỗi mạng thì vẫn phải đếm để ứng dụng không bị kẹt
                                    checkIfAllDone();
                                }

                                // Hàm kiểm tra xem đã đếm xong TẤT CẢ các playlist chưa
                                private void checkIfAllDone() {
                                    // Mỗi lần chạy xong 1 playlist thì tăng biến đếm lên 1
                                    if (requestsCompleted.incrementAndGet() == totalRequests) {

                                        // KHI ĐÃ ĐẾM XONG TOÀN BỘ -> MỚI RÁP ADAPTER VÀO
                                        runOnUiThread(() -> {
                                            PlaylistAdapter adapter = new PlaylistAdapter(UserProfileActivity.this, publicPlaylists, playlist -> {
                                                Intent intent = new Intent(UserProfileActivity.this, com.melodix.app.View.PlaylistDetailActivity.class);
                                                intent.putExtra("extra_playlist_id", playlist.id);
                                                startActivity(intent);
                                            });
                                            rvPlaylists.setAdapter(adapter);
                                        });
                                    }
                                }
                            });
                        }
                    } else {
                        android.util.Log.d("PATCH_DEBUG", "Người dùng không có playlist công khai nào.");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(UserProfileActivity.this, "Lỗi kết nối danh sách phát", Toast.LENGTH_SHORT).show();
            }
        });
    }
 }
