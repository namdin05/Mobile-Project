package com.melodix.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.melodix.app.R;
import com.melodix.app.View.admin.AdminArtistFragment;
import com.melodix.app.View.admin.AdminRequestFragment;
import com.melodix.app.View.admin.AdminSongFragment;
import com.melodix.app.View.admin.AdminStatFragment;

public class AdminActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        bottomNav = findViewById(R.id.bottomNav);

        // 1. Mặc định khi vừa mở lên, cho load màn hình Request trước
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminRequestFragment())
                    .commit();
            bottomNav.setSelectedItemId(R.id.nav_requests); // Highlight sáng nút Request
        }

        // 2. Lắng nghe sự kiện người dùng bấm vào thanh điều hướng
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // Kiểm tra xem user bấm vào nút nào
            if (itemId == R.id.nav_dashboard) { // Nút Stat
                selectedFragment = new AdminStatFragment();
            } else if (itemId == R.id.nav_songs) { // Nút Song
                selectedFragment = new AdminSongFragment();
            } else if (itemId == R.id.nav_artists) { // Nút Artist
                selectedFragment = new AdminArtistFragment();
            } else if (itemId == R.id.nav_requests) { // Nút Request
                selectedFragment = new AdminRequestFragment();
            }

            // 3. Nếu có Fragment được chọn, tiến hành thay thế (replace) vào container
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true; // Trả về true để thanh Nav chuyển màu icon
            }

            return false;
        });
    }
}
