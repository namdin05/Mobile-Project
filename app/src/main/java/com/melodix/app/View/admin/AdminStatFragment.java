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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.melodix.app.Model.AppMetric;
import com.melodix.app.Model.ArtistRequest;
import com.melodix.app.R;
import com.melodix.app.Utils.DateHelper;
import com.melodix.app.View.admin.dashboard.AlbumManagementFragment;
import com.melodix.app.View.admin.dashboard.GenreManagementFragment;
import com.melodix.app.View.admin.dashboard.SongManagementFragment;
import com.melodix.app.View.admin.dashboard.UserManagementFragment;
import com.melodix.app.ViewModel.admin.AdminStatViewModel;

import java.util.List;

public class AdminStatFragment extends Fragment {
    private TextView tvLastSync, tvTotalUsers, tvTotalListeners, tvTotalArtists, tvTotalAdmins,
            tvTotalSongs, tvTotalAlbums, tvTotalGenres, tvTotalPlaylists;

    private MaterialCardView badgeArtistReqs;
    private TextView tvPendingArtistReqs;
    private MaterialCardView cardArtistRequests;

    private List<ArtistRequest> currentPendingList;
    private BottomSheetDialog currentDialog;

    private AdminStatViewModel viewModel;

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
        //MaterialCardView cardPlaylists = view.findViewById(R.id.cardPlaylists);



        // 2. Ánh xạ View cho các TextView hiển thị số liệu
        tvLastSync = view.findViewById(R.id.tvLastSync);
        tvTotalUsers = view.findViewById(R.id.tvTotalUsers);
        tvTotalListeners = view.findViewById(R.id.tvTotalListeners);
        tvTotalArtists = view.findViewById(R.id.tvTotalArtists);
        tvTotalAdmins = view.findViewById(R.id.tvTotalAdmins);
        tvTotalSongs = view.findViewById(R.id.tvTotalSongs);
        tvTotalAlbums = view.findViewById(R.id.tvTotalAlbums);
        tvTotalGenres = view.findViewById(R.id.tvTotalGenres);
        //tvTotalPlaylists = view.findViewById(R.id.tvTotalPlaylists);

        // 3. Thiết lập sự kiện Click gọi hàm điều hướng
        cardUsersBanner.setOnClickListener(v -> navigateToFragment(new UserManagementFragment()));
        cardSongs.setOnClickListener(v -> navigateToFragment(new SongManagementFragment()));
        cardAlbums.setOnClickListener(v -> navigateToFragment(new AlbumManagementFragment()));
        cardGenres.setOnClickListener(v -> navigateToFragment(new GenreManagementFragment()));
        // cardPlaylists.setOnClickListener(v -> navigateToFragment(new PlaylistManagementFragment()));

        // 4. Khởi tạo ViewModel và quan sát dữ liệu
        viewModel = new ViewModelProvider(this).get(AdminStatViewModel.class);
        mappingData();

        setupArtistRequestCard(view);
        observeViewModel();

