package com.melodix.app.View.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.melodix.app.Model.Playlist;
import com.melodix.app.R;
import com.melodix.app.Utils.PlaylistSelectionManager;
import com.melodix.app.View.adapters.PlaylistSelectAdapter;

import androidx.fragment.app.Fragment;
import com.melodix.app.View.fragments.LibraryFragment;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistSelectionDialog extends BottomSheetDialogFragment {

    private String songId;
    private OnPlaylistActionListener actionListener;
    private PlaylistSelectionManager selectionManager;
    private List<Playlist> playlistList = new ArrayList<>();
    private Set<String> selectedIds = new HashSet<>();
    private PlaylistSelectAdapter adapter;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private CreatePlaylistDialog currentCreateDialog;
    private boolean isCreatingPlaylist = false;

    public interface OnPlaylistActionListener {
        void onPlaylistUpdated();
    }

    public static PlaylistSelectionDialog newInstance(String songId) {
        PlaylistSelectionDialog dialog = new PlaylistSelectionDialog();
        Bundle args = new Bundle();
        args.putString("song_id", songId);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            songId = getArguments().getString("song_id");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_select_playlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() == null) return;

        selectionManager = new PlaylistSelectionManager(getContext());

        // Khởi tạo Image Picker Launcher cho CreatePlaylistDialog
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && currentCreateDialog != null) {
                        currentCreateDialog.setSelectedCoverUri(uri);
                    }
                });

        RecyclerView rvPlaylists = view.findViewById(R.id.rv_playlists);
        Button btnCreateNew = view.findViewById(R.id.btn_create_new);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));

        loadPlaylists();

        btnCreateNew.setOnClickListener(v -> {
            if (!isCreatingPlaylist) {
                showCreatePlaylistDialog();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void loadPlaylists() {
        if (getContext() == null) return;

        // ĐÃ SỬA: Lấy USER_ID từ SharedPreferences thay vì SessionManager
        SharedPreferences prefs = getContext().getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", null);
        boolean isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false);

        if (!isLoggedIn || userId == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        selectionManager.loadUserPlaylistsWithSongStatus(userId, songId,
                new PlaylistSelectionManager.OnPlaylistsLoadedListener() {
                    @Override
                    public void onPlaylistsLoaded(List<Playlist> playlists, Set<String> selectedIds) {
                        if (getContext() == null) return;

                        playlistList.clear();
                        playlistList.addAll(playlists);
                        PlaylistSelectionDialog.this.selectedIds.clear();
                        PlaylistSelectionDialog.this.selectedIds.addAll(selectedIds);

                        adapter = new PlaylistSelectAdapter(getContext(), playlistList,
                                new ArrayList<>(selectedIds),
                                (playlist, isSelected) -> {
                                    if (isSelected) {
                                        addToPlaylist(playlist);
                                    } else {
                                        removeFromPlaylist(playlist);
                                    }
                                });

                        if (getView() != null) {
                            RecyclerView rv = getView().findViewById(R.id.rv_playlists);
                            rv.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void addToPlaylist(Playlist playlist) {
        if (getContext() == null) return;

        selectionManager.addSongToPlaylist(songId, playlist.id,
                new PlaylistSelectionManager.OnSelectionChangedListener() {
                    @Override
                    public void onSuccess(String message) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                        selectedIds.add(playlist.id);
                        if (actionListener != null) actionListener.onPlaylistUpdated();
                    }

                    @Override
                    public void onError(String error) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                        }
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                });
    }

    private void removeFromPlaylist(Playlist playlist) {
        if (getContext() == null) return;

        selectionManager.removeSongFromPlaylist(songId, playlist.id,
                new PlaylistSelectionManager.OnSelectionChangedListener() {
                    @Override
                    public void onSuccess(String message) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                        selectedIds.remove(playlist.id);
                        if (actionListener != null) actionListener.onPlaylistUpdated();
                    }

                    @Override
                    public void onError(String error) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                        }
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                });
    }

    private void showCreatePlaylistDialog() {
        if (getContext() == null || isCreatingPlaylist) return;

        isCreatingPlaylist = true;

        CreatePlaylistDialog createDialog = new CreatePlaylistDialog(
                requireContext(),
                newPlaylist -> {
                    Toast.makeText(requireContext(), "Đã tạo playlist: " + newPlaylist.name, Toast.LENGTH_SHORT).show();

                    // Load lại danh sách playlist trong dialog này
                    loadPlaylists();

                    if (getActivity() != null) {
                        Fragment currentFragment = getActivity().getSupportFragmentManager()
                                .findFragmentById(R.id.main_fragment_container);

                        if (currentFragment instanceof LibraryFragment) {
                            ((LibraryFragment) currentFragment).loadUserPlaylists();
                        }
                    }

                    if (actionListener != null) {
                        actionListener.onPlaylistUpdated();
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded() && isVisible()) {
                            dismiss();
                        }
                    }, 800);
                },
                imagePickerLauncher
        );

        currentCreateDialog = createDialog;
        createDialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> isCreatingPlaylist = false, 1500);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isCreatingPlaylist = false;
        currentCreateDialog = null;
    }

    public void setOnPlaylistActionListener(OnPlaylistActionListener listener) {
        this.actionListener = listener;
    }
}