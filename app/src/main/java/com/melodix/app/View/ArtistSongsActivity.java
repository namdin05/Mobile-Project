package com.melodix.app.View;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.View.adapters.SongAdapter;
import java.util.ArrayList;

public class ArtistSongsActivity extends AppCompatActivity {
    private SongAdapter songAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_songs);

        String artistId = getIntent().getStringExtra(ArtistDetailActivity.EXTRA_ARTIST_ID);
        if (artistId == null) { finish(); return; }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_play_all_songs).setOnClickListener(v ->
                Toast.makeText(this, "Đang phát triển", Toast.LENGTH_SHORT).show());

        RecyclerView rvAllSongs = findViewById(R.id.rv_all_songs);
        rvAllSongs.setLayoutManager(new LinearLayoutManager(this));

        songAdapter = new SongAdapter(this, new ArrayList<>(), new SongAdapter.OnSongActionListener() {
            @Override public void onSongClick(Song song, int position) {}
            @Override public void onMenuClick(Song song, int position, String actionId) {}
        });
        rvAllSongs.setAdapter(songAdapter);

        AppRepository.getInstance(this).getSongsByArtist(artistId, new AppRepository.SongListCallback() {
            @Override public void onSuccess(ArrayList<Song> songs) {
                if (!isFinishing() && !isDestroyed()) songAdapter.update(songs);
            }
            @Override public void onError(String message) {}
        });
    }
}