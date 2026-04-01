package com.melodix.app.View.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
                    Toast.makeText(requireContext(), "PLAY THIS SONG", Toast.LENGTH_LONG).show();
                }
            });
            rvAllGenres.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            rvAllGenres.setAdapter(genreAdapter);
        });


        return view;
    }
}
