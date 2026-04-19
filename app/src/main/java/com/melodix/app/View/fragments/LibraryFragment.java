package com.melodix.app.View.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.Model.AppDatabase;
import com.melodix.app.Model.DownloadedSong;
import com.melodix.app.R;
import com.melodix.app.Repository.PlaylistRepository;
import com.melodix.app.View.adapters.PlaylistAdapter;
import com.melodix.app.View.adapters.DownloadedSongAdapter;
import com.melodix.app.View.dialogs.CreatePlaylistDialog;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryFragment extends Fragment {
    public interface OnPlaylistCreatedListener {
        void onPlaylistCreated();
    }

    private OnPlaylistCreatedListener playlistCreatedListener;

    public void setOnPlaylistCreatedListener(OnPlaylistCreatedListener listener) {
        this.playlistCreatedListener = listener;
    }
    private RecyclerView rvPlaylists;
    private PlaylistAdapter playlistAdapter;
    private List<Playlist> playlistList = new ArrayList<>();
    private Button btnCreatePlaylist;
    private RecyclerView rvDownloaded;
    private DownloadedSongAdapter downloadedSongAdapter;
    private PlaylistRepository playlistRepository;

    // Launcher chọn ảnh cho Create Playlist Dialog
    private ActivityResultLauncher<String> imagePickerLauncher;
    private CreatePlaylistDialog currentDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        playlistRepository = new PlaylistRepository(requireContext());

        rvPlaylists = view.findViewById(R.id.rv_playlists);
        btnCreatePlaylist = view.findViewById(R.id.btn_create_playlist);

        rvPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));
        playlistAdapter = new PlaylistAdapter(requireContext(), playlistList, this::onPlaylistClick);
        rvPlaylists.setAdapter(playlistAdapter);

        rvDownloaded = view.findViewById(R.id.rv_downloaded);
        rvDownloaded.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Tạo adapter cho danh sách bài hát đã tải
        downloadedSongAdapter = new DownloadedSongAdapter(requireContext());
        rvDownloaded.setAdapter(downloadedSongAdapter);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && currentDialog != null) {
                        currentDialog.setSelectedCoverUri(uri);
                    }
                });

        if (btnCreatePlaylist != null) {
            btnCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
        }

        loadUserPlaylists();
        loadDownloadedSongs();

        return view;
    }


    private void showCreatePlaylistDialog() {
        currentDialog = new CreatePlaylistDialog(requireContext(), playlist -> {
            // Thêm ngay vào danh sách và thông báo adapter
            playlistList.add(0, playlist);
            playlistAdapter.notifyItemInserted(0);
            rvPlaylists.scrollToPosition(0);

            // Reload từ server sau 1 giây để đồng bộ
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                loadUserPlaylists();
            }, 1000);

            if (playlistCreatedListener != null) {
                playlistCreatedListener.onPlaylistCreated();
            }
        }, imagePickerLauncher);

        currentDialog.show();
    }

    private void loadDownloadedSongs() {
        AppDatabase.getInstance(requireContext())
                .downloadedSongDao()
                .getAllDownloaded()
                .observe(getViewLifecycleOwner(), downloadedSongs -> {
                    if (downloadedSongAdapter != null) {
                        downloadedSongAdapter.updateList(downloadedSongs);
                    }

                    // Ẩn/hiện phần Downloaded nếu không có bài nào
                    if (rvDownloaded != null) {
                        rvDownloaded.setVisibility(downloadedSongs.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                });
    }

    public void loadUserPlaylists() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        playlistRepository.getUserPlaylists(userId, new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    playlistList.clear();
                    playlistList.addAll(response.body());

                    // Sắp xếp playlist mới nhất lên đầu (dùng ID vì ID mới thường lớn hơn)
                    playlistList.sort((p1, p2) -> p2.id.compareTo(p1.id));

                    Log.d("LIBRARY_DEBUG", "Đã load " + playlistList.size() + " playlist từ server");

                    // Cập nhật adapter NGAY LẬP TỨC
                    playlistAdapter.notifyDataSetChanged();

                    // Sau đó mới load số lượng bài hát
                    loadSongCountsForPlaylists();
                } else {
                    Toast.makeText(requireContext(), "Không tải được playlist", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Load số lượng bài hát cho từng playlist để hiển thị trên card
    private void loadSongCountsForPlaylists() {
        if (playlistList.isEmpty()) return;

        AtomicInteger remaining = new AtomicInteger(playlistList.size());

        for (Playlist playlist : playlistList) {
            playlistRepository.getPlaylistSongs(playlist.id, new Callback<List<PlaylistSong>>() {
                @Override
                public void onResponse(Call<List<PlaylistSong>> call, Response<List<PlaylistSong>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        playlist.songCount = response.body().size();
                    } else {
                        playlist.songCount = 0;
                    }

                    // Khi tất cả playlist đã load xong số lượng thì refresh adapter
                    if (remaining.decrementAndGet() == 0) {
                        playlistAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(Call<List<PlaylistSong>> call, Throwable t) {
                    playlist.songCount = 0;
                    if (remaining.decrementAndGet() == 0) {
                        playlistAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    private void onPlaylistClick(Playlist playlist) {
        android.content.Intent intent = new android.content.Intent(requireContext(),
                com.melodix.app.View.PlaylistDetailActivity.class);
        intent.putExtra(com.melodix.app.View.PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.id);
        startActivity(intent);
    }

    private String getCurrentUserId() {
        com.melodix.app.Model.SessionManager session = com.melodix.app.Model.SessionManager.getInstance(requireContext());
        if (session.getCurrentUser() != null) {
            return session.getCurrentUser().getId();
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserPlaylists();
        loadDownloadedSongs();
    }
}