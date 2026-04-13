package com.melodix.app.View.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;

import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Profile;
import com.melodix.app.R;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.View.admin.dashboard.GenreManagementFragment;
import com.melodix.app.View.auth.LoginActivity;
import com.melodix.app.View.profile.AdminProfileActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends AppCompatActivity {

    private TextView tvAdminName;

    private TextView tvAppTitle;
    private ImageView imgProfile;
    private DrawerLayout drawerLayout;

    private String currentAdminName = "";
    private String currentAdminAvatarUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        tvAdminName = findViewById(R.id.tvAdminName);
        imgProfile = findViewById(R.id.imgProfile);
        drawerLayout = findViewById(R.id.drawer_layout);
        tvAppTitle = findViewById(R.id.tvAppTitle);

        ImageButton btnMenu = findViewById(R.id.btnMenu);
        NavigationView navView = findViewById(R.id.nav_view);

        // 1. Mở menu trượt khi bấm nút Hamburger
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        tvAppTitle.setText("Tổng quan");

        // 2. Lắng nghe sự kiện chọn item trên Menu trượt
        navView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.nav_dashboard) {
                selectedFragment = new AdminStatFragment();
                tvAppTitle.setText("Tổng quan");

//            } else if (itemId == R.id.nav_users) {
//                selectedFragment = new AdminUserFragment();
//                tvAppTitle.setText("Người dùng");
//
//            } else if (itemId == R.id.nav_songs) {
//                selectedFragment = new AdminSongFragment();
//                tvAppTitle.setText("Bài hát");
//            }
//            // Cập nhật thêm các Fragment khác của bạn nếu có
//            else if (itemId == R.id.nav_genres) {
//                 selectedFragment = new GenreManagementFragment();
//                 tvAppTitle.setText("Thể loại");
            }
            // else if (itemId == R.id.nav_album) {
            //     selectedFragment = new AdminAlbumFragment();
            // } else if (itemId == R.id.nav_verify) {
            //     selectedFragment = new AdminRequestFragment();
            // }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // 3. Lắng nghe sự kiện bấm vào Avatar để mở Menu Profile
        imgProfile.setOnClickListener(this::showProfileMenu);

        // 4. Mặc định mở màn hình Stat (Dashboard) khi vừa vào app
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminStatFragment())
                    .commit();
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Nếu Menu trượt đang mở -> Đóng menu
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    // Nếu menu đang đóng -> Tạm thời tắt bộ lắng nghe này và gọi back mặc định (thoát app hoặc về trang trước)
                    this.setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAdminInfo();
    }

    private void fetchAdminInfo() {
        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", MODE_PRIVATE);
        String adminUid = prefs.getString("USER_ID", "");

        if (adminUid.isEmpty()) {
            tvAdminName.setText("Chưa đăng nhập");
            return;
        }

        ProfileAPIService apiService = RetrofitClient.getClient().create(ProfileAPIService.class);

        String apiKey = BuildConfig.SERVICE_KEY;
        String token = "Bearer " + BuildConfig.SERVICE_KEY;
        String idFilter = "eq." + adminUid;

        apiService.getProfileById(apiKey, token, idFilter).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Profile adminProfile = response.body().get(0);

                    currentAdminName = adminProfile.getDisplayName();
                    currentAdminAvatarUrl = adminProfile.getAvatarUrl();

                    tvAdminName.setText(currentAdminName);

                    if (currentAdminAvatarUrl != null && !currentAdminAvatarUrl.isEmpty()) {
                        Glide.with(AdminActivity.this)
                                .load(currentAdminAvatarUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(imgProfile);
                    } else {
                        imgProfile.setImageResource(R.drawable.ic_person);
                    }
                } else {
                    Log.e("MELODIX_ADMIN", "Không tìm thấy thông tin Admin");
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                Log.e("MELODIX_ADMIN", "Lỗi mạng: " + t.getMessage());
            }
        });
    }

    private void showProfileMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.admin_info_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_profile) {
                Intent intent = new Intent(AdminActivity.this, AdminProfileActivity.class);
                intent.putExtra("CURRENT_NAME", currentAdminName);
                intent.putExtra("CURRENT_AVATAR", currentAdminAvatarUrl);
                startActivity(intent);
                return true;
            } else if (item.getItemId() == R.id.action_logout) {
                performLogout();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void performLogout() {
        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}