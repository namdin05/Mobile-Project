package com.melodix.app.View.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.AppUiUtils;
import com.melodix.app.View.artist.ManageSongActivity;

public class AccountFragment extends Fragment {
    private AppRepository repository;
    private ImageView avatar;
    private TextView name;
    private TextView headline;
    private Switch dark;
    private LinearLayout cardArtistCenter;
    private LinearLayout btnArtistUpload, btnArtistAlbums, btnArtistStats;

    // Khai báo SharedPreferences để lưu cấu hình Dark Mode
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "MelodixSettings";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        repository = AppRepository.getInstance(requireContext());
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        avatar = view.findViewById(R.id.img_avatar);
        name = view.findViewById(R.id.tv_name);
        headline = view.findViewById(R.id.tv_headline);

        cardArtistCenter = view.findViewById(R.id.card_artist_center);
        btnArtistUpload = view.findViewById(R.id.btn_artist_upload);
        btnArtistAlbums = view.findViewById(R.id.btn_artist_albums);
        btnArtistStats = view.findViewById(R.id.btn_artist_stats);

        // ==========================================
        // 1. TÍNH NĂNG DARK MODE
        // ==========================================
        dark = view.findViewById(R.id.switch_dark_mode);

        // Đọc trạng thái lưu trước đó để hiển thị công tắc cho đúng
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        dark.setChecked(isDarkMode);

        dark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Lưu lựa chọn vào bộ nhớ thiết bị
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_DARK_MODE, isChecked);
            editor.apply();

            // Áp dụng theme ngay lập tức
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // ==========================================
        // 2. TÍNH NĂNG TỐC ĐỘ (SPEED) & HẸN GIỜ (SLEEP TIMER)
        // ==========================================
        view.findViewById(R.id.btn_speed).setOnClickListener(v -> {
            AppUiUtils.showSpeedDialog(requireContext());
        });

        view.findViewById(R.id.btn_sleep_timer).setOnClickListener(v -> {
            AppUiUtils.showSleepTimerDialog(requireContext());
        });

        // Các nút của Artist Center
        btnArtistUpload.setOnClickListener(v -> startActivity(new Intent(getContext(), ManageSongActivity.class)));
        btnArtistAlbums.setOnClickListener(v -> startActivity(new Intent(requireContext(), com.melodix.app.View.artist.CreateAlbumActivity.class)));

        btnArtistStats.setOnClickListener(v -> {
            // ĐÃ SỬA: Lấy USER_ID từ SharedPreferences MelodixPrefs
            SharedPreferences prefs = requireContext().getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
            String userId = prefs.getString("USER_ID", null);

            if (userId != null) {
                Intent intent = new Intent(requireContext(), com.melodix.app.View.artist.ArtistAnalyticsActivity.class);
                intent.putExtra(com.melodix.app.View.artist.ArtistAnalyticsActivity.EXTRA_ARTIST_ID, userId);
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "Lỗi: Không tìm thấy dữ liệu Nghệ sĩ", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btn_share_profile).setOnClickListener(v -> {
            // ĐÃ SỬA: Lấy ID và Tên của user hiện tại từ SharedPreferences
            SharedPreferences prefs = requireContext().getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
            String myId = prefs.getString("USER_ID", null);
            String myName = prefs.getString("USER_NAME", "Người dùng");

            if (myId != null) {
                // Gọi "Máy in thiệp" từ ShareUtils
                com.melodix.app.Utils.ShareUtils.shareContent(requireContext(), "user", myId, myName);
            } else {
                Toast.makeText(requireContext(), "Chưa tải xong dữ liệu tài khoản", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // ĐÃ SỬA: Lấy toàn bộ thông tin User từ SharedPreferences để hiển thị
        SharedPreferences prefs = requireContext().getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false);

        if (isLoggedIn) {
            String displayName = prefs.getString("USER_NAME", "Người dùng");
            String avatarUrl = prefs.getString("USER_AVATAR", "");
            String role = prefs.getString("USER_ROLE", "user");

            name.setText(displayName);
            headline.setText("Thành viên Melodix");

            if (!avatarUrl.isEmpty()) {
                com.bumptech.glide.Glide.with(requireContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .into(avatar);
            }

            // Kiểm tra phân quyền: Nếu là "artist" thì hiện khung công cụ Artist Center
            if ("artist".equalsIgnoreCase(role)) {
                cardArtistCenter.setVisibility(View.VISIBLE);
            } else {
                cardArtistCenter.setVisibility(View.GONE);
            }
        }
    }
}