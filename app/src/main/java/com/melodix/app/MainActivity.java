package com.melodix.app;

import static android.widget.Toast.LENGTH_SHORT;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.View.fragments.AccountFragment;
import com.melodix.app.View.fragments.LibraryFragment;
import com.melodix.app.View.fragments.SearchFragment;
import com.melodix.app.View.home.AllGenresFragment;
import com.melodix.app.View.home.HomeFragment;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private int currentTabId = R.id.nav_home;
    // tao het cac fragment cung luc ngay khi vao app de cac fragment ko bi load lai moi khi chuyen tab
    final Fragment homeFragment = new HomeFragment();
    final Fragment searchFragment = new SearchFragment();

    final Fragment libraryFragment = new LibraryFragment();
    final Fragment accountFragment = new AccountFragment();

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
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        askNotificationPermission();

        fetchAndSaveFCMToken();


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

    // ==========================================
    // LOGIC XỬ LÝ PUSH NOTIFICATION (FIREBASE)
    // ==========================================

    private void askNotificationPermission() {
        // Chỉ Android 13 (TIRAMISU) trở lên mới cần xin quyền này
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Đã có quyền
            } else {
                // Hiển thị popup xin quyền
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

                    // Lấy Token thành công
                    String token = task.getResult();
                    Log.d("MELODIX_FCM", "Đã lấy được FCM Token: " + token);

                    // Cập nhật lên Supabase
                    updateTokenToServer(token);
                });
    }

    private void updateTokenToServer(String token) {
        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "");

        if (userId.isEmpty()) {
            Log.w("MELODIX_FCM", "Chưa đăng nhập, không lưu Token");
            return;
        }

        ProfileAPIService apiService = RetrofitClient.getClient().create(ProfileAPIService.class);

        Map<String, Object> body = new HashMap<>();
        body.put("fcm_token", token);

        // Dùng Service Key như cách bạn đã làm ở AdminActivity
        String apiKey = BuildConfig.SERVICE_KEY;
        String authHeader = "Bearer " + BuildConfig.SERVICE_KEY;

        apiService.updateFcmToken(apiKey, authHeader, "eq." + userId, body)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d("MELODIX_FCM", "Đã lưu Token lên Supabase thành công!");
                        } else {
                            try {
                                // Ép đọc lỗi chi tiết từ Supabase
                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Trống";
                                // Bắt Retrofit khai ra cái link nó vừa gọi
                                String urlCalled = response.raw().request().url().toString();

                                Log.e("MELODIX_FCM", "Lưu Token thất bại, mã lỗi: " + response.code());
                                Log.e("MELODIX_FCM", "Chi tiết lỗi từ Server: " + errorBody);
                                Log.e("MELODIX_FCM", "URL đã gọi: " + urlCalled);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("MELODIX_FCM", "Lỗi mạng khi lưu Token: " + t.getMessage());
                    }
                });
    }
}