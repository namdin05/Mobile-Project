package com.melodix.app.View.playlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.melodix.app.Adapter.PlaylistAdapter;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.PlaylistCreateRequest;
import com.melodix.app.R;
import com.melodix.app.Utils.SessionManager;
import com.melodix.app.ViewModel.PlaylistViewModel;

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

        // Ánh xạ views
        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        btnCreatePlaylist = view.findViewById(R.id.fabCreatePlaylist);   // hoặc fabCreatePlaylist nếu bạn dùng FAB

        // Setup RecyclerView
        adapter = new PlaylistAdapter(this::onPlaylistClicked);
        rvPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPlaylists.setAdapter(adapter);

        // Observe LiveData
        observeViewModel();

        // Load dữ liệu
        loadPlaylists();

        // Sự kiện tạo playlist mới
        btnCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
    }

    private void observeViewModel() {
        viewModel.getMyPlaylists().observe(getViewLifecycleOwner(), playlists -> {
            if (playlists != null) {
                adapter.setPlaylists(playlists);
            }
        });


        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPlaylists() {
        String userId = sessionManager.getUserId();

        Log.d("PLAYLIST_DEBUG", "User ID từ Session: " + userId);   // ← Thêm dòng này

        if (userId != null && !userId.isEmpty()) {
            viewModel.loadMyPlaylists(userId);
        } else {
            Toast.makeText(requireContext(), "Không tìm thấy User ID. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            Log.e("PLAYLIST_DEBUG", "User ID bị null hoặc rỗng!");
        }
    }

    // ==================== TẠO PLAYLIST ====================
    private void showCreatePlaylistDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_playlist, null);

        TextInputEditText edtPlaylistName = dialogView.findViewById(R.id.edtPlaylistName);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tạo Playlist Mới")
                .setView(dialogView)
                .setPositiveButton("Tạo", (dialog, which) -> {
                    String name = edtPlaylistName.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Vui lòng nhập tên playlist", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String userId = sessionManager.getUserId();
                    if (userId == null || userId.isEmpty()) {
                        Toast.makeText(requireContext(), "Không tìm thấy User ID. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Truyền userId vào request
                    PlaylistCreateRequest request = new PlaylistCreateRequest(name, null, userId);

                    viewModel.createPlaylist(request, new PlaylistViewModel.OnOperationCompleteListener() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                            loadPlaylists();        // Refresh danh sách
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Click vào một playlist → Mở chi tiết
    private void onPlaylistClicked(Playlist playlist) {
        PlaylistDetailFragment detailFragment = PlaylistDetailFragment.newInstance(
                playlist.getId(),
                playlist.getName()
        );

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    private void confirmDeletePlaylist(Playlist playlist) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa Playlist")
                .setMessage("Bạn có chắc chắn muốn xóa playlist '" + playlist.getName() + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.deletePlaylist(playlist.getId(), new PlaylistViewModel.OnOperationCompleteListener() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                            loadPlaylists(); // Tải lại danh sách sau khi xóa thành công
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}