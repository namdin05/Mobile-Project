package com.melodix.app.View.dialogs;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.melodix.app.Model.Profile;
import com.melodix.app.R;
import com.melodix.app.Repository.ProfileRepository;
import com.melodix.app.Utils.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PrivacySettingsBottomSheet extends BottomSheetDialogFragment {

    private Switch switchPlaylists, switchRecentArtists;
    private ProgressBar progressBar; // Thêm ProgressBar vào layout nếu cần
    private ProfileRepository repository;
    private String userId;
    private boolean isLoaded = false; // Cờ kiểm tra đã load xong chưa

    public static PrivacySettingsBottomSheet newInstance() {
        return new PrivacySettingsBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_privacy_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new ProfileRepository(requireContext());
        userId = SessionManager.getInstance(requireContext()).getUserId();

        switchPlaylists = view.findViewById(R.id.switch_show_playlists);
        switchRecentArtists = view.findViewById(R.id.switch_show_recent_artists);

        // TẮT SWITCH NGAY LẬP TỨC ĐỂ TRÁNH HIỆN TRUE
        switchPlaylists.setEnabled(false);
        switchRecentArtists.setEnabled(false);

        // Load trạng thái mới nhất từ Supabase
        loadCurrentSettingsFromServer();

        view.findViewById(R.id.btn_save_settings).setOnClickListener(v -> saveSettings());
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
    }

    private void loadCurrentSettingsFromServer() {
        if (userId == null) {
            enableSwitches(true);
            return;
        }

        repository.getProfileById(userId, new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Profile profile = response.body().get(0);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (switchPlaylists != null) {
                            switchPlaylists.setChecked(profile.isShowPlaylists());
                        }
                        if (switchRecentArtists != null) {
                            switchRecentArtists.setChecked(profile.isShowRecentArtists());
                        }
                        isLoaded = true;
                        enableSwitches(true);
                    });
                } else {
                    enableSwitches(true);
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                Toast.makeText(requireContext(), "Không tải được cài đặt", Toast.LENGTH_SHORT).show();
                enableSwitches(true);
            }
        });
    }

    private void enableSwitches(boolean enable) {
        if (switchPlaylists != null) switchPlaylists.setEnabled(enable);
        if (switchRecentArtists != null) switchRecentArtists.setEnabled(enable);
    }

    private void saveSettings() {
        if (userId == null) {
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("show_playlists", switchPlaylists.isChecked());
        updates.put("show_recent_artists", switchRecentArtists.isChecked());

        repository.updatePrivacySettings(userId, updates);
        Toast.makeText(requireContext(), "Đã lưu cài đặt", Toast.LENGTH_SHORT).show();
        dismiss(); // Đóng ngay, không cần delay
    }
}