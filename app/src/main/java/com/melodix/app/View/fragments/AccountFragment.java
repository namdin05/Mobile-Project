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
import com.melodix.app.Utils.SessionManager;
import com.melodix.app.View.artist.ManageSongActivity;

public class AccountFragment extends Fragment {
    private AppRepository repository;
    private ImageView avatar;
    private TextView name, headline;
    private Switch dark;
    private LinearLayout cardArtistCenter, btnArtistUpload, btnArtistAlbums, btnArtistStats;

    private com.google.android.material.button.MaterialButton btnRequestArtist, btnLogOut;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private com.melodix.app.ViewModel.ProfileViewModel profileViewModel;

    // Tách biệt cài đặt App (Dark Mode) ra một két sắt riêng
    private static final String PREF_SETTINGS = "MelodixSettings";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private boolean isRequestPending = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        repository = AppRepository.getInstance(requireContext());

        avatar = view.findViewById(R.id.img_avatar);
        name = view.findViewById(R.id.tv_name);
        headline = view.findViewById(R.id.tv_headline);

        cardArtistCenter = view.findViewById(R.id.card_artist_center);
        btnArtistUpload = view.findViewById(R.id.btn_artist_upload);
        btnArtistAlbums = view.findViewById(R.id.btn_artist_albums);
        btnArtistStats = view.findViewById(R.id.btn_artist_stats);
        btnLogOut = view.findViewById(R.id.btn_logout);
        btnRequestArtist = view.findViewById(R.id.btn_request_artist);

        profileViewModel = new androidx.lifecycle.ViewModelProvider(this).get(com.melodix.app.ViewModel.ProfileViewModel.class);

        btnLogOut.setOnClickListener(v -> profileViewModel.performLogout());

        profileViewModel.getLogoutStatus().observe(getViewLifecycleOwner(), isLoggedOut -> {
            if (isLoggedOut != null && isLoggedOut) {
                Intent intent = new Intent(requireActivity(), com.melodix.app.View.auth.LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            }
        });

        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(android.graphics.Color.parseColor("#1DB954"));
            swipeRefresh.setOnRefreshListener(() -> {
                // ĐÃ SỬA: Gọi trực tiếp SessionManager
                String myId = SessionManager.getInstance(requireContext()).getUserId();
                if (myId != null) {
                    checkRequestStatus(myId);
                    syncProfileRoleSilently(myId);
                } else {
                    swipeRefresh.setRefreshing(false);
                }
            });
        }

