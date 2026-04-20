package com.melodix.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
import com.melodix.app.View.fragments.AccountFragment;
import com.melodix.app.View.fragments.LibraryFragment;
import com.melodix.app.View.fragments.SearchFragment;
import com.melodix.app.View.home.HomeFragment;

// THÊM IMPORT NÀY
import com.melodix.app.ViewModel.ProfileViewModel;

public class MainActivity extends AppCompatActivity {
    private int currentTabId = R.id.nav_home;
    private AppRepository repository;

    // Khai báo ProfileViewModel
    private ProfileViewModel profileViewModel;

    // Tạo sẵn các Fragment
    final Fragment homeFragment = new HomeFragment();
    final Fragment searchFragment = new SearchFragment();
    final Fragment libraryFragment = new LibraryFragment();
    final Fragment accountFragment = new AccountFragment();

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

    Fragment activeFragment = homeFragment;

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. KHỞI TẠO VIEWMODEL ĐẦU TIÊN (Rất quan trọng để tránh lỗi Null)
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // 2. SAU ĐÓ MỚI GỌI CÁC HÀM LIÊN QUAN ĐẾN NÓ
        askNotificationPermission();
        fetchAndSaveFCMToken();

        repository = AppRepository.getInstance(this);

        if(savedInstanceState == null){
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment_container, homeFragment)
                    .add(R.id.main_fragment_container, searchFragment).hide(searchFragment)
                    .add(R.id.main_fragment_container, libraryFragment).hide(libraryFragment)
                    .add(R.id.main_fragment_container, accountFragment).hide(accountFragment)
                    .commit();
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
        } else {
            Log.e("TEST_MINI", "LỖI TRẦM TRỌNG: KHÔNG TÌM THẤY MINI PLAYER TRONG XML!");
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnItemSelectedListener(menuItem -> {
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

            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(enterAnim, exitAnim)
                    .hide(activeFragment).show(selectedFragment).commit();

            currentTabId = newTabId;
            activeFragment = selectedFragment;
            return true;
        });
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

    @Override
    protected void onResume() {
        super.onResume();
        androidx.core.content.ContextCompat.registerReceiver(this, stateReceiver,
                new android.content.IntentFilter(AudioPlayerService.ACTION_STATE_CHANGED),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);

        updateMiniPlayer();
        mainHandler.post(miniPlayerWatcher);
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
                    if (!task.isSuccessful()) {
                        Log.w("MELODIX_FCM", "Lấy FCM Token thất bại", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d("MELODIX_FCM", "Đã lấy được FCM Token: " + token);

                    // Gọi ViewModel để cập nhật Token (ViewModel sẽ gọi xuống Repo)
                    if (profileViewModel != null) {
                        profileViewModel.updateTokenToServer(token);
                    }
                });
    }
}