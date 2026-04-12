package com.melodix.app.View.admin;

import static android.widget.Toast.LENGTH_SHORT;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.View.adapters.SongAdapter;
import com.melodix.app.ViewModel.SongViewModel;

import java.util.ArrayList;
import java.util.List;

public class AdminSongFragment extends Fragment {

    private RecyclerView rvAllSongs;
    private AutoCompleteTextView actvStatus;
    private SongAdapter songAdapter;

    private List<Song> fullSongList = new ArrayList<>(); // Chứa toàn bộ bài hát từ API
    private List<Song> currentDisplayList = new ArrayList<>(); // Chứa danh sách ĐANG hiển thị trên màn hình

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_song, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvAllSongs = view.findViewById(R.id.rvAllSongs);
        actvStatus = view.findViewById(R.id.actvStatus);

        // 1. Khởi tạo RecyclerView 1 lần duy nhất
        setupRecyclerView();

        // 2. Thiết lập Dropdown
        setupStatusFilter();

        // 3. Quan sát ViewModel
        SongViewModel viewModel = new ViewModelProvider(this).get(SongViewModel.class);
        viewModel.getAllSong().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {
                this.fullSongList = songs;
                this.currentDisplayList = new ArrayList<>(songs); // Mặc định ban đầu hiển thị tất cả
                songAdapter.update(new ArrayList<>(currentDisplayList));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Giữ nguyên đoạn code nạp lại Dropdown của bạn vì nó đã hoạt động rất tốt
        String[] statuses = {"All", "Pending", "Approved", "Reject"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.dropdown_item,
                statuses
        );
        actvStatus.setAdapter(adapter);
    }

    private void setupRecyclerView() {
        // Đã khắc phục lỗi trống logic click ở đây
        songAdapter = new SongAdapter(requireContext(), new ArrayList<>(), new SongAdapter.OnSongActionListener() {
            @Override
            public void onSongClick(Song song, int position) {
                // Truyền currentDisplayList để PlaybackUtils lấy đúng danh sách đang lọc làm Queue
                playSongAndSetQueue(song, currentDisplayList);
            }

            @Override
            public void onMenuClick(Song song, int position, String action) {
                handleMenuClick(song, action);
            }
        });

        rvAllSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAllSongs.setAdapter(songAdapter);
    }

    private void playSongAndSetQueue(Song selectedSong, List<Song> currentList) {
        PlaybackUtils.playSong(requireContext(), (ArrayList<Song>) currentList, selectedSong.getId());
    }

    private void handleMenuClick(Song song, String action) {
        switch (action) {
            case "play":
                List<Song> singleList = new ArrayList<>();
                singleList.add(song);
                playSongAndSetQueue(song, singleList);
                break;
            case "like":
                Toast.makeText(requireContext(), "LIKE " + song.getTitle(), LENGTH_SHORT).show();
                break;
            case "playlist":
                Toast.makeText(requireContext(), "Thêm " + song.getTitle() + " vào PLAYLIST", LENGTH_SHORT).show();
                break;
            case "comment":
                Toast.makeText(requireContext(), "COMMENT " + song.getTitle(), LENGTH_SHORT).show();
                break;
            case "share":
                Toast.makeText(requireContext(), "SHARE " + song.getTitle(), LENGTH_SHORT).show();
                break;
            case "download":
                Toast.makeText(requireContext(), "DOWNLOAD " + song.getTitle(), LENGTH_SHORT).show();
                break;
        }
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
        // Chỉ dùng 1 hàm update duy nhất, KHÔNG tạo lại Adapter
        songAdapter.update(new ArrayList<>(currentDisplayList));
    }
}