        // 1. TÍNH NĂNG DARK MODE (Dùng file Setting riêng)
        SharedPreferences settingsPrefs = requireContext().getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE);
        dark = view.findViewById(R.id.switch_dark_mode);
        dark.setChecked(settingsPrefs.getBoolean(KEY_DARK_MODE, false));
        dark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsPrefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // 2. CÁC NÚT CƠ BẢN
        view.findViewById(R.id.btn_speed).setOnClickListener(v -> AppUiUtils.showSpeedDialog(requireContext()));
        view.findViewById(R.id.btn_sleep_timer).setOnClickListener(v -> AppUiUtils.showSleepTimerDialog(requireContext()));
        btnArtistUpload.setOnClickListener(v -> startActivity(new Intent(getContext(), ManageSongActivity.class)));
        btnArtistAlbums.setOnClickListener(v -> startActivity(new Intent(requireContext(), com.melodix.app.View.artist.ManageAlbumActivity.class)));

        btnArtistStats.setOnClickListener(v -> {
            // ĐÃ SỬA: Lấy ID từ SessionManager
            String userId = SessionManager.getInstance(requireContext()).getUserId();
            if (userId != null) {
                Intent intent = new Intent(requireContext(), com.melodix.app.View.artist.ArtistAnalyticsActivity.class);
                intent.putExtra(com.melodix.app.View.artist.ArtistAnalyticsActivity.EXTRA_ARTIST_ID, userId);
                startActivity(intent);
            }
        });

        view.findViewById(R.id.btn_share_profile).setOnClickListener(v -> {
            // ĐÃ SỬA: Lấy thông tin từ Session và MelodixPrefs
            String myId = SessionManager.getInstance(requireContext()).getUserId();
            SharedPreferences prefs = requireContext().getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
            String myName = prefs.getString("USER_NAME", "Người dùng");
            if (myId != null) com.melodix.app.Utils.ShareUtils.shareContent(requireContext(), "user", myId, myName);
        });

        btnRequestArtist.setOnClickListener(v -> {
            // ĐÃ SỬA: Gọi SessionManager
            String myId = SessionManager.getInstance(requireContext()).getUserId();
            if (myId == null) return;

            btnRequestArtist.setEnabled(false);

            if (isRequestPending) {
                cancelArtistRequest(myId);
            } else {
                sendArtistRequest(myId);
            }
        });

        // KIỂM TRA ĐĂNG NHẬP NGAY TỪ ĐẦU
        String myId = SessionManager.getInstance(requireContext()).getUserId();
        if (myId != null) {
            syncProfileRoleSilently(myId);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        SessionManager session = SessionManager.getInstance(requireContext());

        if (session.hasSession()) {
            String myId = session.getUserId();
            String role = session.getRole();

            // Name và Avatar thường được lưu ở bước khác nên ta vẫn đọc từ MelodixPrefs
            SharedPreferences prefs = requireContext().getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
            String displayName = session.getUserName();
            String avatarUrl = session.getUserAvatar();

            name.setText(displayName);
            headline.setText("Melodix Member");

            if (!avatarUrl.isEmpty()) {
                com.bumptech.glide.Glide.with(requireContext()).load(avatarUrl).circleCrop().into(avatar);
            }

            if ("artist".equalsIgnoreCase(role)) {
                cardArtistCenter.setVisibility(View.VISIBLE);
                btnRequestArtist.setVisibility(View.GONE);
            } else {
                cardArtistCenter.setVisibility(View.GONE);
                btnRequestArtist.setVisibility(View.VISIBLE);
                if (myId != null) checkRequestStatus(myId);
            }
            if (myId != null){
                syncProfileRoleSilently(myId);
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            String myId = SessionManager.getInstance(requireContext()).getUserId();
            if (myId != null) {
                syncProfileRoleSilently(myId);
            }
        }
    }

    // ========================================================
    // BỘ 3 HÀM XỬ LÝ LOGIC API GỬI / HỦY / KIỂM TRA
    // ========================================================

    private void checkRequestStatus(String userId) {
        com.melodix.app.Service.ProfileAPIService apiService = com.melodix.app.Service.RetrofitClient.getClient(getActivity().getApplication()).create(com.melodix.app.Service.ProfileAPIService.class);

        apiService.checkArtistRequestStatus("eq." + userId).enqueue(new retrofit2.Callback<java.util.List<Object>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<Object>> call, retrofit2.Response<java.util.List<Object>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    isRequestPending = !response.body().isEmpty();
                    updateRequestUI();
                }
            }
            @Override public void onFailure(retrofit2.Call<java.util.List<Object>> call, Throwable t) {}
        });
    }

    private void sendArtistRequest(String userId) {
        com.melodix.app.Service.ProfileAPIService apiService = com.melodix.app.Service.RetrofitClient.getClient(getActivity().getApplication()).create(com.melodix.app.Service.ProfileAPIService.class);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("user_id", userId);

        apiService.requestArtistRole(data).enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                if (isAdded()) {
                    btnRequestArtist.setEnabled(true);
                    if (response.isSuccessful()) {
                        isRequestPending = true;
                        updateRequestUI();
                        Toast.makeText(getContext(), "Đã gửi yêu cầu xét duyệt!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                if (isAdded()) { btnRequestArtist.setEnabled(true); }
            }
        });
    }

    private void cancelArtistRequest(String userId) {
        com.melodix.app.Service.ProfileAPIService apiService = com.melodix.app.Service.RetrofitClient.getClient(getActivity().getApplication()).create(com.melodix.app.Service.ProfileAPIService.class);

        apiService.cancelArtistRequest("eq." + userId).enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                if (isAdded()) {
                    btnRequestArtist.setEnabled(true);
                    if (response.isSuccessful()) {
                        isRequestPending = false;
                        updateRequestUI();
                        Toast.makeText(getContext(), "Đã hủy yêu cầu!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                if (isAdded()) { btnRequestArtist.setEnabled(true); }
            }
        });
    }

    private void updateRequestUI() {
        if (isRequestPending) {
            btnRequestArtist.setText("Cancel Pending Request ⏳");
            btnRequestArtist.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#FF9800")
                    )
            );
        } else {
            btnRequestArtist.setText("Become an Artist 🌟");
            btnRequestArtist.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#1DB954")
                    )
            );
        }
    }

    private void syncProfileRoleSilently(String userId) {
        com.melodix.app.Repository.ProfileRepository profileRepo = new com.melodix.app.Repository.ProfileRepository(getContext());
        profileRepo.getProfileById(userId, new retrofit2.Callback<java.util.List<com.melodix.app.Model.Profile>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.melodix.app.Model.Profile>> call, retrofit2.Response<java.util.List<com.melodix.app.Model.Profile>> response) {

                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                if (isAdded() && response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    String realRole = response.body().get(0).getRole();

                    if (realRole != null) {
                        // Cập nhật lại role mới nhất vào SessionManager thay vì tự mở SharedPreferences
                        SessionManager.getInstance(requireContext()).updateRole(realRole);

                        if ("artist".equalsIgnoreCase(realRole)) {
                            cardArtistCenter.setVisibility(View.VISIBLE);
                            btnRequestArtist.setVisibility(View.GONE);
                        } else {
                            cardArtistCenter.setVisibility(View.GONE);
                            btnRequestArtist.setVisibility(View.VISIBLE);
                            checkRequestStatus(userId);
                        }
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.melodix.app.Model.Profile>> call, Throwable t) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });
    }
}