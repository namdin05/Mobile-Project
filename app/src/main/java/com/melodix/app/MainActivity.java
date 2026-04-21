package com.melodix.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Repository.PlaybackRepository;
import com.melodix.app.Service.AudioPlayerService;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.View.fragments.AccountFragment;
import com.melodix.app.View.fragments.LibraryFragment;
import com.melodix.app.View.fragments.SearchFragment;
import com.melodix.app.View.home.HomeFragment;
import com.melodix.app.Utils.NetworkUtils;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private int currentTabId = R.id.nav_home;
    private AppRepository repository;

    // Quản lý Fragment
    private Fragment homeFragment;
    private Fragment searchFragment;
    private Fragment libraryFragment;
    private Fragment accountFragment;
    private Fragment activeFragment;

    // Quản lý Mini Player
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
        // --- 1. CHỐT DARK MODE TRƯỚC KHI VẼ MÀN HÌNH ---
        android.content.SharedPreferences prefs = getSharedPreferences("MelodixSettings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode_enabled", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(isDarkMode ?
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES :
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = AppRepository.getInstance(this);

        askNotificationPermission();
        fetchAndSaveFCMToken();

        // --- 2. XỬ LÝ FRAGMENT CHỐNG LỖI DARK MODE ---
        if (savedInstanceState == null) {
            // Lần MỞ APP ĐẦU TIÊN: Tạo mới toàn bộ
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
            // LẦN KHỞI ĐỘNG LẠI (Do đổi Dark Mode/Xoay màn hình): Khôi phục bộ nhớ cũ
            homeFragment = getSupportFragmentManager().findFragmentByTag("HOME");
            searchFragment = getSupportFragmentManager().findFragmentByTag("SEARCH");
            libraryFragment = getSupportFragmentManager().findFragmentByTag("LIB");
            accountFragment = getSupportFragmentManager().findFragmentByTag("ACC");

            // Tìm xem thằng nào đang mở thì gán nó vào active
            if (!homeFragment.isHidden()) activeFragment = homeFragment;
            else if (!searchFragment.isHidden()) activeFragment = searchFragment;
            else if (!libraryFragment.isHidden()) activeFragment = libraryFragment;
            else if (!accountFragment.isHidden()) activeFragment = accountFragment;

            currentTabId = savedInstanceState.getInt("ACTIVE_TAB", R.id.nav_home);
        }

        // --- 3. KHỞI TẠO MINI PLAYER ---
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

        // --- 4. BẮT SỰ KIỆN CLICK TAB ---
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
        // Đồng bộ lại tab đang chọn lỡ như bị Restart
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
    }

    // ==========================================
    // LƯU TRẠNG THÁI TRƯỚC KHI BỊ "GIẾT" BỞI DARK MODE
    // ==========================================
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Cất id của tab hiện tại vào balo
        outState.putInt("ACTIVE_TAB", currentTabId);

    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        handleDeepLink(intent);
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

            // Tắt tab Home và Search để tránh load dữ liệu
            bottomNav.getMenu().findItem(R.id.nav_home).setEnabled(false);
            bottomNav.getMenu().findItem(R.id.nav_search).setEnabled(false);

            Toast.makeText(this, "Không có kết nối mạng.\nChỉ có thể nghe nhạc đã tải về.", Toast.LENGTH_LONG).show();
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

    // ==========================================
    // LOGIC XỬ LÝ PUSH NOTIFICATION (FIREBASE)
    // ==========================================
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
                    if (!task.isSuccessful()) return;
                    String token = task.getResult();
                    updateTokenToServer(token);
                });
    }

    private void updateTokenToServer(String token) {
        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "");
        if (userId.isEmpty()) return;

        ProfileAPIService apiService = RetrofitClient.getClient().create(ProfileAPIService.class);
        Map<String, Object> body = new HashMap<>();
        body.put("fcm_token", token);

        apiService.updateFcmToken(BuildConfig.SERVICE_KEY, "Bearer " + BuildConfig.SERVICE_KEY, "eq." + userId, body)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {}
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {}
                });
    }

    // Hàm Lễ tân phân loại link
// Lễ tân phân loại link
    private void handleDeepLink(android.content.Intent intent) {
        if (intent != null && android.content.Intent.ACTION_VIEW.equals(intent.getAction())) {
            android.net.Uri data = intent.getData();

            // 👇 SỬA ĐÚNG DÒNG NÀY ĐỂ BẮT CẢ 2 ĐƯỜNG 👇
            if (data != null && ("giabaocode.github.io".equals(data.getHost()) || "redirect".equals(data.getHost()))) {

                String type = data.getQueryParameter("type");
                String id = data.getQueryParameter("id");

                if (type != null && id != null) {
                    android.content.Intent nextIntent = null;

                    switch (type) {
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
                            nextIntent.putExtra(com.melodix.app.View.AlbumDetailActivity.EXTRA_ALBUM_ID, id);
                            break;
                        case "profile": // Dành cho Nghệ sĩ
                            nextIntent = new android.content.Intent(this, com.melodix.app.View.ArtistDetailActivity.class);
                            nextIntent.putExtra(com.melodix.app.View.ArtistDetailActivity.EXTRA_ARTIST_ID, id);
                            break;
                        case "song":
                            nextIntent = new android.content.Intent(this, com.melodix.app.PlayerActivity.class);
                            nextIntent.putExtra(com.melodix.app.PlayerActivity.EXTRA_SONG_ID, id);
                            // Có thể cần thêm cờ để tự động Play luôn
                            nextIntent.putExtra("start_playback", true);
                            break;
                    }

                    if (nextIntent != null) {
                        startActivity(nextIntent);
                    }
                }
            }
        }
    }}

