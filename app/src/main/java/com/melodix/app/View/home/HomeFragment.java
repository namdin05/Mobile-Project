package com.melodix.app.View.home;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.melodix.app.Model.Banner;
import com.melodix.app.Model.Genre;
import com.melodix.app.Model.Profile;
import com.melodix.app.Model.SessionManager;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.auth.AuthRepository;
import com.melodix.app.ViewModel.HomeViewModel;

public class HomeFragment extends Fragment {
    Profile user;
    ViewPager2 bannerPager;
    private android.os.Handler sliderHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable sliderRunnable;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        RecyclerView rvNewRelease = view.findViewById(R.id.rv_new_releases);
        RecyclerView rvTrending = view.findViewById(R.id.rv_trending);

        // fetch new release
        // in du lieu ra logcat, getViewLifecycleOwner giup ham chi chay khi user dang mo fragment
        // bien songs co kieu du lieu dua theo du lieu tra ve cua ham fetchNewReleaseSongs
        viewModel.getNewReleases().observe(getViewLifecycleOwner(), songs -> {
            SongAdapter songAdapter = new SongAdapter(requireContext(), songs, new SongAdapter.OnSongClickListener() {
                @Override
                public void onSongClick(Song song) {
                    Toast.makeText(requireContext(), "PLAY NEW RELEASE", LENGTH_LONG).show();
                }
                @Override
                public void onMenuClick(Song song, int postion, String action){
                    handleMenuClick(action);
                }
            });
            Log.d("NEW_RELEASE_SONG", new Gson().toJson(songs));
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
            rvNewRelease.setLayoutManager(linearLayoutManager);
            rvNewRelease.setAdapter(songAdapter);
        });

        // fetch current user
        user = SessionManager.getInstance(requireContext()).getCurrentUser();
        if(user != null)  Log.d("get_session_user", user.getDisplayName()+" hello");
        else Log.d("get_session_user", "chua load xong user");

        ImageView avatar = view.findViewById(R.id.img_avatar);
        TextView greeting = view.findViewById(R.id.tv_greeting);
        TextView subGreeting = view.findViewById(R.id.tv_subgreeting);
        TextView btnViewAllGenres = view.findViewById(R.id.tv_view_all_genres);

        if (user != null) {
            Glide.with(requireContext()).load(user.getAvatarUrl()).circleCrop().into(avatar);
            greeting.setText("Welcome back");
            //   subGreeting.setText(user.headline == null ? "Good evening" : user.headline);
        }

        // fetch trending songs
        viewModel.getTrendingSongs().observe(getViewLifecycleOwner(), songs->{
            SongCardAdapter songCardAdapter = new SongCardAdapter(requireContext(), songs, true, new SongCardAdapter.OnSongClickListener() {
                @Override
                public void onSongClick(Song song) {
                    Toast.makeText(requireContext(), "PLAY TRENDING SONGS", LENGTH_LONG).show();
                }

            });
            Log.d("TRENDINGS", new Gson().toJson(songs));
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
            rvTrending.setLayoutManager(linearLayoutManager);
            rvTrending.setAdapter(songCardAdapter);
        });

        // fetch genres
        RecyclerView rvGenres = view.findViewById(R.id.rv_genres);
        viewModel.getGenres().observe(getViewLifecycleOwner(), genres -> {
            GenreAdapter genreAdapter = new GenreAdapter(requireContext(), genres, new GenreAdapter.OnGenreClickListener() {
                @Override
                public void onGenreClick(Genre genre) {
                    Toast.makeText(requireContext(), genre.getName(), LENGTH_LONG).show();
                }
            });
            GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 1, GridLayoutManager.HORIZONTAL, false);
            rvGenres.setLayoutManager(gridLayoutManager);
            rvGenres.setAdapter(genreAdapter);
        });

        btnViewAllGenres.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment_container, new AllGenresFragment())
                    .addToBackStack(null)
                    .commit();
        });

        bannerPager = view.findViewById(R.id.banner_pager);
        viewModel.getBanners().observe(getViewLifecycleOwner(), banners -> {
            bannerPager.setAdapter(new BannerAdapter(requireContext(), banners, new BannerAdapter.OnBannerClickListener() {
                @Override
                public void onBannerClick(Banner item) {
                    Toast.makeText(requireContext(), "BANNER CLICKED", LENGTH_LONG).show();
                }
            }));
            autoSlide();
        });


    }
    private void autoSlide(){
        sliderRunnable = () -> {
            if (bannerPager.getAdapter() != null) {
                int itemCount = bannerPager.getAdapter().getItemCount();
                if (itemCount > 0) {
                    int nextItem = (bannerPager.getCurrentItem() + 1) % itemCount;
                    bannerPager.setCurrentItem(nextItem, true);
                }
            }
            sliderHandler.postDelayed(sliderRunnable, 3000); // Lặp lại sau 3s
        };
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    private void stopAutoSlide(){
        if(sliderRunnable != null){
            sliderHandler.removeCallbacks(sliderRunnable);
            sliderRunnable = null;
        }
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        stopAutoSlide();
    }

    private void handleMenuClick(String action){
        switch (action){
            case "play":
                Toast.makeText(requireContext(),"PLAY", LENGTH_SHORT).show();
                break;
            case "like":
                Toast.makeText(requireContext(),"LIKE", LENGTH_SHORT).show();
                break;
            case "playlist":
                Toast.makeText(requireContext(),"PLAYLIST", LENGTH_SHORT).show();
                break;
            case "comment":
                Toast.makeText(requireContext(),"COMMENT", LENGTH_SHORT).show();
                break;
            case "share":
                Toast.makeText(requireContext(),"SHARE", LENGTH_SHORT).show();
                break;
            case "download":
                Toast.makeText(requireContext(),"DOWNLOAD", LENGTH_SHORT).show();
                break;
        }
    }
}
