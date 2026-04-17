package com.melodix.app.View.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.View.adapters.SongCardAdapter;
import com.melodix.app.ViewModel.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class GenreDetailFragment extends Fragment {
    private String genreId;
    private String genreName;

    // Hàm khởi tạo nhận dữ liệu truyền vào
    public static GenreDetailFragment newInstance(String genreId, String genreName) {
        GenreDetailFragment fragment = new GenreDetailFragment();
        Bundle args = new Bundle();
        args.putString("GENRE_ID", genreId);
        args.putString("GENRE_NAME", genreName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_genre_detail, container, false);


        view.setTranslationZ(100f); // Ép nổi lên 100 pixel
        view.setClickable(true);    // Đóng băng điểm chạm (Chống bấm xuyên)
        view.setFocusable(true);    // Đóng băng focus
        view.setBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.mdx_bg)); // Đổ lại màu nền cho chắc cú

        // Nhận dữ liệu
        if (getArguments() != null) {
            genreId = getArguments().getString("GENRE_ID");
            genreName = getArguments().getString("GENRE_NAME");
        }

        // Set tiêu đề thể loại
        TextView tvTitle = view.findViewById(R.id.tv_genre_title);
        tvTitle.setText(genreName);

        // Nút Back
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // Setup RecyclerView hiển thị dạng Lưới (2 cột) cho đẹp mắt với SongCardAdapter
        RecyclerView rvSongs = view.findViewById(R.id.rv_genre_songs);
        rvSongs.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        // Lấy dữ liệu bài hát
        HomeViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // Parse ID từ String sang int (Vì bảng genres lưu id là integer)
        int id = Integer.parseInt(genreId);

        // Gọi API lấy đúng danh sách nhạc của Thể loại đó
        sharedViewModel.getSongsByGenre(id).observe(getViewLifecycleOwner(), genreSongs -> {
            // MÁY QUAY 2: Xem giao diện nhận được bao nhiêu bài
            android.util.Log.d("DEBUG_UI", "Thể loại ID " + id + " nhận được: " + genreSongs.size() + " bài hát.");

            SongCardAdapter adapter = new SongCardAdapter(requireContext(), genreSongs, false, song -> {
                PlaybackUtils.playSong(requireContext(), (ArrayList<Song>) genreSongs, song.getId());
            });
            rvSongs.setAdapter(adapter);
        });

//        // TODO: Đổi getNewReleases() thành hàm lấy bài hát theo Thể loại của bạn (VD: getSongsByGenre(genreId))
//        sharedViewModel.getNewReleases().observe(getViewLifecycleOwner(), allSongs -> {
//            List<Song> genreSongs = new ArrayList<>();
//
//            // Lọc các bài hát thuộc thể loại này (Hoặc API của bạn đã lọc sẵn rồi thì bỏ qua bước này)
//            for (Song song : allSongs) {
//                // Nếu backend trả về tất cả, bạn mở comment dòng IF dưới đây để lọc:
//                // if (song.getGenreId() != null && song.getGenreId().equals(genreId)) {
//                genreSongs.add(song);
//                // }
//            }
//
//            // Đổ dữ liệu vào SongCardAdapter
//            SongCardAdapter adapter = new SongCardAdapter(requireContext(), genreSongs, false, song -> {
//                // Bắt sự kiện bấm vào Card thì phát nhạc
//                PlaybackUtils.playSong(requireContext(), (ArrayList<Song>) genreSongs, song.getId());
//            });
//
//            rvSongs.setAdapter(adapter);
//        });

        return view;
    }
}