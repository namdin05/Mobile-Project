package com.melodix.app.View.fragments;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.DownloadedSong;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.Model.Song;
import com.melodix.app.Model.AppDatabase;
import com.melodix.app.PlayerActivity;
import com.melodix.app.R;
import com.melodix.app.Repository.PlaybackRepository;
import com.melodix.app.Repository.PlaylistRepository;
import com.melodix.app.Utils.NetworkUtils;
import com.melodix.app.View.adapters.DownloadedSongAdapter;
import com.melodix.app.View.adapters.PlaylistAdapter;
import com.melodix.app.View.dialogs.CreatePlaylistDialog;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryFragment extends Fragment {

    private RecyclerView rvPlaylists, rvDownloaded;
    private PlaylistAdapter playlistAdapter;
    private DownloadedSongAdapter downloadedSongAdapter;

    private List<Playlist> playlistList = new ArrayList<>();
    private Button btnCreatePlaylist;
    private TextView tvOfflineNotice;           // ← Quan trọng

    private PlaylistRepository playlistRepository;

    private ActivityResultLauncher<String> imagePickerLauncher;
    private CreatePlaylistDialog currentDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        playlistRepository = new PlaylistRepository(requireContext());

        rvPlaylists = view.findViewById(R.id.rv_playlists);
        rvDownloaded = view.findViewById(R.id.rv_downloaded);
        btnCreatePlaylist = view.findViewById(R.id.btn_create_playlist);
        tvOfflineNotice = view.findViewById(R.id.tv_offline_notice);
        if (tvOfflineNotice == null) {
            Log.w("LibraryFragment", "tvOfflineNotice not found in layout");
        }

        // Setup RecyclerViews
        rvPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));
        playlistAdapter = new PlaylistAdapter(requireContext(), playlistList, this::onPlaylistClick);
        rvPlaylists.setAdapter(playlistAdapter);

        rvDownloaded.setLayoutManager(new LinearLayoutManager(requireContext()));
        downloadedSongAdapter = new DownloadedSongAdapter(requireContext(), new DownloadedSongAdapter.OnDownloadedSongClickListener() {
            @Override
            public void onSongClick(DownloadedSong song) {
                playDownloadedSong(song);
            }

            @Override
            public void onMoreClick(DownloadedSong song, int position) {
                Toast.makeText(requireContext(), "Đã tải: " + song.title, Toast.LENGTH_SHORT).show();
            }
        });
        rvDownloaded.setAdapter(downloadedSongAdapter);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && currentDialog != null) {
                        currentDialog.setSelectedCoverUri(uri);
                    }
                });

        btnCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        // Load dữ liệu an toàn
        loadDownloadedSongs();
        checkNetworkAndLoadContent();

        return view;
    }

    private void checkNetworkAndLoadContent() {
        boolean isOnline = NetworkUtils.isNetworkAvailable(requireContext());
        if (tvOfflineNotice != null) {
            tvOfflineNotice.setVisibility(isOnline ? View.GONE : View.VISIBLE);
        }
        if (isOnline) {
            tvOfflineNotice.setVisibility(View.GONE);
            rvPlaylists.setVisibility(View.VISIBLE);
            loadUserPlaylists();           // Chỉ load khi có mạng
        } else {
            tvOfflineNotice.setVisibility(View.VISIBLE);
            rvPlaylists.setVisibility(View.GONE);   // Ẩn hoàn toàn phần Playlist
            playlistList.clear();
            if (playlistAdapter != null) playlistAdapter.notifyDataSetChanged();
        }
    }

    private void loadDownloadedSongs() {
        // Chạy việc xóa bản ghi không hợp lệ trên background thread
        new Thread(() -> {
            try {
                // Xóa các bản ghi có localAudioPath rỗng hoặc null
                AppDatabase.getInstance(requireContext())
                        .downloadedSongDao()
                        .deleteInvalidEntries();

                // Xóa thêm các bản ghi mà file thực tế không tồn tại
                List<DownloadedSong> allSongs = AppDatabase.getInstance(requireContext())
                        .downloadedSongDao()
                        .getAllDownloadedSync();

                for (DownloadedSong song : allSongs) {
                    if (song.localAudioPath != null && !song.localAudioPath.isEmpty()) {
                        File file = new File(song.localAudioPath);
                        if (!file.exists()) {
                            AppDatabase.getInstance(requireContext())
                                    .downloadedSongDao()
                                    .deleteById(song.songId);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("LibraryFragment", "Lỗi khi dọn dẹp downloaded songs", e);
            }

            // Sau khi dọn dẹp xong, load dữ liệu hợp lệ và cập nhật UI trên Main Thread
            requireActivity().runOnUiThread(() -> {
                AppDatabase.getInstance(requireContext())
                        .downloadedSongDao()
                        .getAllDownloaded()
                        .observe(getViewLifecycleOwner(), downloadedSongs -> {

                            List<DownloadedSong> validList = new ArrayList<>();

                            for (DownloadedSong song : downloadedSongs) {
                                if (song.localAudioPath != null && !song.localAudioPath.isEmpty()) {
                                    File file = new File(song.localAudioPath);
                                    if (file.exists()) {
                                        validList.add(song);
                                    }
                                }
                            }

                            downloadedSongAdapter.updateList(validList);

                            if (rvDownloaded != null) {
                                rvDownloaded.setVisibility(validList.isEmpty() ? View.GONE : View.VISIBLE);
                            }
                        });
            });
        }).start();
    }
    private void showCreatePlaylistDialog() {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Không thể tạo playlist khi không có mạng", Toast.LENGTH_SHORT).show();
            return;
        }

        currentDialog = new CreatePlaylistDialog(requireContext(), playlist -> {
            playlistList.add(0, playlist);
            playlistAdapter.notifyItemInserted(0);
            rvPlaylists.scrollToPosition(0);

            new Handler(Looper.getMainLooper()).postDelayed(this::loadUserPlaylists, 1000);
        }, imagePickerLauncher);

        currentDialog.show();
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
                    playlistList.sort((p1, p2) -> p2.id.compareTo(p1.id));

                    playlistAdapter.notifyDataSetChanged();
                    loadSongCountsForPlaylists();
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                // Không hiện Toast lỗi khi offline để tránh spam
            }
        });
    }

    private void loadSongCountsForPlaylists() {
        if (playlistList.isEmpty()) return;

        AtomicInteger remaining = new AtomicInteger(playlistList.size());

        for (Playlist playlist : playlistList) {
            playlistRepository.getPlaylistSongs(playlist.id, new Callback<List<PlaylistSong>>() {
                @Override
                public void onResponse(Call<List<PlaylistSong>> call, Response<List<PlaylistSong>> response) {
                    playlist.songCount = (response.isSuccessful() && response.body() != null) ? response.body().size() : 0;
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

    private void playDownloadedSong(DownloadedSong downloadedSong) {
        if (downloadedSong == null || downloadedSong.localAudioPath == null) {
            Toast.makeText(requireContext(), "Không tìm thấy file nhạc", Toast.LENGTH_SHORT).show();
            return;
        }

        File audioFile = new File(downloadedSong.localAudioPath);
        if (!audioFile.exists()) {
            Toast.makeText(requireContext(), "File nhạc đã bị xóa hoặc không tồn tại", Toast.LENGTH_LONG).show();
            // Xóa bản ghi lỗi
            AppDatabase.getInstance(requireContext()).downloadedSongDao().deleteById(downloadedSong.songId);
            loadDownloadedSongs();
            return;
        }

        Song offlineSong = new Song(
                downloadedSong.songId,
                downloadedSong.title != null ? downloadedSong.title : "Unknown Title",
                null,
                downloadedSong.artistName != null ? downloadedSong.artistName : "Unknown Artist",
                null, null,
                downloadedSong.coverUrl != null ? downloadedSong.coverUrl : "",
                downloadedSong.localAudioPath,
                null,
                "Bài hát đã tải về",
                downloadedSong.durationSeconds,
                0, 0
        );

        PlaybackRepository.getInstance().setCurrentSong(offlineSong);

        Intent intent = new Intent(requireContext(), PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_SONG_ID, downloadedSong.songId);
        intent.putExtra("start_playback", true);
        startActivity(intent);
    }

    private void onPlaylistClick(Playlist playlist) {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Không thể mở playlist khi offline", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(requireContext(), com.melodix.app.View.PlaylistDetailActivity.class);
        intent.putExtra(com.melodix.app.View.PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.id);
        startActivity(intent);
    }

    // ĐÃ SỬA: Lấy USER_ID từ SharedPreferences thay vì SessionManager
    private String getCurrentUserId() {
        SharedPreferences prefs = requireContext().getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        return prefs.getString("USER_ID", null);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDownloadedSongs();
        checkNetworkAndLoadContent();        // Quan trọng: kiểm tra lại mạng
    }
}