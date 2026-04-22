package com.melodix.app.View;

import static android.widget.Toast.LENGTH_SHORT;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.gson.Gson;
import com.melodix.app.Model.Album;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.Song;
import com.melodix.app.PlayerActivity;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.ShareUtils;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.View.adapters.SongAdapter;
import com.melodix.app.View.adapters.AlbumAdapter;
import com.melodix.app.View.adapters.ArtistAdapter;
import com.melodix.app.Utils.FollowManager;
import java.util.ArrayList;

public class ArtistDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ARTIST_ID = "extra_artist_id";
    private AppRepository repository;
    private AlbumAdapter albumAdapter;
    private ArtistAdapter artistAdapter;
    private SongAdapter songAdapter;
    private FollowManager followManager;

    private RecyclerView rvSongs, rvAlbums, rvRelated;
    private TextView tvSongsTitle, tvAlbumsTitle, tvRelatedTitle;
    private TextView tvSeeAllSongs, tvSeeAllAlbums;

    // THÊM: Khai báo MiniPlayerController
    private com.melodix.app.Model.MiniPlayerController miniPlayerController;

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

        ImageView btnShare = findViewById(R.id.btn_share);
        btnShare.setOnClickListener(v -> {
            if(repository != null){
                TextView tvName = findViewById(R.id.tv_name);
                String artistName = tvName.getText().toString();
                com.melodix.app.Utils.ShareUtils.shareContent(
                        ArtistDetailActivity.this,
                        "profile",
                        artistId,
                        artistName
                );
            }else{
                Toast.makeText(ArtistDetailActivity.this, "Vui lòng đợi tải xong thông tin nghệ sĩ", Toast.LENGTH_SHORT).show();
            }
        });

//        findViewById(R.id.btn_follow).setOnClickListener(v -> Toast.makeText(this, "Đang phát triển", Toast.LENGTH_SHORT).show());

        // ==========================================
        // KÍCH HOẠT NÚT "PHÁT TẤT CẢ"
        // ==========================================
        findViewById(R.id.btn_play_all).setOnClickListener(v -> {
            if (songAdapter != null && songAdapter.getSongs() != null && !songAdapter.getSongs().isEmpty()) {
                playSongAndSetQueue(songAdapter.getSongs().get(0), songAdapter.getSongs());
            } else {
                Toast.makeText(this, "Chưa có bài hát để phát", Toast.LENGTH_SHORT).show();
            }
        });

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

        // ==========================================
        // BẮT SỰ KIỆN CLICK BÀI HÁT
        // ==========================================
        songAdapter = new SongAdapter(this, repository.getAllApprovedSongs(), new SongAdapter.OnSongActionListener() {
            @Override
            public void onSongClick(Song song, int position) {
                playSongAndSetQueue(song, songAdapter.getSongs());
            }
            @Override
            public void onMenuClick(Song song, int position, String actionId) {
                handleMenuClick(song, actionId);
            }
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

                TextView tvFollowerCount = findViewById(R.id.tv_follower_count);

                repository.getFollowerCount(artistId, count -> {
                    if (isFinishing() || isDestroyed()) return;
                    tvFollowerCount.setVisibility(View.VISIBLE);
                    String displayCount = count >= 1000 ?
                            String.format(java.util.Locale.US, "%.1fK", count / 1000f) :
                            String.valueOf(count);
                    tvFollowerCount.setText(displayCount + " follower(s)");
                });

                // LẤY BÀI HÁT
                repository.getSongsByArtist(artistId, new AppRepository.SongListCallback() {
                    @Override public void onSuccess(ArrayList<Song> songs) {
                        if (isFinishing() || isDestroyed()) return;

                        if (songs == null || songs.isEmpty()) {
                            tvSongsTitle.setVisibility(View.GONE);
                            rvSongs.setVisibility(View.GONE);
                            tvSeeAllSongs.setVisibility(View.GONE);
                            return;
                        }

                        tvSongsTitle.setVisibility(View.VISIBLE);
                        rvSongs.setVisibility(View.VISIBLE);

                        ArrayList<Song> top10Songs = new ArrayList<>(songs.subList(0, Math.min(songs.size(), 10)));
                        songAdapter.update(top10Songs);

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

                        tvSeeAllAlbums.setVisibility(View.VISIBLE);
                        tvSeeAllAlbums.setOnClickListener(v -> {
                            Intent intent = new Intent(ArtistDetailActivity.this, ArtistAlbumsActivity.class);
                            intent.putExtra(EXTRA_ARTIST_ID, artistId);
                            startActivity(intent);
                        });
                    }
                    @Override public void onError(String message) {}
                });
                //LẤY SỐ LƯỢNG FOLLOW
                repository.getFollowerCount(artistId, count -> {
                    if (isFinishing() || isDestroyed()) return;
                    tvFollowerCount.setVisibility(View.VISIBLE);
                    String displayCount = count >= 1000 ?
                            String.format(java.util.Locale.US, "%.1fK", count / 1000f) :
                            String.valueOf(count);
                    tvFollowerCount.setText(displayCount + " follower(s)");

                    if (followManager != null) followManager.setFollowerCount(count);
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
        // Khởi tạo FollowManager
        Button btnFollow = findViewById(R.id.btn_follow);
        TextView tvFollowerCountView = findViewById(R.id.tv_follower_count);
        followManager = new FollowManager(this, artistId, btnFollow, tvFollowerCountView);
        followManager.init();
        // THÊM: Khởi tạo MiniPlayer Controller
        miniPlayerController = new com.melodix.app.Model.MiniPlayerController(this);
    }

    private void playSongAndSetQueue(Song selectedSong, java.util.List<Song> currentList) {
        PlaybackUtils.playSong(this, (ArrayList<Song>) currentList, selectedSong.getId());
    }

    private void handleMenuClick(Song song, String action){
        switch (action){
            case "play":
                java.util.List<Song> singleList = new ArrayList<>();
                singleList.add(song);
                playSongAndSetQueue(song, singleList);
                break;
            case "like":
                Toast.makeText(this,"Đã thích " + song.getTitle(), LENGTH_SHORT).show();
                break;
            case "playlist":
                Toast.makeText(this,"Thêm " + song.getTitle() + " vào Playlist", LENGTH_SHORT).show();
                break;
            case "comment":
                Toast.makeText(this,"Bình luận về " + song.getTitle(), LENGTH_SHORT).show();
                break;
            case "share":
                if (song != null && song.getId() != null) {
                    com.melodix.app.Utils.ShareUtils.shareContent(
                            ArtistDetailActivity.this,
                            "song",
                            song.getId(),
                            song.getTitle()
                    );
                } else {
                    Toast.makeText(ArtistDetailActivity.this, "Lỗi dữ liệu bài hát", Toast.LENGTH_SHORT).show();
                }
                break;
            case "download":
                Toast.makeText(this,"Tải xuống " + song.getTitle(), LENGTH_SHORT).show();
                break;
        }
    }

    // THÊM: Quản lý vòng đời của Mini Player (Đánh thức và Ngủ đông)
    @Override
    protected void onResume() {
        super.onResume();
        if (miniPlayerController != null) {
            miniPlayerController.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miniPlayerController != null) {
            miniPlayerController.onPause();
        }
    }
}