package com.melodix.app.View.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

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
    private TextView tvFollowerCount, tvFollowingCount;
    private android.widget.Button btnFollow;

    private boolean isFollowing = false;
    private int followerCount = 0;
    private int followingCount = 0;
    private boolean showPlaylists = true;
    private String targetUserId;
    private String myUserId;
    
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        String userId = getIntent().getStringExtra(EXTRA_USER_ID);

        
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();


        targetUserId = userId;

        
        
        
        android.content.SharedPreferences prefs = getSharedPreferences("MelodixPrefs", android.content.Context.MODE_PRIVATE);
        myUserId = prefs.getString("USER_ID", null);
        loadUserInfo(userId);
        
        loadFollowerCount();
        loadFollowingCount();

        
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

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));
        tvFollowerCount = findViewById(R.id.tv_follower_count);
        tvFollowingCount = findViewById(R.id.tv_following_count);
        btnFollow = findViewById(R.id.btn_follow);
    }

    private void loadUserInfo(String userId) {
        ProfileRepository profileRepo = new ProfileRepository(getBaseContext());
        profileRepo.getProfileById(userId, new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                
                if (isFinishing() || isDestroyed()) return;
                android.util.Log.d("USER_PROFILE_DEBUG", "Response code: " + response.code());
                android.util.Log.d("USER_PROFILE_DEBUG", "Response body: " +
                        new com.google.gson.Gson().toJson(response.body()));

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Profile profile = response.body().get(0);

                    android.util.Log.d("USER_PROFILE_DEBUG", "Response code: " + response.code());
                    android.util.Log.d("USER_PROFILE_DEBUG", "Response body: " +
                            new com.google.gson.Gson().toJson(response.body()));

                    showPlaylists = profile.isShowPlaylists();
                    if (rvPlaylists != null) {
                        rvPlaylists.setVisibility(showPlaylists ? View.VISIBLE : View.GONE);
                    }
                    
                    tvName.setText(profile.getDisplayName() != null ? profile.getDisplayName() : "Người dùng Melodix");

                    
                    if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isEmpty()) {
                        Glide.with(UserProfileActivity.this)
                                .load(profile.getAvatarUrl())
                                .placeholder(R.drawable.circle_placeholder)
                                .circleCrop()
                                .into(imgAvatar);
                    } else {
                        
                        imgAvatar.setImageResource(R.drawable.circle_placeholder);
                    }
                    if (showPlaylists) {
                        loadUserPlaylists(userId);
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
        if (rvPlaylists == null || rvPlaylists.getVisibility() == View.GONE) {
            android.util.Log.d("PLAYLIST_PRIVACY", "Người dùng đã tắt hiển thị playlist → Không load");
            return;
        }
        PlaylistRepository playlistRepo = new PlaylistRepository(this);

        playlistRepo.getUserPlaylists(userId, new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Playlist> publicPlaylists = new ArrayList<>();

                    
                    for (Playlist p : response.body()) {
                        if (p.isPublic) {
                            publicPlaylists.add(p);
                        }
                    }

                    if (!publicPlaylists.isEmpty()) {
                        
                        int totalRequests = publicPlaylists.size();

                        
                        AtomicInteger requestsCompleted = new AtomicInteger(0);

                        for (int i = 0; i < totalRequests; i++) {
                            final Playlist finalP = publicPlaylists.get(i);

                            
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
                                            if (rvPlaylists != null && rvPlaylists.getVisibility() == View.VISIBLE) {
                                                PlaylistAdapter adapter = new PlaylistAdapter(UserProfileActivity.this, publicPlaylists, playlist -> {
                                                    Intent intent = new Intent(UserProfileActivity.this, com.melodix.app.View.PlaylistDetailActivity.class);
                                                    intent.putExtra("extra_playlist_id", playlist.id);
                                                    startActivity(intent);
                                                });
                                                rvPlaylists.setAdapter(adapter);
                                            }
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

    private void loadFollowerCount() {
        com.melodix.app.Repository.AppRepository.getInstance(this).getFollowerCount(targetUserId, count -> {
            if (isFinishing() || isDestroyed()) return;
            followerCount = count;
            updateFollowerCountUI();
        });
    }

    private void loadFollowingCount() {
        com.melodix.app.Repository.AppRepository.getInstance(this).getFollowingCount(targetUserId, count -> {
            if (isFinishing() || isDestroyed()) return;
            followingCount = count;
            updateFollowingCountUI();
        });
    }

    private void updateFollowerCountUI() {
        String displayCount = followerCount >= 1000 ?
                String.format(java.util.Locale.US, "%.1fK", followerCount / 1000f) : String.valueOf(followerCount);
        tvFollowerCount.setText(displayCount + " follower(s)");
    }

    private void updateFollowingCountUI() {
        String displayCount = followingCount >= 1000 ?
                String.format(java.util.Locale.US, "%.1fK", followingCount / 1000f) : String.valueOf(followingCount);
        tvFollowingCount.setText(displayCount + " following ");
    }

    private void checkCurrentFollowStatus() {
        com.melodix.app.Service.ProfileAPIService apiService = com.melodix.app.Service.RetrofitClient.getClient(getApplication()).create(com.melodix.app.Service.ProfileAPIService.class);
        apiService.checkFollowStatus("eq." + myUserId, "eq." + targetUserId).enqueue(new retrofit2.Callback<List<Object>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Object>> call, retrofit2.Response<List<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isFollowing = !response.body().isEmpty();
                    updateFollowButtonUI();
                }
            }
            @Override public void onFailure(retrofit2.Call<List<Object>> call, Throwable t) {}
        });
    }

    private void updateFollowButtonUI() {
        if (isFollowing) {
            btnFollow.setText("Following");
            btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#535353"))); 
        } else {
            btnFollow.setText("Follow");
            btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1DB954"))); 
        }
    }

    private void toggleFollowStatus() {
        com.melodix.app.Service.ProfileAPIService apiService = com.melodix.app.Service.RetrofitClient.getClient(getApplication()).create(com.melodix.app.Service.ProfileAPIService.class);

        if (isFollowing) {
            
            isFollowing = false;
            followerCount = Math.max(0, followerCount - 1);
            updateFollowButtonUI();
            updateFollowerCountUI();

            apiService.unfollowUser("eq." + myUserId, "eq." + targetUserId).enqueue(new retrofit2.Callback<Void>() {
                @Override public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {}
                @Override public void onFailure(retrofit2.Call<Void> call, Throwable t) {}
            });
        } else {
            
            isFollowing = true;
            followerCount++;
            updateFollowButtonUI();
            updateFollowerCountUI();

            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("follower_id", myUserId);
            data.put("artist_id", targetUserId); 

            apiService.followUser(data).enqueue(new retrofit2.Callback<Void>() {
                @Override public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {}
                @Override public void onFailure(retrofit2.Call<Void> call, Throwable t) {}
            });
        }
    }

 }
