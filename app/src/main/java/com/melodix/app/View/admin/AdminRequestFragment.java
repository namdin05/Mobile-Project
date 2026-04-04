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
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.melodix.app.Adapter.SongRequestAdapter;
import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Song;
import com.melodix.app.Model.StatusUpdateRequest;
import com.melodix.app.R;

// Lưu ý: Nhớ import đúng các class Service và API của bạn nhé
import com.melodix.app.Service.AdminAPIService;
import com.melodix.app.Service.RetrofitClient;
//import com.melodix.app.Service.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRequestFragment extends Fragment {

    private RecyclerView rvSongRequests;
    private SongRequestAdapter adapter;
    private List<Song> requestList;

    // Các biến cho Trình phát nhạc
    private ExoPlayer exoPlayer;
    private String currentSongTitle = "";
    private MaterialCardView cardPlayer;
    private TextView tvPlayerStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ View
        rvSongRequests = view.findViewById(R.id.rvSongRequests);
        cardPlayer = view.findViewById(R.id.cardPlayer);
        tvPlayerStatus = view.findViewById(R.id.tvPlayerStatus);

        // 2. Cấu hình RecyclerView
        rvSongRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        requestList = new ArrayList<>();

        // 3. Khởi tạo Trình phát nhạc
        setupExoPlayer();

        // 4. Khởi tạo Adapter và bắt sự kiện click
        adapter = new SongRequestAdapter(requestList, new SongRequestAdapter.OnItemActionListener() {
            @Override
            public void onApproveClick(Song request, int position) {
                // Gọi API đổi thành 'approved'
                updateStatusOnServer(request, "approved", position);
            }

            @Override
            public void onRejectClick(Song request, int position) {
                // Gọi API đổi thành 'rejected'
                updateStatusOnServer(request, "rejected", position);
            }

            @Override
            public void onPlayClick(Song request) {
                if (request.getAudioUrl() != null && !request.getAudioUrl().isEmpty()) {
                    playAudio(request.getAudioUrl(), request.getTitle());
                } else {
                    Toast.makeText(getContext(), "Bài hát này không có link Audio!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        rvSongRequests.setAdapter(adapter);

        // 5. GỌI API LẤY DỮ LIỆU TỪ SUPABASE
        fetchPendingRequests();
    }

    // =========================================================================
    // HÀM GỬI LỆNH DUYỆT / TỪ CHỐI LÊN SUPABASE
    // =========================================================================
    private void updateStatusOnServer(Song request, String newStatus, int position) {
        AdminAPIService apiService = RetrofitClient.getClient().create(AdminAPIService.class);
        String token = "Bearer " + BuildConfig.SERVICE_KEY;

        StatusUpdateRequest body = new StatusUpdateRequest(newStatus);
        String idFilter = "eq." + request.getId();

        apiService.updateRequestStatus(BuildConfig.SERVICE_KEY, token, idFilter, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Supabase thường trả về mã 200 (OK) hoặc 204 (No Content) khi Update thành công
                if (response.isSuccessful()) {

                    // Lấy Tên bài hát để hiện thông báo
                    String songName = request.getTitle();
                    String msg = newStatus.equals("approved") ? "Đã duyệt bài: " : "Đã từ chối bài: ";

                    // Hiện thông báo (Toast)
                    Toast.makeText(requireContext(), msg + songName, Toast.LENGTH_SHORT).show();

                    // Xóa bài hát khỏi UI
                    removeAndStop(position);

                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Lỗi không xác định";
                        Log.e("MELODIX_ADMIN", "Lỗi Cập nhật: " + errorMsg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(requireContext(), "Lỗi từ máy chủ!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MELODIX_ADMIN", "Lỗi mạng: " + t.getMessage());
                Toast.makeText(requireContext(), "Không có kết nối mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================================
    // HÀM GỌI API (RETROFIT)
    // =========================================================================
    private void fetchPendingRequests() {
        // 1. Tạo service từ RetrofitClient vừa viết
        AdminAPIService apiService = RetrofitClient.getClient().create(AdminAPIService.class);

        // 2. Lấy Key bảo mật
        String apiKey = BuildConfig.API_KEY;

        // (Tạm thời dùng chính API_KEY làm Token khách để test lấy danh sách cho nhanh.
        // Sau này bạn làm chức năng lưu SharedPreferences thì thay bằng Token của Admin nhé).
        String token = "Bearer " + BuildConfig.API_KEY;

        // 3. Thực hiện cuộc gọi
        apiService.getPendingRequests(apiKey, token).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    requestList.clear();
                    requestList.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (requestList.isEmpty()) {
                        Toast.makeText(getContext(), "Không có bài hát nào chờ duyệt!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("MELODIX_ADMIN", "Lỗi API: " + response.code());
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                Log.e("MELODIX_ADMIN", "Lỗi mạng: " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================================
    // HỆ THỐNG PHÁT NHẠC (MEDIA PLAYER)
    // =========================================================================
    private void setupExoPlayer() {
        exoPlayer = new ExoPlayer.Builder(requireContext()).build();

        // Lắng nghe các trạng thái của ExoPlayer để đổi chữ trên màn hình
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    // Đang tải dữ liệu mạng
                    tvPlayerStatus.setText("Đang tải cực nhanh: " + currentSongTitle + "...");
                } else if (playbackState == Player.STATE_READY) {
                    // Đã tải xong một đoạn, bắt đầu phát
                    tvPlayerStatus.setText("🎵 Đang nghe thử: " + currentSongTitle);
                } else if (playbackState == Player.STATE_ENDED) {
                    // Hết bài
                    tvPlayerStatus.setText("Đã nghe xong: " + currentSongTitle);
                }
            }
        });
    }

    private void playAudio(String audioUrl, String songTitle) {
        currentSongTitle = songTitle;

        // Hiện cái Bottom Player lên
        cardPlayer.setVisibility(View.VISIBLE);
        tvPlayerStatus.setText("Đang kết nối...");

        // Dừng bài cũ nếu đang phát
        if (exoPlayer.isPlaying()) {
            exoPlayer.stop();
        }

        // Nạp link nhạc vào MediaItem (Cách làm chuẩn của Media3)
        MediaItem mediaItem = MediaItem.fromUri(audioUrl);
        exoPlayer.setMediaItem(mediaItem);

        // Ra lệnh chuẩn bị và tự động phát ngay khi có đủ dữ liệu (không cần đợi tải hết bài)
        exoPlayer.prepare();
        exoPlayer.play();
    }

    private void removeAndStop(int position) {
        // Kiểm tra an toàn xem vị trí có hợp lệ không
        if (position >= 0 && position < requestList.size()) {
            // 1. Xóa dữ liệu khỏi danh sách List
            requestList.remove(position);

            // 2. Báo cho Adapter hiệu ứng biến mất
            adapter.notifyItemRemoved(position);

            // 3. CỰC KỲ QUAN TRỌNG: Cập nhật lại vị trí của các bài hát bên dưới
            adapter.notifyItemRangeChanged(position, requestList.size());

            // 4. Tắt nhạc nếu đang phát đúng bài đó
            if (exoPlayer != null && exoPlayer.isPlaying()) {
                exoPlayer.stop();
                exoPlayer.clearMediaItems();
                tvPlayerStatus.setText("Đã hoàn tất duyệt.");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Giải phóng ExoPlayer
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}