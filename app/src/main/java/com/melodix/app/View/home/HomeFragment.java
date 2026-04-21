package com.melodix.app.View.home;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.melodix.app.Model.Banner;
import com.melodix.app.Model.Genre;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.PlayerActivity;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.Utils.ShareUtils;
import com.melodix.app.View.adapters.BannerAdapter;
import com.melodix.app.View.adapters.GenreAdapter;
import com.melodix.app.View.adapters.SongCardAdapter;
import com.melodix.app.View.adapters.SongAdapter;
import com.melodix.app.ViewModel.HomeViewModel;
import com.melodix.app.View.dialogs.PlaylistSelectionDialog;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    ViewPager2 bannerPager;
    private android.os.Handler sliderHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable sliderRunnable;
    private ImageView avatar;
    private TextView greeting;

    // logic
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // khai bao ViewModel
        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        // danh sach bai hat moi nhat
        RecyclerView rvNewRelease = view.findViewById(R.id.rv_new_releases);
        // danh sach bai hat thinh hanh
        RecyclerView rvTrending = view.findViewById(R.id.rv_trending);

        // ==========================================
        // FETCH NEW RELEASE
        // ==========================================
        // goi API lay danh sach bai hat moi nhat
        viewModel.getNewReleases().observe(getViewLifecycleOwner(), songs -> {
            // goi song apdapter
            SongAdapter songAdapter = new SongAdapter(requireContext(), songs, new SongAdapter.OnSongActionListener() {
                @Override
                public void onSongClick(Song song, int position) {
                    playSongAndSetQueue(song, songs); // Gọi hàm phát nhạc
                }
                @Override
                public void onMenuClick(Song song, int position, String action){
                    // ĐÃ SỬA LỖI MENU 3 CHẤM: Truyền nguyên danh sách 'songs' vào
                    handleMenuClick(song, action, songs);
                }
            });
            Log.d("NEW_RELEASE_SONG", new Gson().toJson(songs));
            // dinh nghia layout
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
            rvNewRelease.setLayoutManager(linearLayoutManager);
            rvNewRelease.setAdapter(songAdapter);
        });

        // ĐÃ SỬA: Lấy thông tin user từ SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false);
        String avatarUrl = prefs.getString("USER_AVATAR", "");
        String userName = prefs.getString("USER_NAME", "");

        if(isLoggedIn)  Log.d("get_session_user", userName + " hello");
        else Log.d("get_session_user", "chua load xong user");

        // khai bao account section
        avatar = view.findViewById(R.id.img_avatar);
        greeting = view.findViewById(R.id.tv_greeting);
        TextView subGreeting = view.findViewById(R.id.tv_subgreeting);
        TextView btnViewAllGenres = view.findViewById(R.id.tv_view_all_genres);
        // ========

        // Cập nhật Avatar nếu đã đăng nhập
        if (isLoggedIn && !avatarUrl.isEmpty()) {
            Glide.with(requireContext()).load(avatarUrl).circleCrop().into(avatar);
            greeting.setText("Welcome back");
        }

        // tim den bottomNavigationView cua MainActivity
        BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottom_nav);
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomNavigationView.setSelectedItemId(R.id.nav_account);
            }
        });

        // goi API lay danh sach bai hat trending
        viewModel.getTrendingSongs().observe(getViewLifecycleOwner(), songs->{
            // goi song card adapter
            SongCardAdapter songCardAdapter = new SongCardAdapter(requireContext(), songs, true, new SongCardAdapter.OnSongClickListener() {
                @Override
                public void onSongClick(Song song) {
                    playSongAndSetQueue(song, songs); // Gọi hàm phát nhạc
                }
            });
            Log.d("TRENDINGS", new Gson().toJson(songs));
            // set layout
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
            rvTrending.setLayoutManager(linearLayoutManager);
            rvTrending.setAdapter(songCardAdapter);
        });

        // lay danh sach the loai
        RecyclerView rvGenres = view.findViewById(R.id.rv_genres);
        viewModel.getGenres().observe(getViewLifecycleOwner(), genres -> {
            // set adpater
            GenreAdapter genreAdapter = new GenreAdapter(requireContext(), genres, new GenreAdapter.OnGenreClickListener() {
                @Override
                public void onGenreClick(Genre genre) {
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right)
                            .add(R.id.main_fragment_container, GenreDetailFragment.newInstance(genre.getId(), genre.getName()))
                            .addToBackStack(null)
                            .commit();
                }
            });
            // set layout
            GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 1, GridLayoutManager.HORIZONTAL, false);
            rvGenres.setLayoutManager(gridLayoutManager);
            rvGenres.setAdapter(genreAdapter);
        });

        // xu li logic view all genres
        btnViewAllGenres.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right)
                    .add(R.id.main_fragment_container, new AllGenresFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // khai bao banner
        bannerPager = view.findViewById(R.id.banner_pager);

        // goi API lay danh sach banners
        viewModel.getBanners().observe(getViewLifecycleOwner(), banners -> {
            // set adapter
            bannerPager.setAdapter(new BannerAdapter(requireContext(), banners, new BannerAdapter.OnBannerClickListener() {
                @Override
                public void onBannerClick(Banner item) {
                    Toast.makeText(requireContext(), "BANNER CLICKED", LENGTH_LONG).show();
                }
            }));
            autoSlide();
        });
    }

    private void playSongAndSetQueue(Song selectedSong, List<Song> currentList) {
        PlaybackUtils.playSong(requireContext(), (ArrayList<Song>) currentList, selectedSong.getId());
    }

    // ham chay banners
    private void autoSlide(){
        sliderRunnable = () -> {
            if (bannerPager.getAdapter() != null) {
                int itemCount = bannerPager.getAdapter().getItemCount();
                if (itemCount > 0) {
                    int nextItem = (bannerPager.getCurrentItem() + 1) % itemCount;
                    bannerPager.setCurrentItem(nextItem, true);
                }
            }
            // 2. Tự động lấy User mới nhất và cập nhật Avatar mỗi 3 giây
            if (isAdded() && getContext() != null) { // Đảm bảo Fragment chưa bị đóng
                SharedPreferences prefs = requireContext().getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
                boolean isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false);
                String currentAvatarUrl = prefs.getString("USER_AVATAR", "");

                if (isLoggedIn && !currentAvatarUrl.isEmpty() && avatar != null) {
                    Glide.with(this)
                            .load(currentAvatarUrl)
                            .circleCrop()
                            .into(avatar);
                    if (greeting != null) greeting.setText("Welcome back");
                }
            }
            sliderHandler.postDelayed(sliderRunnable, 3000); // Lặp lại sau 3s
        };
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    // ham dung chay banners
    private void stopAutoSlide(){
        if(sliderRunnable != null){
            sliderHandler.removeCallbacks(sliderRunnable);
            sliderRunnable = null;
        }
    }

    // onCreateView chi de bom giao dien
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        stopAutoSlide(); // dung auto slide tranh memory leak
    }

    private void showPlaylistSelectionDialog(Song song) {
        SharedPreferences prefs = requireContext().getSharedPreferences("MelodixPrefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false);

        if (!isLoggedIn) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để thêm vào playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        PlaylistSelectionDialog dialog = PlaylistSelectionDialog.newInstance(song.getId());
        dialog.setOnPlaylistActionListener(() -> {
            Toast.makeText(requireContext(), "Playlist đã được cập nhật", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getChildFragmentManager(), "playlist_selection");
    }

    // ĐÃ SỬA: Nhận listSongs để nạp toàn bộ Queue khi người dùng bấm Play từ Menu
    private void handleMenuClick(Song song, String action, List<Song> listSongs){
        switch (action){
            case "play":
                // Truyền nguyên danh sách để nghe được bài tiếp theo
                playSongAndSetQueue(song, listSongs);
                break;
            case "like":
                Toast.makeText(requireContext(),"LIKE " + song.getTitle(), LENGTH_SHORT).show();
                break;
            case "playlist":
                showPlaylistSelectionDialog(song);
                break;
            case "comment":
                Toast.makeText(requireContext(),"COMMENT " + song.getTitle(), LENGTH_SHORT).show();
                break;
            case "share":
                if (song != null && song.getId() != null) {
                    com.melodix.app.Utils.ShareUtils.shareContent(
                            requireContext(),
                            "song",
                            song.getId(),
                            song.getTitle()
                    );
                } else {
                    Toast.makeText(requireContext(), "Lỗi dữ liệu bài hát", Toast.LENGTH_SHORT).show();
                }
                break;
            case "download":
                Toast.makeText(requireContext(),"DOWNLOAD " + song.getTitle(), LENGTH_SHORT).show();
                break;
        }
    }
}