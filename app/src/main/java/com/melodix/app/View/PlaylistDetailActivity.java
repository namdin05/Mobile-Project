package com.melodix.app.View;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.melodix.app.Model.Playlist;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.ResourceUtils;

public class PlaylistDetailActivity extends AppCompatActivity {
    public static final String EXTRA_PLAYLIST_ID = "extra_playlist_id";
    private AppRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        repository = AppRepository.getInstance(this);
        String playlistId = getIntent().getStringExtra(EXTRA_PLAYLIST_ID);

        // Do bản AppRepository tinh gọn không có hàm getPlaylistById,
        // ta sẽ quét qua danh sách playlist hiện tại để tìm.
        Playlist playlist = null;
        for (Playlist p : repository.getCurrentUserPlaylists()) {
            if (p.id.equals(playlistId)) {
                playlist = p;
                break;
            }
        }

        if (playlist == null) {
            finish();
            return;
        }

        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageView cover = findViewById(R.id.img_cover);
        TextView title = findViewById(R.id.tv_title);
        TextView meta = findViewById(R.id.tv_meta);

        // Gắn sự kiện nút Back
        btnBack.setOnClickListener(v -> finish());

        // Hiển thị thông tin cơ bản lấy được từ kết quả tìm kiếm
        cover.setImageResource(ResourceUtils.anyDrawable(this, playlist.coverRes));
        title.setText(playlist.name);
        meta.setText("Mở từ kết quả tìm kiếm");

        // Các nút như btn_more, nút add bài hát và RecyclerView chứa danh sách bài hát
        // tạm thời không xử lý vì chúng ta đang tập trung 100% vào Search.
    }
}