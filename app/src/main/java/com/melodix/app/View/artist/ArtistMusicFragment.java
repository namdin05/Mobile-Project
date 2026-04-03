package com.melodix.app.View.artist;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.melodix.app.R;

public class ArtistMusicFragment extends Fragment {

    private RecyclerView rvSongs;
    private FloatingActionButton fabAddSong;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artist_music, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvSongs = view.findViewById(R.id.rv_artist_songs);
        fabAddSong = view.findViewById(R.id.fab_add_song);

        // Bắt sự kiện click nút thêm bài hát
        fabAddSong.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), UploadSongActivity.class);
            startActivity(intent);            // Lát nữa chúng ta sẽ gọi Intent mở UploadActivity ở đây
        });
    }
}