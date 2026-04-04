package com.melodix.app.View.admin;

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
import com.melodix.app.Adapter.AdminSongAdapter;
import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Service.AdminAPIService;
import com.melodix.app.Service.RetrofitClient;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminSongFragment extends Fragment {

    private RecyclerView rvAllSongs;
    private AdminSongAdapter adapter;
    private List<Song> songList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_song, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvAllSongs = view.findViewById(R.id.rvAllSongs);
        rvAllSongs.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminSongAdapter(songList);
        rvAllSongs.setAdapter(adapter);

        fetchAllSongs();
    }

    private void fetchAllSongs() {
        AdminAPIService apiService = RetrofitClient.getClient().create(AdminAPIService.class);
        String token = "Bearer " + BuildConfig.SERVICE_KEY;

        apiService.getAllSongs(BuildConfig.SERVICE_KEY, token).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    songList.clear();
                    songList.addAll(response.body());
                    adapter.notifyDataSetChanged();


                    // THÊM: Báo nếu danh sách trả về rỗng
                    if (songList.isEmpty()) {
                        Toast.makeText(getContext(), "Kho nhạc đang trống!", Toast.LENGTH_SHORT).show();
                        Log.d("MELODIX_ADMIN", "API gọi thành công nhưng không có bài hát nào trong bảng songs.");
                    } else {
                        Log.d("MELODIX_ADMIN", "Đã tải thành công: " + songList.size() + " bài hát.");
                    }

                } else {
                    // THÊM CÁI NÀY ĐỂ BẮT LỖI TỪ SUPABASE
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Lỗi không xác định";
                        Log.e("MELODIX_ADMIN", "Lỗi API lấy nhạc: " + errorMsg);
                        Toast.makeText(getContext(), "Lỗi máy chủ: " + response.code(), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                Log.e("MELODIX_ADMIN", "Lỗi mạng: " + t.getMessage());
                Toast.makeText(getContext(), "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}