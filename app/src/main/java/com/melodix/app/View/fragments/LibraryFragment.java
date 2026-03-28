package com.melodix.app.View.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;

public class LibraryFragment extends Fragment {
    private AppRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        repository = AppRepository.getInstance(requireContext());

        // Chỉ ánh xạ giao diện cơ bản, chưa nạp dữ liệu vào Adapter để tránh lỗi thiếu hàm
        androidx.recyclerview.widget.RecyclerView rvPlaylists = view.findViewById(R.id.rv_playlists);
        androidx.recyclerview.widget.RecyclerView rvDownloaded = view.findViewById(R.id.rv_downloaded);
        androidx.recyclerview.widget.RecyclerView rvRecent = view.findViewById(R.id.rv_recent);

        return view;
    }
}