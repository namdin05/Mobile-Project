package com.melodix.app.View.admin.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Utils.LoadingDialog;
import com.melodix.app.Utils.SongActionHelper;
import com.melodix.app.View.adapters.SongAdapter;
import com.melodix.app.ViewModel.SongViewModel;

import java.util.ArrayList;
import java.util.List;

public class SongManagementFragment extends Fragment {
    private RecyclerView rvAllSongs;
    private AutoCompleteTextView actvStatus;
    private SongAdapter songAdapter;
    private List<Song> fullSongList = new ArrayList<>(); 
    private List<Song> currentDisplayList = new ArrayList<>(); 
    private SongViewModel viewModel;
    private LoadingDialog loadingDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_song, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadingDialog = new LoadingDialog();

        rvAllSongs = view.findViewById(R.id.rvAllSongs);
        actvStatus = view.findViewById(R.id.actvStatus);

        setupRecyclerView();

        setupStatusFilter();

        viewModel = new ViewModelProvider(this).get(SongViewModel.class);

        loadingDialog.showLoading(requireActivity());
        viewModel.getAllSong().observe(getViewLifecycleOwner(), songs -> {
            loadingDialog.hideLoading();
            if (songs != null) {
                this.fullSongList = songs;
                this.currentDisplayList = new ArrayList<>(songs);

                String currentFilter = actvStatus.getText().toString();
                filterSongsByStatus(currentFilter);
            }
        });

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().onBackPressed();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        String[] statuses = {"All", "Pending", "Approved", "Rejected"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.dropdown_item,
                statuses
        );
        actvStatus.setAdapter(adapter);

        if (!fullSongList.isEmpty()) {
            String currentFilter = actvStatus.getText().toString();
            if (currentFilter.isEmpty()) currentFilter = "All";
            filterSongsByStatus(currentFilter);
        }
    }

    private void setupRecyclerView() {
        songAdapter = new SongAdapter(requireContext(), new ArrayList<>(), new SongAdapter.OnSongActionListener() {
            @Override
            public void onSongClick(Song song, int position) {
                SongActionHelper.playSongAndSetQueue(requireContext(), song, currentDisplayList);
            }

            @Override
            public void onMenuClick(Song song, int position, String action) {
                SongActionHelper.handleMenuClick(requireContext(), song, action, currentDisplayList);
            }
        });

        // KÍCH HOẠT CHẾ ĐỘ ADMIN: Ẩn nút More (menu dành cho người dùng)
        songAdapter.setAdminMode(true);

        rvAllSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAllSongs.setAdapter(songAdapter);
    }

    private void setupStatusFilter() {
        actvStatus.setOnItemClickListener((parent, view, position, id) -> {
            String selectedStatus = parent.getItemAtPosition(position).toString();
            filterSongsByStatus(selectedStatus);
        });
    }

    private void filterSongsByStatus(String status) {
        if (status.equals("All")) {
            currentDisplayList = new ArrayList<>(fullSongList);
        } else {
            currentDisplayList = new ArrayList<>();
            for (Song song : fullSongList) {
                if (song.getStatus() != null && song.getStatus().equalsIgnoreCase(status)) {
                    currentDisplayList.add(song);
                }
            }
        }
        songAdapter.update(new ArrayList<>(currentDisplayList));
    }
}
