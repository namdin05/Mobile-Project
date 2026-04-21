package com.melodix.app.View.admin.dashboard;

import static java.security.AccessController.getContext;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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


        setupRecyclerView();

        viewModel = new ViewModelProvider(this).get(AlbumViewModel.class);
        viewModel.getAllAlbums().observe(getViewLifecycleOwner(), albums -> {
            if (albums != null) {
                albumList.clear();
                albumList.addAll(albums);
                adapter.notifyDataSetChanged();
            }

            if(albumList.isEmpty()){
                Toast.makeText(getContext(), "Chưa có dữ liệu album nào", Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void setupRecyclerView() {
        adapter = new AlbumAdapter(requireContext(), albumList, new AlbumAdapter.OnAlbumClickListener() {
            @Override
            public void onAlbumClick(Album album) {
                openAlbumSongsFragment(album.getId(), album.getTitle());
            }
        });

        rvAlbums.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        rvAlbums.setAdapter(adapter);
    }


    // Hàm điều hướng
    private void openAlbumSongsFragment(String albumId, String albumTitle) {
        // Tạo Fragment mới để chứa danh sách bài hát (bạn sẽ cần tạo AdminAlbumDetailFragment)
        AlbumDetailFragment detailFragment = new AlbumDetailFragment();

        // Gói album_id gửi sang bên kia
        Bundle bundle = new Bundle();
        bundle.putString("ALBUM_ID", albumId);
        bundle.putString("ALBUM_TITLE", albumTitle);
        detailFragment.setArguments(bundle);

        // Chuyển Fragment trong AdminActivity
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null) // Cho phép bấm nút Back để quay lại list Album
                .commit();
    }
}
