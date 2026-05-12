package com.melodix.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Repository.PlaybackRepository;
import com.melodix.app.Service.AudioPlayerService;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.View.auth.LoginActivity;
import com.melodix.app.View.fragments.AccountFragment;
import com.melodix.app.View.fragments.LibraryFragment;
import com.melodix.app.View.fragments.SearchFragment;
import com.melodix.app.View.home.HomeFragment;
import com.melodix.app.Utils.NetworkUtils;

import com.melodix.app.ViewModel.ProfileViewModel;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private int currentTabId = R.id.nav_home;
    private AppRepository repository;

    private ProfileViewModel profileViewModel;

    
    private Fragment homeFragment;
    private Fragment searchFragment;
    private Fragment libraryFragment;
    private Fragment accountFragment;
    private Fragment activeFragment;

    
    private LinearLayout miniPlayer;
    private ImageView miniCover;
    private TextView miniTitle;
    private TextView miniSubtitle;
    private ImageButton miniPlayPause;

    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable miniPlayerWatcher = new Runnable() {
        @Override
        public void run() {
            if (miniPlayer != null && miniPlayer.getVisibility() == android.view.View.VISIBLE) {
                miniPlayPause.setImageResource(AudioPlayerService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
            }
            mainHandler.postDelayed(this, 500);
        }
    };

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d("MELODIX_FCM", "Người dùng đã cấp quyền thông báo!");
                } else {
                    Log.w("MELODIX_FCM", "Người dùng từ chối cấp quyền thông báo.");
                    Toast.makeText(this, "Bạn sẽ không nhận được thông báo bài hát mới!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        android.content.SharedPreferences prefs = getSharedPreferences("MelodixSettings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode_enabled", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(isDarkMode ?
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES :
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        
        askNotificationPermission();
        fetchAndSaveFCMToken();

        
        if (savedInstanceState == null) {
            
            homeFragment = new HomeFragment();
            searchFragment = new SearchFragment();
            libraryFragment = new LibraryFragment();
            accountFragment = new AccountFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment_container, homeFragment, "HOME")
                    .add(R.id.main_fragment_container, searchFragment, "SEARCH").hide(searchFragment)
                    .add(R.id.main_fragment_container, libraryFragment, "LIB").hide(libraryFragment)
                    .add(R.id.main_fragment_container, accountFragment, "ACC").hide(accountFragment)
                    .commit();

            activeFragment = homeFragment;
            currentTabId = R.id.nav_home;
        } else {
            
            homeFragment = getSupportFragmentManager().findFragmentByTag("HOME");
            searchFragment = getSupportFragmentManager().findFragmentByTag("SEARCH");
            libraryFragment = getSupportFragmentManager().findFragmentByTag("LIB");
            accountFragment = getSupportFragmentManager().findFragmentByTag("ACC");

            
            if (!homeFragment.isHidden()) activeFragment = homeFragment;
            else if (!searchFragment.isHidden()) activeFragment = searchFragment;
            else if (!libraryFragment.isHidden()) activeFragment = libraryFragment;
            else if (!accountFragment.isHidden()) activeFragment = accountFragment;

            currentTabId = savedInstanceState.getInt("ACTIVE_TAB", R.id.nav_home);
        }

        
        miniPlayer = findViewById(R.id.mini_player_root);
        if (miniPlayer != null) {
            miniCover = findViewById(R.id.mini_cover);
            miniTitle = findViewById(R.id.mini_title);
            miniSubtitle = findViewById(R.id.mini_subtitle);
            miniPlayPause = findViewById(R.id.mini_play_pause);

            miniPlayer.setOnClickListener(v -> {
                String currentSongId = AudioPlayerService.getCurrentSongId();
                if (currentSongId != null) {
                    PlaybackUtils.openPlayer(this, currentSongId);
                }
            });

            miniPlayPause.setOnClickListener(v -> {
                boolean isNowPlaying = !AudioPlayerService.isPlaying();
                miniPlayPause.setImageResource(isNowPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
                PlaybackUtils.sendAction(this, AudioPlayerService.ACTION_TOGGLE_PLAY);
            });
        }

        
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
        
        bottomNavigationView.setSelectedItemId(currentTabId);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                Fragment selectedFragment = null;
                int newTabId = menuItem.getItemId();
                if(newTabId == currentTabId) return false;

                boolean isMovingRight = (getTabIdx(newTabId) > getTabIdx(currentTabId));
                int enterAnim = isMovingRight ? R.anim.slide_in_right : R.anim.slide_in_left;
                int exitAnim = isMovingRight ? R.anim.slide_out_left : R.anim.slide_out_right;

                if(newTabId == R.id.nav_home) selectedFragment = homeFragment;
                else if(newTabId == R.id.nav_search) selectedFragment = searchFragment;
                else if(newTabId == R.id.nav_library) selectedFragment = libraryFragment;
                else if(newTabId == R.id.nav_account) selectedFragment = accountFragment;

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(enterAnim, exitAnim)
                            .hide(activeFragment)
                            .show(selectedFragment)
                            .commit();
                    currentTabId = newTabId;
                    activeFragment = selectedFragment;
                }
                return true;
            }

        });
        handleDeepLink(getIntent());
        checkNetworkAndSwitchTab();

        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        
        outState.putInt("ACTIVE_TAB", currentTabId);

    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); 
        handleDeepLink(intent);

        handleNotificationIntent(intent);
    }

    private int getTabIdx(int id){
        if (id == R.id.nav_home) return 1;
        if (id == R.id.nav_search) return 2;
        if (id == R.id.nav_library) return 3;
        if (id == R.id.nav_account) return 4;
        return 0;
    }

    private void updateMiniPlayer() {
        String currentSongId = AudioPlayerService.getCurrentSongId();
        if (currentSongId == null) {
            miniPlayer.setVisibility(android.view.View.GONE);
            return;
        }

        Song song = PlaybackRepository.getInstance().getCurrentSong();
        if (song == null) {
            miniPlayer.setVisibility(android.view.View.GONE);
            return;
        }

        miniPlayer.setVisibility(android.view.View.VISIBLE);

        if (song.getCoverUrl() != null && !song.getCoverUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(song.getCoverUrl())
                    .placeholder(R.drawable.ic_logo)
                    .error(R.drawable.ic_logo)
                    .into(miniCover);
        } else {
            miniCover.setImageResource(R.drawable.ic_logo);
        }

        miniTitle.setText(song.getTitle());
        miniSubtitle.setText(song.getArtistName());
        miniPlayPause.setImageResource(AudioPlayerService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMiniPlayer();
        }
    };

    private void checkNetworkAndSwitchTab() {
        boolean isOnline = NetworkUtils.isNetworkAvailable(this);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) return;

        if (!isOnline) {
            bottomNav.setSelectedItemId(R.id.nav_library);

            
            bottomNav.getMenu().findItem(R.id.nav_home).setEnabled(false);
            bottomNav.getMenu().findItem(R.id.nav_search).setEnabled(false);

        } else {
            bottomNav.getMenu().findItem(R.id.nav_home).setEnabled(true);
            bottomNav.getMenu().findItem(R.id.nav_search).setEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        androidx.core.content.ContextCompat.registerReceiver(this, stateReceiver,
                new android.content.IntentFilter(AudioPlayerService.ACTION_STATE_CHANGED),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);

        updateMiniPlayer();
        mainHandler.post(miniPlayerWatcher);
        checkNetworkAndSwitchTab();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(stateReceiver); } catch (Exception ignored) {}
        mainHandler.removeCallbacks(miniPlayerWatcher);
    }


    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void fetchAndSaveFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("MELODIX_FCM", "Lấy FCM Token thất bại", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d("MELODIX_FCM", "Đã lấy được FCM Token: " + token);

                    
                    if (profileViewModel != null) {
                        profileViewModel.updateTokenToServer(token);
                    }
                });
    }

    private void handleDeepLink(android.content.Intent intent) {
        if (intent != null && android.content.Intent.ACTION_VIEW.equals(intent.getAction())) {
            android.net.Uri data = intent.getData();

            
            if (data != null) {
                boolean isMyScheme = "melodix".equals(data.getScheme()) && "redirect".equals(data.getHost());
                boolean isMyWeb = "giabaocode.github.io".equals(data.getHost());

                if (isMyScheme || isMyWeb) {
                    String type = data.getQueryParameter("type");
                    String id = data.getQueryParameter("id");

                    if (type != null && id != null) {
                        android.content.Intent nextIntent = null;

                        switch (type.toLowerCase()) { 
                            case "user":
                                nextIntent = new android.content.Intent(this, com.melodix.app.View.profile.UserProfileActivity.class);
                                nextIntent.putExtra(com.melodix.app.View.profile.UserProfileActivity.EXTRA_USER_ID, id);
                                break;
                            case "playlist":
                                nextIntent = new android.content.Intent(this, com.melodix.app.View.PlaylistDetailActivity.class);
                                nextIntent.putExtra(com.melodix.app.View.PlaylistDetailActivity.EXTRA_PLAYLIST_ID, id);
                                break;
                            case "album":
                                nextIntent = new android.content.Intent(this, com.melodix.app.View.AlbumDetailActivity.class);
                                
                                nextIntent.putExtra("extra_album_id", id);
                                break;
                            case "profile":
                            case "artist": 
                                nextIntent = new android.content.Intent(this, com.melodix.app.View.ArtistDetailActivity.class);
                                nextIntent.putExtra("extra_artist_id", id);
                                break;
                            case "song":
                                nextIntent = new android.content.Intent(this, com.melodix.app.PlayerActivity.class);
                                nextIntent.putExtra(com.melodix.app.PlayerActivity.EXTRA_SONG_ID, id);
                                nextIntent.putExtra("start_playback", true);
                                break;
                        }

                        if (nextIntent != null) {
                            startActivity(nextIntent);
                        }
                    }
                }
            }
        }
    }

    private void handleNotificationIntent(android.content.Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            
            String action = intent.getStringExtra("action");
            String songId = intent.getStringExtra("song_id");

            if ("OPEN_SONG_DETAIL".equals(action) && songId != null) {
                Log.d("MELODIX_FCM", "Mở bài hát từ thông báo: " + songId);

                
                android.content.Intent nextIntent = new android.content.Intent(this, com.melodix.app.PlayerActivity.class);
                nextIntent.putExtra(com.melodix.app.PlayerActivity.EXTRA_SONG_ID, songId);
                nextIntent.putExtra("start_playback", true);
                startActivity(nextIntent);

                
                intent.removeExtra("action");
                intent.removeExtra("song_id");
            } else if ("ROLE_UPGRADED".equals(action)) {
                
                Log.d("MELODIX_FCM", "Người dùng được duyệt làm Artist!");

                
                com.melodix.app.Utils.SessionManager.getInstance(this).updateRole("artist");

                
                Toast.makeText(this, "Chúc mừng! Bạn đã chính thức trở thành Nghệ sĩ trên Melodix.", Toast.LENGTH_LONG).show();

                
                

                
                intent.removeExtra("action");
            } else if ("OPEN_ALBUM_DETAIL".equals(action)) {
                
                String albumId = intent.getStringExtra("album_id");

                if (albumId != null) {
                    Log.d("MELODIX_FCM", "Mở Album từ thông báo: " + albumId);

                    android.content.Intent nextIntent = new android.content.Intent(this, com.melodix.app.View.AlbumDetailActivity.class);
                    
                    nextIntent.putExtra("extra_album_id", albumId);
                    startActivity(nextIntent);

                    
                    intent.removeExtra("action");
                    intent.removeExtra("album_id");
                }
            }
        }
    }
}


