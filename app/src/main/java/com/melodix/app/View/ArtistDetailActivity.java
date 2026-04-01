package com.melodix.app.View;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.melodix.app.Model.Album;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.View.adapters.SongAdapter;
import com.melodix.app.View.adapters.AlbumAdapter;
import com.melodix.app.View.adapters.ArtistAdapter;
import java.util.ArrayList;

public class ArtistDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ARTIST_ID = "extra_artist_id";
    private AppRepository repository;
    private AlbumAdapter albumAdapter;
    private ArtistAdapter artistAdapter;
    private SongAdapter songAdapter;

    private RecyclerView rvSongs, rvAlbums, rvRelated;
    private TextView tvSongsTitle, tvAlbumsTitle, tvRelatedTitle;
    private TextView tvSeeAllSongs, tvSeeAllAlbums;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_detail);

        repository = AppRepository.getInstance(this);
        String artistId = getIntent().getStringExtra(EXTRA_ARTIST_ID);

        if (artistId == null) {
            finish();
            return;
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Nút tính năng mới
        findViewById(R.id.btn_follow).setOnClickListener(v -> Toast.makeText(this, "Đang phát triển", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_play_all).setOnClickListener(v -> Toast.makeText(this, "Đang phát triển", Toast.LENGTH_SHORT).show());

        tvSongsTitle = findViewById(R.id.tv_songs_title);
        tvAlbumsTitle = findViewById(R.id.tv_albums_title);
        tvRelatedTitle = findViewById(R.id.tv_related_title);
        tvSeeAllSongs = findViewById(R.id.tv_see_all_songs);
        tvSeeAllAlbums = findViewById(R.id.tv_see_all_albums);

        rvSongs = findViewById(R.id.rv_songs);
        rvAlbums = findViewById(R.id.rv_albums);
        rvRelated = findViewById(R.id.rv_related);

        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        rvAlbums.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRelated.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        albumAdapter = new AlbumAdapter(this, new ArrayList<>());
        artistAdapter = new ArtistAdapter(this, new ArrayList<>());
        rvAlbums.setAdapter(albumAdapter);
        rvRelated.setAdapter(artistAdapter);

        songAdapter = new SongAdapter(this, new ArrayList<>(), new SongAdapter.OnSongActionListener() {
            @Override public void onSongClick(Song song, int position) {}
            @Override public void onMenuClick(Song song, int position, String actionId) {}
        });
        rvSongs.setAdapter(songAdapter);

        repository.getArtistByIdAsync(artistId, new AppRepository.ArtistCallback() {
            @Override
            public void onSuccess(Artist artist) {
                if (isFinishing() || isDestroyed()) return;

                ImageView imgAvatar = findViewById(R.id.img_avatar);
                TextView tvName = findViewById(R.id.tv_name);
                TextView tvBio = findViewById(R.id.tv_bio);

                tvName.setText(artist.name);
                Glide.with(ArtistDetailActivity.this).load(artist.avatarRes)
                        .error(android.R.color.darker_gray).circleCrop()
                        .transition(DrawableTransitionOptions.withCrossFade(300)).into(imgAvatar);

                if (TextUtils.isEmpty(artist.bio)) tvBio.setVisibility(View.GONE);
                else { tvBio.setVisibility(View.VISIBLE); tvBio.setText(artist.bio); }

                // ==========================================
                // LẤY BÀI HÁT (ĐÃ FIX LỖI ẨN DANH SÁCH)
                // ==========================================
                repository.getSongsByArtist(artistId, new AppRepository.SongListCallback() {
                    @Override public void onSuccess(ArrayList<Song> songs) {
                        if (isFinishing() || isDestroyed()) return;

                        // Nếu danh sách rỗng, hiện thông báo cho dễ debug
                        if (songs == null || songs.isEmpty()) {
                            tvSongsTitle.setVisibility(View.GONE);
                            rvSongs.setVisibility(View.GONE);
                            tvSeeAllSongs.setVisibility(View.GONE);
                            Toast.makeText(ArtistDetailActivity.this, "Không tìm thấy bài hát nào của nghệ sĩ này", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Nếu có bài hát thì bật hiển thị
                        tvSongsTitle.setVisibility(View.VISIBLE);
                        rvSongs.setVisibility(View.VISIBLE);

                        // Lấy tối đa 10 bài
                        ArrayList<Song> top10Songs = new ArrayList<>(songs.subList(0, Math.min(songs.size(), 10)));
                        songAdapter.update(top10Songs);

                        // Mở nút Xem thêm nếu có trên 10 bài
                        if (songs.size() > 10) {
                            tvSeeAllSongs.setVisibility(View.VISIBLE);
                            tvSeeAllSongs.setOnClickListener(v -> {
                                Intent intent = new Intent(ArtistDetailActivity.this, ArtistSongsActivity.class);
                                intent.putExtra(EXTRA_ARTIST_ID, artistId);
                                startActivity(intent);
                            });
                        } else {
                            tvSeeAllSongs.setVisibility(View.GONE);
                        }
                    }
                    @Override public void onError(String message) {
                        Toast.makeText(ArtistDetailActivity.this, "Lỗi API bài hát: " + message, Toast.LENGTH_SHORT).show();
                    }
                });

                // LẤY ALBUMS
                repository.getAlbumsByArtist(artistId, new AppRepository.AlbumListCallback() {
                    @Override public void onSuccess(ArrayList<Album> albums) {
                        if (isFinishing() || isDestroyed() || albums.isEmpty()) return;
                        tvAlbumsTitle.setVisibility(View.VISIBLE);
                        rvAlbums.setVisibility(View.VISIBLE);
                        albumAdapter.update(albums);

                        // Cứ có album là hiện nút Xem tất cả
                        tvSeeAllAlbums.setVisibility(View.VISIBLE);
                        tvSeeAllAlbums.setOnClickListener(v -> {
                            Intent intent = new Intent(ArtistDetailActivity.this, ArtistAlbumsActivity.class);
                            intent.putExtra(EXTRA_ARTIST_ID, artistId);
                            startActivity(intent);
                        });
                    }
                    @Override public void onError(String message) {}
                });

                // LẤY NGHỆ SĨ LIÊN QUAN
                repository.getRelatedArtists(artistId, new AppRepository.ArtistListCallback() {
                    @Override public void onSuccess(ArrayList<Artist> artists) {
                        if (isFinishing() || isDestroyed() || artists.isEmpty()) return;
                        tvRelatedTitle.setVisibility(View.VISIBLE);
                        rvRelated.setVisibility(View.VISIBLE);
                        artistAdapter.update(artists);
                    }
                    @Override public void onError(String message) {}
                });
            }
            @Override
            public void onError(String message) {
                if (!isFinishing() && !isDestroyed()) finish();
            }
        });
    }
}