        return view;
    }

    private void navigateToFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void mappingData() {
        viewModel.getAllAppMetrics().observe(getViewLifecycleOwner(), appMetrics -> {
            if (appMetrics == null || appMetrics.isEmpty()) return;

            int usersCount = 0;
            int artistsCount = 0;
            int listenersCount = 0;

            for (AppMetric metric : appMetrics) {
                String value = metric.getMetricValue();
                String key = metric.getMetricKey();

                if (key == null || value == null) continue;

                if (key.equals("latest_update")) {
                    if (tvLastSync != null) {
                        String formattedDate = DateHelper.formatSupabaseDate(value);
                        tvLastSync.setText("Latest update: " + formattedDate);
                    }
                    continue;
                }

                try {
                    switch (key) {
                        case "total_users":
                            usersCount = Integer.parseInt(value);
                            if (tvTotalUsers != null) tvTotalUsers.setText(value);
                            break;
                        case "total_listeners":
                            listenersCount = Integer.parseInt(value);
                            if (tvTotalListeners != null) tvTotalListeners.setText(value);
                            break;
                        case "total_artists":
                            artistsCount = Integer.parseInt(value);
                            if (tvTotalArtists != null) tvTotalArtists.setText(value);
                            break;
                        case "total_songs":
                            if (tvTotalSongs != null) tvTotalSongs.setText(value);
                            break;
                        case "total_albums":
                            if (tvTotalAlbums != null) tvTotalAlbums.setText(value);
                            break;
                        case "total_genres":
                            if (tvTotalGenres != null) tvTotalGenres.setText(value);
                            break;
                        case "total_playlists":
                            if (tvTotalPlaylists != null) tvTotalPlaylists.setText(value);
                            break;
                    }
                } catch (NumberFormatException e) {
                    Log.e("ADMIN_STAT", "Lỗi ép kiểu số tại key: " + key + " với value: " + value);
                }
            }

            int totalAdmins = usersCount - artistsCount - listenersCount;
            if (tvTotalAdmins != null) {
                tvTotalAdmins.setText(String.valueOf(totalAdmins));
            }
        });
    }

    private void observeViewModel() {
        // Lắng nghe danh sách trả về để hiển thị Badge số lượng
        viewModel.getPendingArtistRequests().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                currentPendingList = list;
                int count = list.size();

                if (count > 0) {
                    badgeArtistReqs.setVisibility(View.VISIBLE);
                    tvPendingArtistReqs.setText(count + " New");
                } else {
                    badgeArtistReqs.setVisibility(View.GONE);
                    // Đóng hộp thoại nếu đang mở mà hết người
                    if (currentDialog != null && currentDialog.isShowing()) {
                        currentDialog.dismiss();
                    }
                }
            }
        });

        // Lắng nghe kết quả bấm Duyệt/Từ chối
        viewModel.getActionSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess != null && isSuccess) {
                // Nếu thành công -> Load lại danh sách, giao diện sẽ tự động cập nhật
                viewModel.getPendingArtistRequests();
            }
        });

        // Lắng nghe để hiện Toast
        viewModel.getActionMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupArtistRequestCard(View view) {
        badgeArtistReqs = view.findViewById(R.id.badgeArtistReqs);
        tvPendingArtistReqs = view.findViewById(R.id.tvPendingArtistReqs);
        MaterialCardView cardArtistRequests = view.findViewById(R.id.cardArtistRequests);

        badgeArtistReqs.setVisibility(View.GONE);

        cardArtistRequests.setOnClickListener(v -> {
            if (currentPendingList != null && !currentPendingList.isEmpty()) {
                showReviewBottomSheet();
            } else {
                Toast.makeText(getContext(), "Không có yêu cầu nào mới!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showReviewBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());

        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(60, 60, 60, 60);

        android.graphics.drawable.GradientDrawable bgShape = new android.graphics.drawable.GradientDrawable();
        bgShape.setColor(android.graphics.Color.WHITE);
        bgShape.setCornerRadii(new float[]{60, 60, 60, 60, 0, 0, 0, 0});
        container.setBackground(bgShape);

        TextView title = new TextView(requireContext());
        title.setText("Request (" + currentPendingList.size() + ")");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(android.graphics.Color.BLACK);
        title.setPadding(0, 0, 0, 40);
        container.addView(title);

        // Hiển thị từng yêu cầu
        for (ArtistRequest req : currentPendingList) {
            // TẠO MỘT CÁI "HỘP" ĐỂ CHỨA RIÊNG THÔNG TIN CỦA 1 NGƯỜI NÀY
            android.widget.LinearLayout itemContainer = new android.widget.LinearLayout(requireContext());
            itemContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
            itemContainer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

            String userName = req.getUserProfile() != null ? req.getUserProfile().getDisplayName() : "User " + req.getUserId().substring(0, 6);

            TextView tvName = new TextView(requireContext());
            tvName.setText("👤 " + userName);
            tvName.setTextSize(16);
            tvName.setTextColor(android.graphics.Color.DKGRAY);
            tvName.setPadding(0, 0, 0, 20);
            itemContainer.addView(tvName); // Thêm vào hộp nhỏ

            android.widget.LinearLayout btnLayout = new android.widget.LinearLayout(requireContext());
            btnLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);

            // Nút Approve
            com.google.android.material.button.MaterialButton btnApprove = new com.google.android.material.button.MaterialButton(requireContext());
            btnApprove.setText("Approve");
            btnApprove.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1DB954")));
            android.widget.LinearLayout.LayoutParams paramsApprove = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            paramsApprove.setMargins(0, 0, 20, 0);
            btnApprove.setLayoutParams(paramsApprove);

            // Nút Reject
            com.google.android.material.button.MaterialButton btnReject = new com.google.android.material.button.MaterialButton(requireContext());
            btnReject.setText("Reject");
            btnReject.setTextColor(android.graphics.Color.RED);
            btnReject.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            btnReject.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.RED));
            btnReject.setStrokeWidth(3);
            btnReject.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            // ========================================================
            // BẮT SỰ KIỆN: Xóa luôn cái "hộp" (itemContainer) khỏi màn hình
            // ========================================================
            btnApprove.setOnClickListener(v -> {
                viewModel.processRequest(req, "approved");
                currentPendingList.remove(req);
                container.removeView(itemContainer); // Xóa ngay lập tức trên UI

                updateDialogState(title, dialog);    // Cập nhật số lượng
            });

            btnReject.setOnClickListener(v -> {
                viewModel.processRequest(req, "rejected");
                currentPendingList.remove(req);
                container.removeView(itemContainer); // Xóa ngay lập tức trên UI

                updateDialogState(title, dialog);    // Cập nhật số lượng
            });

            btnLayout.addView(btnApprove);
            btnLayout.addView(btnReject);
            itemContainer.addView(btnLayout); // Thêm 2 nút vào hộp nhỏ

            View divider = new View(requireContext());
            divider.setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"));
            android.widget.LinearLayout.LayoutParams divParams = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 2);
            divParams.setMargins(0, 40, 0, 40);
            divider.setLayoutParams(divParams);
            itemContainer.addView(divider); // Thêm đường kẻ vào hộp nhỏ

            // CUỐI CÙNG: Nhét cái hộp nhỏ này vào container lớn của Dialog
            container.addView(itemContainer);
        }

        dialog.setContentView(container);
        ((View) container.getParent()).setBackgroundColor(android.graphics.Color.TRANSPARENT);
        dialog.show();

        currentDialog = dialog;
    }

    // Hàm phụ trợ giúp cập nhật cả Dialog lẫn Badge ngoài màn hình chính
    private void updateDialogState(TextView title, BottomSheetDialog dialog) {
        int remainingCount = currentPendingList.size();

        if (remainingCount == 0) {
            // Nếu đã duyệt hết: Đóng dialog và Ẩn luôn cái Badge đỏ ở ngoài
            dialog.dismiss();
            if (badgeArtistReqs != null) {
                badgeArtistReqs.setVisibility(View.GONE);
            }
        } else {
            // Nếu vẫn còn người: Cập nhật tiêu đề Dialog
            title.setText("Request (" + remainingCount + ")");

            // ĐỒNG THỜI: Cập nhật luôn con số "2 New" ở màn hình ngoài
            if (tvPendingArtistReqs != null) {
                tvPendingArtistReqs.setText(remainingCount + " New");
            }
        }
    }


}
