package com.melodix.app.View.admin.dashboard;

import static android.widget.Toast.LENGTH_SHORT;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Service.SongAPIService;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.Utils.SongActionHelper;
import com.melodix.app.View.adapters.SongAdapter;
import com.melodix.app.ViewModel.AlbumViewModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlbumDetailFragment extends Fragment {

    private String albumId = "";
    private String albumTitle = "";
    private String albumCover = "";
    private String albumStatus = "";

    private RecyclerView rvAlbumSongs;
    private TextView tvDetailAlbumTitle, tvAlbumName;
    private android.widget.ImageView imgAlbumCover;
    private android.widget.LinearLayout layoutActionButtons;
    private com.google.android.material.button.MaterialButton btnApprove, btnReject, btnHide;;

    private SongAdapter songAdapter;
    private List<Song> songList;
    private AlbumViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_album_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            albumId = getArguments().getString("ALBUM_ID", "");
            albumTitle = getArguments().getString("ALBUM_TITLE", "Chi tiết");
            albumCover = getArguments().getString("ALBUM_COVER", "");
            albumStatus = getArguments().getString("ALBUM_STATUS", "");
        }

        // 2. ÁNH XẠ VIEW (Bắt buộc phải làm trước tiên)
        tvAlbumName = view.findViewById(R.id.tvAlbumName);
        imgAlbumCover = view.findViewById(R.id.imgAlbumCover);
        layoutActionButtons = view.findViewById(R.id.layoutActionButtons);
        btnApprove = view.findViewById(R.id.btnApprove);
        btnReject = view.findViewById(R.id.btnReject);
        btnHide = view.findViewById(R.id.btnHide);
        rvAlbumSongs = view.findViewById(R.id.rvAlbumSongs);

        layoutActionButtons.setVisibility(View.VISIBLE);

        updateUIBasedOnStatus(albumStatus);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            // Quay lại Fragment trước đó trong BackStack
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().onBackPressed();
            }
        });

        tvAlbumName.setText(albumTitle);
        com.bumptech.glide.Glide.with(requireContext()).load(albumCover).into(imgAlbumCover);

        // XỬ LÝ NÚT APPROVE / REJECT
        btnApprove.setOnClickListener(v -> viewModel.updateAlbumStatus(albumId, "approved"));
        btnReject.setOnClickListener(v -> viewModel.updateAlbumStatus(albumId, "rejected"));

        if ("hide".equalsIgnoreCase(albumStatus)) {
            btnHide.setOnClickListener(v -> viewModel.updateAlbumStatus(albumId, "approved"));
        } else {
            btnHide.setOnClickListener(v -> viewModel.updateAlbumStatus(albumId, "hide"));
        }

        // 3. SETUP RECYCLER VIEW (Sau khi đã có rvAlbumSongs)
        songList = new ArrayList<>();
        setupRecyclerView();

        if (!albumId.isEmpty()) {
            viewModel = new ViewModelProvider(this).get(AlbumViewModel.class);
            viewModel.getSongsByAlbumId(albumId).observe(getViewLifecycleOwner(), songs -> {
                if (songs != null) {
                    songList.clear();
                    songList.addAll(songs);
                    songAdapter.update(new ArrayList<>(songList));
                    if(songList.isEmpty()){
                        Toast.makeText(getContext(), "Album này chưa có bài hát nào", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            viewModel.getUpdateStatusResult().observe(getViewLifecycleOwner(), result -> {
                if (result != null) {
                    if (result.equals("error")) {
                        Toast.makeText(requireContext(), "Lỗi cập nhật trạng thái Album!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Nếu thành công, result chính là chữ "approved" hoặc "rejected"
                        Toast.makeText(requireContext(), "Cập nhật trạng thái Album thành công!", Toast.LENGTH_SHORT).show();

                        // Cập nhật lại biến cục bộ và gọi hàm vẽ lại giao diện (để ẩn nút đi)
                        albumStatus = result;
                        updateUIBasedOnStatus(albumStatus);
                    }
                }
            });
        } else {
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy ID Album", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        // Khởi tạo Adapter
        songAdapter = new SongAdapter(requireContext(), new ArrayList<>(), new SongAdapter.OnSongActionListener() {
            @Override
            public void onSongClick(Song song, int position) {
                // Truyền songList để PlaybackUtils lấy đúng danh sách đang lọc làm Queue
                SongActionHelper.playSongAndSetQueue(requireContext(), song, songList);
            }

            @Override
            public void onMenuClick(Song song, int position, String action) {
                SongActionHelper.handleMenuClick(requireContext(), song, action, songList);
            }
        });

        // Gắn vào RecyclerView
        rvAlbumSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAlbumSongs.setAdapter(songAdapter);
    }

    private void updateUIBasedOnStatus(String status) {
        if ("pending".equalsIgnoreCase(status)) {
            // Nếu đang chờ duyệt -> Hiện Approve/Reject, Ẩn HIDE
            btnApprove.setVisibility(View.VISIBLE);
            btnReject.setVisibility(View.VISIBLE);
            btnHide.setVisibility(View.GONE);

        } else if ("hide".equalsIgnoreCase(status) || "rejected".equalsIgnoreCase(status)) {
            btnHide.setText("SHOW");
            btnHide.setBackgroundColor(getResources().getColor(R.color.mdx_main_color));
            btnApprove.setVisibility(View.GONE);
            btnReject.setVisibility(View.GONE);
            btnHide.setVisibility(View.VISIBLE);
        }


        else {
            // Nếu đã duyệt (hoặc bất kỳ trạng thái nào khác) -> Hiện HIDE, Ẩn Approve/Reject
            btnHide.setText("HIDE");
            btnHide.setBackgroundColor(Color.parseColor("#F44336"));
            btnApprove.setVisibility(View.GONE);
            btnReject.setVisibility(View.GONE);
            btnHide.setVisibility(View.VISIBLE);
        }
    }
}