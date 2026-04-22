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
import com.melodix.app.Utils.SongActionHelper;
import com.melodix.app.View.adapters.SongAdapter;
import com.melodix.app.ViewModel.SongViewModel;

import java.util.ArrayList;
import java.util.List;

public class SongManagementFragment extends Fragment {
    private RecyclerView rvAllSongs;
    private AutoCompleteTextView actvStatus;
    private SongAdapter songAdapter;

    private List<Song> fullSongList = new ArrayList<>(); // Chứa toàn bộ bài hát từ API
    private List<Song> currentDisplayList = new ArrayList<>(); // Chứa danh sách ĐANG hiển thị trên màn hình
    private SongViewModel viewModel;

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

        // 3. Khởi tạo ViewModel và Quan sát (Observe)
        viewModel = new ViewModelProvider(this).get(SongViewModel.class);

        viewModel.getAllSong().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {
                this.fullSongList = songs;
                this.currentDisplayList = new ArrayList<>(songs);

                // Xử lý bộ lọc ngầm: Khi có data mới, phải lọc lại theo Dropdown hiện tại
                String currentFilter = actvStatus.getText().toString();
                filterSongsByStatus(currentFilter); // Thay bằng hàm lọc của bạn nếu tên khác
            }
        });

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            // Quay lại Fragment trước đó trong BackStack
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

        // Giữ nguyên thiết lập Dropdown
        String[] statuses = {"All", "Pending", "Approved", "Rejected"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.dropdown_item,
                statuses
        );
        actvStatus.setAdapter(adapter);

        // =================================================================
        // GỌI HÀM FETCH API TRONG VIEWMODEL ĐỂ LẤY DATA MỚI NHẤT TỪ SUPABASE
        // =================================================================
        if (!fullSongList.isEmpty()) {
            String currentFilter = actvStatus.getText().toString();
            if (currentFilter.isEmpty()) currentFilter = "All";

            // Hàm filter này sẽ tự động quét lại fullSongList.
            // Nếu bài đó đã bị bạn setStatus thành "Approved" thì nó sẽ tự rớt khỏi list "Pending"
            filterSongsByStatus(currentFilter);
        }
    }

    private void setupRecyclerView() {
        // Đã khắc phục lỗi trống logic click ở đây
        songAdapter = new SongAdapter(requireContext(), new ArrayList<>(), new SongAdapter.OnSongActionListener() {
            @Override
            public void onSongClick(Song song, int position) {
                // Truyền currentDisplayList để PlaybackUtils lấy đúng danh sách đang lọc làm Queue
                SongActionHelper.playSongAndSetQueue(requireContext(), song, currentDisplayList);
            }

            @Override
            public void onMenuClick(Song song, int position, String action) {
                SongActionHelper.handleMenuClick(requireContext(), song, action, currentDisplayList);
            }
        });

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
        // Chỉ dùng 1 hàm update duy nhất, KHÔNG tạo lại Adapter
        songAdapter.update(new ArrayList<>(currentDisplayList));
    }
}