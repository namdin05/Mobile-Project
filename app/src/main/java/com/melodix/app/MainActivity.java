package com.melodix.app;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.ThemeUtils;
import com.melodix.app.View.fragments.AccountFragment;
import com.melodix.app.View.fragments.HomeFragment;
import com.melodix.app.View.fragments.LibraryFragment;
import com.melodix.app.View.fragments.SearchFragment;

public class MainActivity extends AppCompatActivity {
    private AppRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        repository = AppRepository.getInstance(this);
        ThemeUtils.applyNightMode(repository.getCurrentUser() == null || repository.getCurrentUser().darkMode);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Đã tạm ẩn các view của Mini Player để tránh lỗi vì không còn hàm getSongById

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                // Nếu HomeFragment lỗi, bạn có thể comment dòng dưới lại
                openFragment(new HomeFragment());
                return true;
            } else if (item.getItemId() == R.id.nav_search) {
                openFragment(new SearchFragment());
                return true;
            } else if (item.getItemId() == R.id.nav_library) {
                openFragment(new LibraryFragment());
                return true;
            } else if (item.getItemId() == R.id.nav_account) {
                openFragment(new AccountFragment());
                return true;
            }
            return false;
        });

        // Đổi màn hình mặc định khi mở app thành màn hình Search để bạn dễ test
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_search);
        }
    }

    private void openFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }
}


// String selectQuery = "id, title, audio_url, song_artists(profiles(display_name))";