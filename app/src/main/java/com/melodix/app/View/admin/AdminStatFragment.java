package com.melodix.app.View.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.melodix.app.BuildConfig;
import com.melodix.app.Model.AppMetric;
import com.melodix.app.R;
import com.melodix.app.Service.AdminAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Utils.DateHelper;
import com.melodix.app.View.admin.dashboard.AlbumManagementFragment;
import com.melodix.app.View.admin.dashboard.GenreManagementFragment;
import com.melodix.app.View.admin.dashboard.SongManagementFragment;
import com.melodix.app.View.admin.dashboard.UserManagementFragment;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminStatFragment extends Fragment {
    TextView tvLastSync;
    TextView tvTotalUsers;
    TextView tvTotalListeners;
    TextView tvTotalArtists;
    TextView tvTotalAdmins;
    TextView tvTotalSongs;
    TextView tvTotalAlbums;
    TextView tvTotalGenres;
    TextView tvTotalPlaylists;

    public AdminStatFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_stat, container, false);

        // 1. Ánh xạ View cho các Card điều hướng
        MaterialCardView cardUsersBanner = view.findViewById(R.id.cardUsersBanner);
        MaterialCardView cardSongs = view.findViewById(R.id.cardSongs);
        MaterialCardView cardAlbums = view.findViewById(R.id.cardAlbums);
        MaterialCardView cardGenres = view.findViewById(R.id.cardGenres);
        MaterialCardView cardPlaylists = view.findViewById(R.id.cardPlaylists);

        // 2. BẮT BUỘC: Ánh xạ View cho các TextView hiển thị số liệu
        // (Lưu ý: Bạn cần kiểm tra lại file XML xem đã đặt đúng các id này chưa nhé)
        tvLastSync = view.findViewById(R.id.tvLastSync);
        tvTotalUsers = view.findViewById(R.id.tvTotalUsers);
        tvTotalListeners = view.findViewById(R.id.tvTotalListeners);
        tvTotalArtists = view.findViewById(R.id.tvTotalArtists);
        tvTotalAdmins = view.findViewById(R.id.tvTotalAdmins);
        tvTotalSongs = view.findViewById(R.id.tvTotalSongs);
        tvTotalAlbums = view.findViewById(R.id.tvTotalAlbums);
        tvTotalGenres = view.findViewById(R.id.tvTotalGenres);
        tvTotalPlaylists = view.findViewById(R.id.tvTotalPlaylists);

        // 3. Thiết lập sự kiện Click gọi hàm điều hướng
        // cardUsersBanner.setOnClickListener(v -> navigateToFragment(new UserManagementFragment()));
        cardSongs.setOnClickListener(v -> navigateToFragment(new SongManagementFragment()));
        cardAlbums.setOnClickListener(v -> navigateToFragment(new AlbumManagementFragment()));
        cardGenres.setOnClickListener(v -> navigateToFragment(new GenreManagementFragment()));
        // cardPlaylists.setOnClickListener(v -> navigateToFragment(new PlaylistManagementFragment()));

        // 4. Gọi API tải dữ liệu thống kê
        fetchAllData();

        return view;
    }

    private void navigateToFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void fetchAllData() {
        AdminAPIService apiService = RetrofitClient.getClient().create(AdminAPIService.class);
        String apiKey = BuildConfig.SERVICE_KEY;
        String authHeader = "Bearer " + BuildConfig.SERVICE_KEY;

        apiService.getAppMetrics(apiKey, authHeader).enqueue(new Callback<List<AppMetric>>() {
            @Override
            public void onResponse(Call<List<AppMetric>> call, Response<List<AppMetric>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AppMetric> metrics = response.body();




                    // 1. Khai báo biến tạm để lưu số liệu tính toán
                    int usersCount = 0;
                    int artistsCount = 0;
                    int listenersCount = 0;



                    // 2. Duyệt qua dữ liệu
                    for (AppMetric metric : metrics) {
                        String value = metric.getMetricValue();
                        String key = metric.getMetricKey();

                        if (key.equals("latest_update")) {
                            if (tvLastSync != null) {
                                String formattedDate = DateHelper.formatSupabaseDate(value);
                                tvLastSync.setText("Lastest update: " + formattedDate);
                            }
                            continue; // Xong ngày tháng thì nhảy sang dòng tiếp theo luôn
                        }

                        try {
                            switch (key) {
                                case "total_users":
                                    usersCount = Integer.parseInt(value);
                                    if(tvTotalUsers != null) tvTotalUsers.setText(value);
                                    break;
                                case "total_listeners":
                                    listenersCount = Integer.parseInt(value);
                                    if(tvTotalListeners != null) tvTotalListeners.setText(value);
                                    break;
                                case "total_artists":
                                    artistsCount = Integer.parseInt(value);
                                    if(tvTotalArtists != null) tvTotalArtists.setText(value);
                                    break;
                                case "total_songs":
                                    if(tvTotalSongs != null) tvTotalSongs.setText(value);
                                    break;
                                case "total_albums":
                                    if(tvTotalAlbums != null) tvTotalAlbums.setText(value);
                                    break;
                                case "total_genres":
                                    if(tvTotalGenres != null) tvTotalGenres.setText(value);
                                    break;
                                case "total_playlists":
                                    if(tvTotalPlaylists != null) tvTotalPlaylists.setText(value);
                            }
                        } catch (NumberFormatException e) {
                            Log.e("AdminStat", "Lỗi ép kiểu số tại key: " + key);
                        }
                    }

                    int totalAdmins = usersCount - artistsCount - listenersCount;

                    // Hiển thị lên TextView của Admin
                    if (tvTotalAdmins != null) {
                        tvTotalAdmins.setText(String.valueOf(totalAdmins));
                    }

                } else {
                    Log.e("AdminStat", "Lỗi lấy dữ liệu thống kê: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<AppMetric>> call, Throwable t) {
                Log.e("AdminStat", "Lỗi mạng: " + t.getMessage());
            }
        });
    }
}