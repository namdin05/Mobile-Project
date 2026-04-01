package com.melodix.app.View;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.melodix.app.Model.Album;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.View.adapters.AlbumAdapter;
import java.util.ArrayList;

public class ArtistAlbumsActivity extends AppCompatActivity {
    private AlbumAdapter albumAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_albums);

        String artistId = getIntent().getStringExtra(ArtistDetailActivity.EXTRA_ARTIST_ID);
        if (artistId == null) { finish(); return; }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rvAllAlbums = findViewById(R.id.rv_all_albums);

        // Setup Grid 2 cột
        rvAllAlbums.setLayoutManager(new GridLayoutManager(this, 2));

        albumAdapter = new AlbumAdapter(this, new ArrayList<>());
        rvAllAlbums.setAdapter(albumAdapter);

        AppRepository.getInstance(this).getAlbumsByArtist(artistId, new AppRepository.AlbumListCallback() {
            @Override public void onSuccess(ArrayList<Album> albums) {
                if (!isFinishing() && !isDestroyed()) albumAdapter.update(albums);
            }
            @Override public void onError(String message) {}
        });
    }
}