package com.melodix.app.View.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;

import com.melodix.app.R;
import com.melodix.app.View.auth.LoginActivity;
import com.melodix.app.View.profile.AdminProfileActivity;
import com.melodix.app.ViewModel.ProfileViewModel;

public class AdminActivity extends AppCompatActivity {

    private TextView tvAdminName;
    private TextView tvAppTitle;
    private ImageView imgProfile;
    private DrawerLayout drawerLayout;

    private ProfileViewModel viewModel;

    // Lưu tạm state để truyền sang Intent Profile
    private String currentAdminName = "";
    private String currentAdminAvatarUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        initViews();
        setupNavigation();

        // 1. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // 2. Quan sát (Observe) dữ liệu Profile
        viewModel.getProfile().observe(this, profile -> {
            if (profile != null) {
                currentAdminName = profile.getDisplayName();
                currentAdminAvatarUrl = profile.getAvatarUrl();

                tvAdminName.setText(currentAdminName);

                if (currentAdminAvatarUrl != null && !currentAdminAvatarUrl.isEmpty()) {
                    Glide.with(this)
                            .load(currentAdminAvatarUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(imgProfile);
                } else {
                    imgProfile.setImageResource(R.drawable.ic_person);


                }
            } else {
                tvAdminName.setText("Chưa đăng nhập");
                imgProfile.setImageResource(R.drawable.ic_person);
                Log.e("ADMIN_ACTIVITY", "LỖI CMNR" );
            }
        });

        viewModel.getLogoutStatus().observe(this, isLoggedOut -> {
            if (isLoggedOut != null && isLoggedOut) {
                // 1. Tạo Intent về lại màn hình Login
                Intent intent = new Intent(AdminActivity.this, LoginActivity.class);

                // 2. CHỐT CHẶN BẢO MẬT: Xóa sạch lịch sử màn hình cũ
                // Điều này giúp người dùng khi về Login mà bấm nút Back trên điện thoại
                // thì app sẽ thoát ra ngoài Home chứ không bị chui ngược lại vào Admin.
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                // 3. Chuyển màn hình
                startActivity(intent);
                finish();
            }
        });

        // 4. Mặc định mở màn hình Stat (Dashboard) khi vừa vào app
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminStatFragment())
                    .commit();
        }

        setupBackPressedDispatcher();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ra lệnh cho ViewModel đi lấy data
        if (viewModel != null) {
            viewModel.loadProfileInfo();
        }
    }

    private void initViews() {
        tvAdminName = findViewById(R.id.tvAdminName);
        imgProfile = findViewById(R.id.imgProfile);
        drawerLayout = findViewById(R.id.drawer_layout);
        tvAppTitle = findViewById(R.id.tvAppTitle);

        imgProfile.setOnClickListener(this::showProfileMenu);
    }

    private void setupNavigation() {
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        NavigationView navView = findViewById(R.id.nav_view);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        tvAppTitle.setText("DASHBOARD");

        navView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.nav_dashboard) {
                selectedFragment = new AdminStatFragment();
                tvAppTitle.setText("DASHBOARD");
            } else if (itemId == R.id.nav_log) {
                selectedFragment = new AdminLogFragment();
                tvAppTitle.setText("LOG");
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupBackPressedDispatcher() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    this.setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
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
                // Thay vì tự xóa biến, chỉ cần ra lệnh cho ViewModel
                viewModel.performLogout();
                return true;
            }
            return false;
        });
        popup.show();
    }
}