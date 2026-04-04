package com.melodix.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.melodix.app.View.artist.ArtistMusicFragment; // Import file vừa tạo
import com.melodix.app.View.artist.ArtistStatFragment;
import com.melodix.app.View.artist.ArtistProfileFragment;
public class ArtistActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);

        bottomNav = findViewById(R.id.bottomNavArtist);

        // Mở khóa: Mặc định load màn hình Quản lý bài hát
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_artist, new ArtistMusicFragment())
                    .commit();
            bottomNav.setSelectedItemId(R.id.nav_artist_music);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_artist_music) {
                selectedFragment = new ArtistMusicFragment(); // Mở khóa
            } else if (itemId == R.id.nav_artist_stat) {
                selectedFragment = new ArtistStatFragment();
            } else if (itemId == R.id.nav_artist_profile) {
                selectedFragment = new ArtistProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_artist, selectedFragment)
                        .commit();
                return true; // Trả về true thì thanh nav mới sáng đèn!
            }
            return false;
        });
    }
}