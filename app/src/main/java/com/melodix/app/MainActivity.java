package com.melodix.app;

import static android.widget.Toast.LENGTH_SHORT;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.melodix.app.View.fragments.AccountFragment;
import com.melodix.app.View.fragments.LibraryFragment;
import com.melodix.app.View.fragments.SearchFragment;
import com.melodix.app.View.home.AllGenresFragment;
import com.melodix.app.View.home.HomeFragment;

public class MainActivity extends AppCompatActivity {
    private int currentTabId = R.id.nav_home;
    // tao het cac fragment cung luc ngay khi vao app de cac fragment ko bi load lai moi khi chuyen tab
    final Fragment homeFragment = new HomeFragment();
    final Fragment searchFragment = new SearchFragment();

    final Fragment libraryFragment = new LibraryFragment();
    final Fragment accountFragment = new AccountFragment();

    Fragment activeFragment = homeFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
        if(savedInstanceState == null){
            // 1 lan tao het cac fragment, an nhung fragment k xai di
            getSupportFragmentManager().beginTransaction().add(R.id.main_fragment_container, homeFragment)
                    .add(R.id.main_fragment_container, searchFragment).hide(searchFragment)
                    .add(R.id.main_fragment_container, libraryFragment).hide(libraryFragment)
                    .add(R.id.main_fragment_container, accountFragment).hide(accountFragment)
                    .commit();
        }
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                Fragment selectedFragment = null;
                int newTabId = menuItem.getItemId();
                if(newTabId == currentTabId) return false;
                boolean isMovingRight = (getTabIdx(newTabId) > getTabIdx(currentTabId));
                int enterAnim = isMovingRight ? R.anim.slide_in_right : R.anim.slide_in_left;
                int exitAnim = isMovingRight ? R.anim.slide_out_left : R.anim.slide_out_right;
                if(newTabId == R.id.nav_home){
                    selectedFragment = homeFragment;
                } else if(newTabId == R.id.nav_search){
                    selectedFragment = searchFragment;
                } else if(newTabId == R.id.nav_library){
                    selectedFragment = libraryFragment;
                } else if(newTabId == R.id.nav_account){
                    selectedFragment = accountFragment;
                }
                getSupportFragmentManager().beginTransaction().setCustomAnimations(enterAnim, exitAnim)
                        .hide(activeFragment).show(selectedFragment).commit();
                currentTabId = newTabId;
                activeFragment = selectedFragment;
                return true;
            }
        });

    }
    private int getTabIdx(int id){
        if (id == R.id.nav_home) return 1;
        if (id == R.id.nav_search) return 2;
        if (id == R.id.nav_library) return 3;
        if (id == R.id.nav_account) return 4;
        return 0;
    }
}


// String selectQuery = "id, title, audio_url, song_artists(profiles(display_name))";