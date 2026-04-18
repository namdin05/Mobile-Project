package com.melodix.app.View.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.Genre;
import com.melodix.app.R;
import com.melodix.app.View.adapters.GenreAdapter;
import com.melodix.app.ViewModel.HomeViewModel;

public class AllGenresFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        HomeViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        View view = inflater.inflate(R.layout.fragment_all_genres, container, false);
        RecyclerView rvAllGenres = view.findViewById(R.id.rv_all_genres);
        sharedViewModel.getGenres().observe(getViewLifecycleOwner(), genres -> {
            GenreAdapter genreAdapter = new GenreAdapter(requireContext(), genres, new GenreAdapter.OnGenreClickListener() {
                @Override
                public void onGenreClick(Genre genre) {
                    android.util.Log.d("TEST_CLICK", "ĐÃ BẤM VÀO THỂ LOẠI: " + genre.getName());
                    try {
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right)
                                .add(R.id.main_fragment_container, GenreDetailFragment.newInstance(String.valueOf(genre.getId()), genre.getName()))
                                .addToBackStack(null)
                                .commit();

                        // MÁY QUAY 2: Kiểm tra xem lệnh chuyển trang có chạy mượt không
                        android.util.Log.d("TEST_CLICK", "Đã gọi lệnh chuyển trang thành công!");
                    } catch (Exception e) {
                        android.util.Log.e("TEST_CLICK", "Lỗi chuyển trang: " + e.getMessage());
                    }
                }
            });
            rvAllGenres.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            rvAllGenres.setAdapter(genreAdapter);

            ImageView backBtn = view.findViewById(R.id.btn_back);
            backBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
        });


        return view;
    }
}
