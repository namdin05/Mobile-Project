package com.melodix.app.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.melodix.app.R;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowManager {

    private final Context context;
    private final String artistId;
    private final String myUserId;
    private final Button btnFollow;
    private final TextView tvFollowerCount;

    private boolean isFollowing = false;
    private int followerCount = 0;
    private FollowListener listener;

    public interface FollowListener {
        void onFollowStateChanged(boolean isFollowing, int followerCount);
        void onError(String message);
    }

    public FollowManager(Context context, String artistId, Button btnFollow, TextView tvFollowerCount) {
        this.context = context;
        this.artistId = artistId;
        this.btnFollow = btnFollow;
        this.tvFollowerCount = tvFollowerCount;

        // Lấy user ID hiện tại
        SharedPreferences prefs = context.getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        this.myUserId = prefs.getString("USER_ID", null);
    }

    public void setListener(FollowListener listener) {
        this.listener = listener;
    }

    public void init() {
        if (myUserId == null || myUserId.isEmpty() || myUserId.equals(artistId)) {
            btnFollow.setVisibility(android.view.View.GONE);
            return;
        }

        btnFollow.setVisibility(android.view.View.VISIBLE);
        btnFollow.setOnClickListener(v -> toggleFollow());
        checkFollowStatus();
    }

    public void setFollowerCount(int count) {
        this.followerCount = count;
        updateFollowerCountUI();
    }

    private void checkFollowStatus() {
        ProfileAPIService apiService = RetrofitClient.getClient(context)
                .create(ProfileAPIService.class);

        apiService.checkFollowStatus("eq." + myUserId, "eq." + artistId)
                .enqueue(new Callback<List<Object>>() {
                    @Override
                    public void onResponse(Call<List<Object>> call, Response<List<Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            isFollowing = !response.body().isEmpty();
                            updateFollowButtonUI();
                            if (listener != null) {
                                listener.onFollowStateChanged(isFollowing, followerCount);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Object>> call, Throwable t) {
                        // Không làm gì cả
                    }
                });
    }

    private void toggleFollow() {
        if (myUserId == null || myUserId.isEmpty()) {
            Toast.makeText(context, "Vui lòng đăng nhập để theo dõi", Toast.LENGTH_SHORT).show();
            return;
        }

        ProfileAPIService apiService = RetrofitClient.getClient(context)
                .create(ProfileAPIService.class);

        final boolean wasFollowing = isFollowing;

        // Optimistic update
        isFollowing = !isFollowing;
        updateFollowButtonUI();

        if (isFollowing) {
            followerCount++;
        } else {
            followerCount = Math.max(0, followerCount - 1);
        }
        updateFollowerCountUI();

        if (wasFollowing) {
            // Unfollow
            apiService.unfollowUser("eq." + myUserId, "eq." + artistId)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (!response.isSuccessful()) {
                                rollback(wasFollowing);
                                Toast.makeText(context, "Lỗi khi bỏ theo dõi", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Đã bỏ theo dõi", Toast.LENGTH_SHORT).show();
                                if (listener != null) {
                                    listener.onFollowStateChanged(isFollowing, followerCount);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            rollback(wasFollowing);
                            Toast.makeText(context, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Follow
            Map<String, String> data = new HashMap<>();
            data.put("follower_id", myUserId);
            data.put("artist_id", artistId);

            apiService.followUser(data).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (!response.isSuccessful()) {
                        rollback(wasFollowing);
                        Toast.makeText(context, "Lỗi khi theo dõi", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Đã theo dõi nghệ sĩ", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onFollowStateChanged(isFollowing, followerCount);
                        }
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    rollback(wasFollowing);
                    Toast.makeText(context, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void rollback(boolean wasFollowing) {
        isFollowing = wasFollowing;
        followerCount = wasFollowing ? followerCount + 1 : Math.max(0, followerCount - 1);
        updateFollowButtonUI();
        updateFollowerCountUI();
        if (listener != null) {
            listener.onFollowStateChanged(isFollowing, followerCount);
        }
    }

    private void updateFollowButtonUI() {
        if (isFollowing) {
            btnFollow.setText("Đang theo dõi");
            btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#535353")));
        } else {
            btnFollow.setText("Theo dõi");
            btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#1DB954")));
        }
    }

    private void updateFollowerCountUI() {
        if (tvFollowerCount != null) {
            String displayCount = followerCount >= 1000 ?
                    String.format(Locale.US, "%.1fK", followerCount / 1000f) :
                    String.valueOf(followerCount);
            tvFollowerCount.setText(displayCount + " follower(s)");
        }
    }
}