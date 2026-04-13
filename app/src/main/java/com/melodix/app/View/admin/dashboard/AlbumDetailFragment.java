package com.melodix.app.View.admin.dashboard;

import static android.widget.Toast.LENGTH_SHORT;

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

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlbumDetailFragment extends Fragment {

    private String albumId = "";
    private String albumTitle = "";

    private RecyclerView rvAlbumSongs;
    private TextView tvDetailAlbumTitle;
    private SongAdapter songAdapter;
    private List<Song> songList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_album_detail, container, false);

        // 1. Lấy dữ liệu từ màn hình trước gửi sang
        if (getArguments() != null) {
            albumId = getArguments().getString("ALBUM_ID", "");
            albumTitle = getArguments().getString("ALBUM_TITLE", "Chi tiết Album");
        }

        // 2. ÁNH XẠ VIEW (Bắt buộc phải làm trước tiên)
        tvDetailAlbumTitle = view.findViewById(R.id.tvDetailAlbumTitle);
        rvAlbumSongs = view.findViewById(R.id.rvAlbumSongs); // Đưa lên đây!
        ImageButton btnBack = view.findViewById(R.id.btnBackToAlbums);

        tvDetailAlbumTitle.setText(albumTitle);
        songList = new ArrayList<>();

        // 3. SETUP RECYCLER VIEW (Sau khi đã có rvAlbumSongs)
        setupRecyclerView();

        // 4. Bắt sự kiện nút Back
        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // 5. Gọi API tải danh sách bài hát
        if (!albumId.isEmpty()) {
            fetchSongsInAlbum();
        } else {
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy ID Album", Toast.LENGTH_SHORT).show();
        }

        return view;
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

    private void fetchSongsInAlbum() {
        SongAPIService apiService = RetrofitClient.getClient().create(SongAPIService.class);
        String apiKey = BuildConfig.SERVICE_KEY;
        String authHeader = "Bearer " + BuildConfig.SERVICE_KEY;

        // Truyền bộ lọc eq.ID_CỦA_ALBUM
        String filter = "eq." + albumId;

        apiService.getSongsByAlbum(apiKey, authHeader, filter).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    songList.clear();
                    songList.addAll(response.body());
                    songAdapter.update(new ArrayList<>(songList));

                    if(songList.isEmpty()){
                        Toast.makeText(getContext(), "Album này chưa có bài hát nào", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("AdminAlbumDetail", "Lỗi tải bài hát: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                Log.e("AdminAlbumDetail", "Lỗi mạng: " + t.getMessage());
            }
        });
    }
}