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
import com.melodix.app.Utils.LoadingDialog;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.View.adapters.SongCardAdapter;
import com.melodix.app.ViewModel.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class GenreDetailFragment extends Fragment {
    private String genreId;
    private String genreName;
    private LoadingDialog loadingDialog;

    
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

        loadingDialog = new LoadingDialog();

        view.setTranslationZ(100f); 
        view.setClickable(true);    
        view.setFocusable(true);    
        view.setBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.mdx_bg)); 

        
        if (getArguments() != null) {
            genreId = getArguments().getString("GENRE_ID");
            genreName = getArguments().getString("GENRE_NAME");
        }

        
        TextView tvTitle = view.findViewById(R.id.tv_genre_title);
        tvTitle.setText(genreName);

        
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        
        RecyclerView rvSongs = view.findViewById(R.id.rv_genre_songs);
        rvSongs.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        
        HomeViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        
        int id = Integer.parseInt(genreId);

        loadingDialog.showLoading(requireActivity());
        
        sharedViewModel.getSongsByGenre(id).observe(getViewLifecycleOwner(), genreSongs -> {
            loadingDialog.hideLoading();
            
            android.util.Log.d("DEBUG_UI", "Thể loại ID " + id + " nhận được: " + genreSongs.size() + " bài hát.");

            SongCardAdapter adapter = new SongCardAdapter(requireContext(), genreSongs, false, song -> {
                PlaybackUtils.playSong(requireContext(), (ArrayList<Song>) genreSongs, song.getId());
            });
            rvSongs.setAdapter(adapter);
        });






















        return view;
    }
}