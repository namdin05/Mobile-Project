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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Adapter.AlbumAdapter;
import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Album;
import com.melodix.app.R;
import com.melodix.app.Service.AlbumAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlbumManagementFragment extends Fragment {
    private RecyclerView rvAlbums;
    private AlbumAdapter adapter;
    private List<Album> albumList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_album, container, false);

        rvAlbums = view.findViewById(R.id.rvAlbums);
        rvAlbums.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2));

        albumList = new ArrayList<>();

        // Setup Adapter và bắt sự kiện Click
        adapter = new AlbumAdapter(requireContext(), albumList, new AlbumAdapter.OnAlbumClickListener() {
            @Override
            public void onAlbumClick(Album album) {
                // CHUYỂN SANG FRAGMENT DANH SÁCH BÀI HÁT
                openAlbumSongsFragment(album.getId(), album.getTitle());
            }
        });

        rvAlbums.setAdapter(adapter);

        fetchAlbums();

        return view;
    }

    private void fetchAlbums() {
        AlbumAPIService apiService = RetrofitClient.getClient().create(AlbumAPIService.class);
        String apiKey = BuildConfig.SERVICE_KEY;
        String authHeader = "Bearer " + BuildConfig.SERVICE_KEY;

        apiService.getAllAlbums(apiKey, authHeader).enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    albumList.clear();
                    albumList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e("AdminAlbum", "Lỗi tải album: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Album>> call, Throwable t) {
                Log.e("AdminAlbum", "Lỗi mạng: " + t.getMessage());
            }
        });
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
