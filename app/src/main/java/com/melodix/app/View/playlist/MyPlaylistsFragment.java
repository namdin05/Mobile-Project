package com.melodix.app.View.playlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.melodix.app.Adapter.PlaylistAdapter;
import com.melodix.app.Model.Playlist;
import com.melodix.app.R;
import com.melodix.app.Utils.SessionManager;
import com.melodix.app.ViewModel.PlaylistViewModel;

import java.util.List;

public class MyPlaylistsFragment extends Fragment {

    private PlaylistViewModel viewModel;
    private PlaylistAdapter adapter;
    private RecyclerView rvPlaylists;
    private MaterialButton btnCreatePlaylist;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_playlists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(this).get(PlaylistViewModel.class);

        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        btnCreatePlaylist = view.findViewById(R.id.btnCreatePlaylist);

        // Setup RecyclerView
        adapter = new PlaylistAdapter(this::onPlaylistClicked);
        rvPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPlaylists.setAdapter(adapter);

        // Observe data
        observeViewModel();

        // Load dữ liệu
        loadPlaylists();

        // Sự kiện nút tạo playlist
        btnCreatePlaylist.setOnClickListener(v -> {
            // Tạm thời chỉ Toast, sau sẽ mở dialog tạo playlist
            Toast.makeText(requireContext(), "Chức năng tạo playlist sẽ được triển khai ở đợt sau", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        viewModel.getMyPlaylists().observe(getViewLifecycleOwner(), playlists -> {
            if (playlists != null) {
                adapter.setPlaylists(playlists);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Có thể thêm ProgressBar sau
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPlaylists() {
        String userId = sessionManager.getUserId();
        if (userId != null && !userId.isEmpty()) {
            viewModel.loadMyPlaylists(userId);
        } else {
            Toast.makeText(requireContext(), "Chưa đăng nhập hoặc không tìm thấy user ID", Toast.LENGTH_SHORT).show();
        }
    }

    private void onPlaylistClicked(Playlist playlist) {
        Toast.makeText(requireContext(), "Clicked: " + playlist.getName(), Toast.LENGTH_SHORT).show();

    }
}