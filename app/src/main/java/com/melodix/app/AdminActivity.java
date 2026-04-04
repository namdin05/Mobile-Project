package com.melodix.app; // Nếu package của bạn chứa chữ View thì nhớ đổi lại nhé

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.melodix.app.Model.Profile;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.View.admin.AdminGenreFragment;
import com.melodix.app.View.admin.AdminUserFragment;
import com.melodix.app.View.admin.AdminRequestFragment;
import com.melodix.app.View.admin.AdminSongFragment;
import com.melodix.app.View.admin.AdminStatFragment;
import com.melodix.app.View.auth.LoginActivity;
import com.melodix.app.View.profile.AdminProfileActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private TextView tvAdminName;
    private ImageView imgProfile;

    // Hai biến này dùng để lưu tạm dữ liệu, lát nữa truyền sang trang Edit Profile
    private String currentAdminName = "";
    private String currentAdminAvatarUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Ánh xạ View
        tvAdminName = findViewById(R.id.tvAdminName);
        imgProfile = findViewById(R.id.imgProfile);
        bottomNav = findViewById(R.id.bottom_navigation);

        // 1. Lắng nghe sự kiện bấm vào Avatar để mở Menu
        imgProfile.setOnClickListener(v -> showProfileMenu(v));

        // 2. Mặc định mở màn hình Stat (Dashboard) khi vừa vào app
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminStatFragment())
                    .commit();
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }

        // 3. Lắng nghe sự kiện thanh điều hướng Bottom Navigation
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                selectedFragment = new AdminStatFragment();
            } else if (itemId == R.id.nav_genres) {
                selectedFragment = new AdminGenreFragment();
            } else if (itemId == R.id.nav_songs) {
                selectedFragment = new AdminSongFragment();
            } else if (itemId == R.id.nav_artists) {
                selectedFragment = new AdminUserFragment();
            } else if (itemId == R.id.nav_requests) {
                selectedFragment = new AdminRequestFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mỗi khi màn hình Admin hiện lên mặt tiền, tự động lấy lại dữ liệu mới nhất!
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

        // Dùng Service Role Key để không bị kẹt RLS Policy
        String apiKey = BuildConfig.SERVICE_KEY;
        String token = "Bearer " + BuildConfig.SERVICE_KEY;
        String idFilter = "eq." + adminUid;

        apiService.getProfileById(apiKey, token, idFilter).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Profile adminProfile = response.body().get(0);

                    // Lưu dữ liệu vào biến toàn cục
                    currentAdminName = adminProfile.getDisplayName();
                    currentAdminAvatarUrl = adminProfile.getAvatarUrl();

                    // Đổ tên lên UI
                    tvAdminName.setText(currentAdminName);

                    // Đổ ảnh lên UI bằng Glide
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

    // Hàm hiển thị Dropdown Menu khi bấm vào Avatar
    private void showProfileMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.admin_info_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_profile) {
                // Mở màn hình Edit Profile và truyền dữ liệu hiện tại sang đó
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

    // Hàm xử lý Đăng xuất
    private void performLogout() {
        // Xóa toàn bộ dữ liệu trong SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Chuyển về màn hình Login
        // (Chú ý: Đổi tên LoginActivity cho khớp với class Đăng nhập của bạn)
        Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

    }
}