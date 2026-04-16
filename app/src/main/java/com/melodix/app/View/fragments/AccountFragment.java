package com.melodix.app.View.fragments;

import android.content.Intent;
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
import androidx.fragment.app.Fragment;

import com.melodix.app.Model.AppUser;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.ResourceUtils;
import com.melodix.app.View.artist.ManageSongActivity;
import com.melodix.app.View.artist.UploadSongActivity;

public class AccountFragment extends Fragment {
    private AppRepository repository;
    private ImageView avatar;
    private TextView name;
    private TextView headline;
    private Switch dark;

    // FIX LỖI CRASH Ở ĐÂY: Đổi từ MaterialCardView thành LinearLayout (hoặc View)
    private LinearLayout cardArtistCenter;

    private LinearLayout btnArtistUpload, btnArtistAlbums, btnArtistStats;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        repository = AppRepository.getInstance(requireContext());

        avatar = view.findViewById(R.id.img_avatar);
        name = view.findViewById(R.id.tv_name);
        headline = view.findViewById(R.id.tv_headline);
        dark = view.findViewById(R.id.switch_dark_mode);

        // Ánh xạ Artist Center
        cardArtistCenter = view.findViewById(R.id.card_artist_center);
        btnArtistUpload = view.findViewById(R.id.btn_artist_upload);
        btnArtistAlbums = view.findViewById(R.id.btn_artist_albums);
        btnArtistStats = view.findViewById(R.id.btn_artist_stats);

        dark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                if (isChecked) {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
        });

        // Bắt sự kiện click cho các nút của Artist
        btnArtistUpload.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), ManageSongActivity.class));        });

        btnArtistAlbums.setOnClickListener(v -> {
            // Mở trang Tạo Album
            startActivity(new Intent(requireContext(), com.melodix.app.View.artist.ManageAlbumActivity.class));
        });

        btnArtistStats.setOnClickListener(v -> {
            // Lấy user hiện tại từ Session
            com.melodix.app.Model.Profile realUser = com.melodix.app.Model.SessionManager.getInstance(requireContext()).getCurrentUser();

            if (realUser != null && realUser.getId() != null) {
                // Mở màn hình Thống kê và truyền ID vào
                Intent intent = new Intent(requireContext(), com.melodix.app.View.artist.ArtistAnalyticsActivity.class);

                // ĐÃ FIX: Sửa Arti thành ArtistAnalyticsActivity
                intent.putExtra(com.melodix.app.View.artist.ArtistAnalyticsActivity.EXTRA_ARTIST_ID, realUser.getId());

                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "Lỗi: Không tìm thấy dữ liệu Nghệ sĩ", Toast.LENGTH_SHORT).show();
            }
        });

// Sự kiện click nút Chia sẻ Profile
        view.findViewById(R.id.btn_share_profile).setOnClickListener(v -> {
            // Lấy ID và Tên của user hiện tại từ SessionManager
            com.melodix.app.Model.Profile currentUser = com.melodix.app.Model.SessionManager.getInstance(requireContext()).getCurrentUser();

            if (currentUser != null && currentUser.getId() != null) {
                String myId = currentUser.getId();
                String myName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Người dùng";

                // Gọi "Máy in thiệp" từ ShareUtils
                com.melodix.app.Utils.ShareUtils.shareContent(requireContext(), "user", myId, myName);
            } else {
                android.widget.Toast.makeText(requireContext(), "Chưa tải xong dữ liệu tài khoản", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // 1. LẤY USER THẬT TỪ SESSION MANAGER (Không dùng AppRepository MockData nữa)
        com.melodix.app.Model.Profile realUser = com.melodix.app.Model.SessionManager.getInstance(requireContext()).getCurrentUser();

        if (realUser != null) {
            // 2. Cập nhật Tên
            name.setText(realUser.getDisplayName());
            headline.setText("Thành viên Melodix"); // Bảng Profile thật không có headline, mình gán chữ mặc định

            // 3. Load Avatar thật bằng Glide
            if (realUser.getAvatarUrl() != null && !realUser.getAvatarUrl().isEmpty()) {
                com.bumptech.glide.Glide.with(requireContext())
                        .load(realUser.getAvatarUrl())
                        .circleCrop()
                        .into(avatar);
            }

            // 4. KIỂM TRA ROLE VÀ HIỂN THỊ ARTIST CENTER
            String role = realUser.getRole();
            android.util.Log.d("ACCOUNT_ROLE", "Role hiện tại là: " + role); // In ra Logcat để kiểm tra

            if ("artist".equalsIgnoreCase(role)) {
                cardArtistCenter.setVisibility(View.VISIBLE);
            } else {
                cardArtistCenter.setVisibility(View.GONE);
            }
        }
    }

}