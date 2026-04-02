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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.melodix.app.Adapter.PlaylistSongAdapter;
import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.R;
import com.melodix.app.ViewModel.PlaylistViewModel;

import java.util.List;

public class PlaylistDetailFragment extends Fragment {

    private static final String ARG_PLAYLIST_ID = "playlist_id";
    private static final String ARG_PLAYLIST_NAME = "playlist_name";

    private String playlistId;
    private String playlistName;

    private PlaylistViewModel viewModel;
    private PlaylistSongAdapter adapter;
    private RecyclerView rvSongs;
    private MaterialToolbar toolbar;
    private TextView tvPlaylistName;
    private TextView tvSongCount;
    private MaterialButton btnAddSong, btnEditPlaylist, btnDeletePlaylist;

    public static PlaylistDetailFragment newInstance(String playlistId, String playlistName) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PLAYLIST_ID, playlistId);
        args.putString(ARG_PLAYLIST_NAME, playlistName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            playlistId = getArguments().getString(ARG_PLAYLIST_ID);
            playlistName = getArguments().getString(ARG_PLAYLIST_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PlaylistViewModel.class);

        // Ánh xạ
        toolbar = view.findViewById(R.id.toolbar);
        rvSongs = view.findViewById(R.id.rvPlaylistSongs);
        tvPlaylistName = view.findViewById(R.id.tvPlaylistName);
        tvSongCount = view.findViewById(R.id.tvSongCount);

        btnAddSong = view.findViewById(R.id.btnAddSong);
        btnEditPlaylist = view.findViewById(R.id.btnEditPlaylist);
        btnDeletePlaylist = view.findViewById(R.id.btnDeletePlaylist);

        if (playlistName != null) {
            tvPlaylistName.setText(playlistName);
            toolbar.setTitle("");
        }

        toolbar.setTitle(playlistName);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // Setup RecyclerView
        adapter = new PlaylistSongAdapter(this::onSongRemove);
        rvSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSongs.setAdapter(adapter);

        // Setup Drag & Drop
        setupDragAndDrop();

        // Observe data
        observeViewModel();

        // Load dữ liệu
        loadPlaylistSongs();

        // Sự kiện nút
        btnAddSong.setOnClickListener(v -> showAddSongDialog());
        btnEditPlaylist.setOnClickListener(v -> showEditPlaylistDialog());
        btnDeletePlaylist.setOnClickListener(v -> showDeletePlaylistConfirm());
    }

    private void observeViewModel() {
        viewModel.getPlaylistSongs().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {
                adapter.setSongs(songs);

                int count = songs.size();
                String countText = count + " bài hát";
                tvSongCount.setText(countText);

                updateSongCount(count);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPlaylistSongs() {
        if (playlistId != null) {
            viewModel.loadPlaylistSongs(playlistId);
        }
    }

    private void updateSongCount(int count) {
    }

    // ==================== DRAG & DROP ====================
    private void setupDragAndDrop() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                adapter.moveItem(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // Sau khi kéo thả xong, cập nhật thứ tự lên server
                adapter.notifyDataSetChanged();
                viewModel.reorderSongs(playlistId, adapter.getCurrentOrder());
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(rvSongs);
    }

    private void onSongRemove(PlaylistSong playlistSong) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa bài hát")
                .setMessage("Bạn có chắc muốn xóa bài hát này khỏi playlist?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.removeSongFromPlaylist(playlistId, playlistSong.getSongId(),
                            new PlaylistViewModel.OnOperationCompleteListener() {
                                @Override
                                public void onSuccess(String message) {
                                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                    loadPlaylistSongs();
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

    // ==================== DIALOGS ====================
    private void showAddSongDialog() {
        Toast.makeText(requireContext(), "Chức năng chọn bài hát để thêm sẽ được triển khai sau", Toast.LENGTH_SHORT).show();
        // TODO: Mở dialog hoặc fragment chọn bài hát từ danh sách songs
    }

    private void showEditPlaylistDialog() {
        Toast.makeText(requireContext(), "Chức năng sửa playlist sẽ được triển khai sau", Toast.LENGTH_SHORT).show();
    }

    private void showDeletePlaylistConfirm() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa Playlist")
                .setMessage("Bạn có chắc muốn xóa playlist này và tất cả bài hát bên trong?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // GỌI VM ĐỂ XÓA THỰC SỰ TRÊN DATABASE
                    viewModel.deletePlaylist(playlistId, new PlaylistViewModel.OnOperationCompleteListener() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(requireContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}