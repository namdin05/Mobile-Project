package com.melodix.app.View.admin.dashboard;

import static java.security.AccessController.getContext;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Adapter.AlbumAdapter;
import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Album;
import com.melodix.app.R;
import com.melodix.app.Service.AlbumAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.ViewModel.AlbumViewModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlbumManagementFragment extends Fragment {
    private RecyclerView rvAlbums;
    private AlbumAdapter adapter;
    private List<Album> albumList;

    private AutoCompleteTextView actvStatus;
    private List<Album> fullAlbumList = new ArrayList<>();
    private List<Album> displayList = new ArrayList<>();

    private AlbumViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_album, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvAlbums = view.findViewById(R.id.rvAlbums);
        albumList = new ArrayList<>();
        actvStatus = view.findViewById(R.id.actvStatus);

        setupRecyclerView();
        setupStatusFilter();

        viewModel = new ViewModelProvider(this).get(AlbumViewModel.class);
        viewModel.getAllAlbums().observe(getViewLifecycleOwner(), albums -> {
            if (albums != null) {
                // Đổ dữ liệu vào list gốc
                fullAlbumList.clear();
                fullAlbumList.addAll(albums);

                // Lọc lại dữ liệu dựa trên dropdown hiện tại
                String currentFilter = actvStatus.getText().toString();
                filterAlbumByStatus(currentFilter);
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

    private void setupRecyclerView() {
        adapter = new AlbumAdapter(requireContext(), displayList, new AlbumAdapter.OnAlbumClickListener() {
            @Override
            public void onAlbumClick(Album album) {
                // SỬA ĐỔI: Phải truyền thêm Cover và Status sang màn hình Chi tiết
                openAlbumSongsFragment(album.getId(), album.getTitle(), album.getCoverUrl(), album.getStatus());
            }
        });

        rvAlbums.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        rvAlbums.setAdapter(adapter);
    }

    private void setupStatusFilter() {
        actvStatus.setOnItemClickListener((parent, view, position, id) -> {
            String selectedStatus = parent.getItemAtPosition(position).toString();
            filterAlbumByStatus(selectedStatus);
        });
    }

    private void filterAlbumByStatus(String status) {
        displayList.clear();
        if (status.equals("All")) {
            displayList.addAll(fullAlbumList);
        } else {
            for (Album album : fullAlbumList) {
                if (album.getStatus() != null && album.getStatus().equalsIgnoreCase(status)) {
                    displayList.add(album);
                }
            }
        }

        // SỬA: Cần truyền displayList vào Adapter và gọi notifyDataSetChanged()
        // Nếu AlbumAdapter của bạn chưa có hàm update(), hãy tự thêm vào hoặc gán lại list
        adapter.update(new ArrayList<>(displayList));
    }

    @Override
    public void onResume() {
        super.onResume();

        String[] statuses = {"All", "Pending", "Approved", "Hide", "Rejected"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                requireContext(),
                R.layout.dropdown_item, // Đảm bảo bạn có file layout này (giống bên User)
                statuses
        );
        actvStatus.setAdapter(adapter);
    }


    // Hàm điều hướng
    private void openAlbumSongsFragment(String albumId, String albumTitle, String coverUrl, String status) {
        AlbumDetailFragment detailFragment = new AlbumDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putString("ALBUM_ID", albumId);
        bundle.putString("ALBUM_TITLE", albumTitle);
        bundle.putString("ALBUM_COVER", coverUrl);
        bundle.putString("ALBUM_STATUS", status);
        detailFragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }
}
