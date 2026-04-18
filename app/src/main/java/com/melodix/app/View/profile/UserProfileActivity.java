package com.melodix.app.View.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Repository.PlaylistRepository;
import com.melodix.app.Repository.ProfileRepository;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.View.adapters.PlaylistAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "extra_user_id";

    private ImageView imgAvatar;
    private TextView tvName, tvFollowerCount, tvFollowingCount;
    private RecyclerView rvPlaylists;
    private Button btnFollow;

    private boolean isFollowing = false;
    private int followerCount = 0;
    private int followingCount = 0;

    private String targetUserId;
    private String myUserId;

    // Khởi tạo API dùng chung cho toàn Class
    private ProfileAPIService apiService;

    // Callback dùng chung cho các API không cần quan tâm kết quả trả về
    private final Callback<Void> silentCallback = new Callback<Void>() {
        @Override public void onResponse(Call<Void> call, Response<Void> response) {}
        @Override public void onFailure(Call<Void> call, Throwable t) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        targetUserId = getIntent().getStringExtra(EXTRA_USER_ID);

        if (targetUserId == null || targetUserId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadUserInfo();
        loadUserPlaylists();

        Profile myProfile = AppRepository.getInstance(this).getCurrentUser();
        if (myProfile != null) {
            myUserId = myProfile.getId();
        }

        loadFollowStats();

        if (myUserId != null && !myUserId.equals(targetUserId)) {
            btnFollow.setVisibility(android.view.View.VISIBLE);
            checkCurrentFollowStatus();
            btnFollow.setOnClickListener(v -> toggleFollowStatus());
        } else {
            btnFollow.setVisibility(android.view.View.GONE);
        }
    }

    private void initViews() {
        imgAvatar = findViewById(R.id.img_user_avatar);
        tvName = findViewById(R.id.tv_user_name);
        rvPlaylists = findViewById(R.id.rv_user_playlists);
        tvFollowerCount = findViewById(R.id.tv_follower_count);
        tvFollowingCount = findViewById(R.id.tv_following_count);
        btnFollow = findViewById(R.id.btn_follow);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo API 1 lần duy nhất
        apiService = RetrofitClient.getSupabaseClient().create(ProfileAPIService.class);
    }

    private void loadUserInfo() {
        new ProfileRepository().getProfileById(targetUserId, new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Profile profile = response.body().get(0);
                    tvName.setText(profile.getDisplayName() != null ? profile.getDisplayName() : "Người dùng Melodix");

                    if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isEmpty()) {
                        Glide.with(UserProfileActivity.this).load(profile.getAvatarUrl())
                                .placeholder(R.drawable.circle_placeholder).circleCrop().into(imgAvatar);
                    } else {
                        imgAvatar.setImageResource(R.drawable.circle_placeholder);
                    }
                } else {
                    tvName.setText("Người dùng ẩn danh");
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                if (!isFinishing() && !isDestroyed()) tvName.setText("Lỗi kết nối mạng");
            }
        });
    }

    private void loadUserPlaylists() {
        PlaylistRepository playlistRepo = new PlaylistRepository(this);
        playlistRepo.getUserPlaylists(targetUserId, new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                if (isFinishing() || isDestroyed() || !response.isSuccessful() || response.body() == null) return;

                List<Playlist> publicPlaylists = new ArrayList<>();
                for (Playlist p : response.body()) {
                    if (p.isPublic) publicPlaylists.add(p);
                }

                if (!publicPlaylists.isEmpty()) {
                    AtomicInteger requestsCompleted = new AtomicInteger(0);
                    int totalRequests = publicPlaylists.size();

                    for (Playlist finalP : publicPlaylists) {
                        playlistRepo.getPlaylistSongs(finalP.id, new Callback<List<PlaylistSong>>() {
                            @Override
                            public void onResponse(Call<List<PlaylistSong>> call, Response<List<PlaylistSong>> responseSongs) {
                                if (responseSongs.isSuccessful() && responseSongs.body() != null) {
                                    finalP.songCount = responseSongs.body().size();
                                }
                                checkIfAllDone();
                            }
                            @Override
                            public void onFailure(Call<List<PlaylistSong>> call, Throwable t) {
                                checkIfAllDone();
                            }
                            private void checkIfAllDone() {
                                if (requestsCompleted.incrementAndGet() == totalRequests) {
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
                }
            }
            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                if (!isFinishing() && !isDestroyed()) Toast.makeText(UserProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==========================================
    // LOGIC FOLLOW & THỐNG KÊ (ĐÃ TỐI ƯU DRY)
    // ==========================================

    private void loadFollowStats() {
        AppRepository repo = AppRepository.getInstance(this);

        repo.getFollowerCount(targetUserId, count -> {
            if (isFinishing() || isDestroyed()) return;
            followerCount = count;
            tvFollowerCount.setText(formatCount(followerCount) + " người theo dõi");
        });

        repo.getFollowingCount(targetUserId, count -> {
            if (isFinishing() || isDestroyed()) return;
            followingCount = count;
            tvFollowingCount.setText(formatCount(followingCount) + " đang theo dõi");
        });
    }

    // Hàm Helper định dạng số lượng chung cho 2 bộ đếm
    private String formatCount(int count) {
        return count >= 1000 ? String.format(java.util.Locale.US, "%.1fK", count / 1000f) : String.valueOf(count);
    }

    private void checkCurrentFollowStatus() {
        apiService.checkFollowStatus("eq." + myUserId, "eq." + targetUserId).enqueue(new Callback<List<Object>>() {
            @Override
            public void onResponse(Call<List<Object>> call, Response<List<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isFollowing = !response.body().isEmpty();
                    updateFollowButtonUI();
                }
            }
            @Override public void onFailure(Call<List<Object>> call, Throwable t) {}
        });
    }

    private void updateFollowButtonUI() {
        btnFollow.setText(isFollowing ? "Đang theo dõi" : "Theo dõi");
        int color = android.graphics.Color.parseColor(isFollowing ? "#535353" : "#1DB954");
        btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void toggleFollowStatus() {
        // 1. Đảo trạng thái và tính toán lại số lượng cực nhanh
        isFollowing = !isFollowing;
        followerCount += isFollowing ? 1 : -1;
        followerCount = Math.max(0, followerCount); // Đảm bảo không bao giờ bị âm số

        // 2. Cập nhật UI ngay lập tức
        updateFollowButtonUI();
        tvFollowerCount.setText(formatCount(followerCount) + " người theo dõi");

        // 3. Gọi API chạy ngầm phía sau bằng silentCallback
        if (isFollowing) {
            Map<String, String> data = new HashMap<>();
            data.put("follower_id", myUserId);
            data.put("artist_id", targetUserId);
            apiService.followUser(data).enqueue(silentCallback);
        } else {
            apiService.unfollowUser("eq." + myUserId, "eq." + targetUserId).enqueue(silentCallback);
        }
    }
}