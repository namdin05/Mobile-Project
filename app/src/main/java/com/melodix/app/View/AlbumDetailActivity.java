package com.melodix.app.View;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.melodix.app.Model.Album;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.ResourceUtils;

public class AlbumDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ALBUM_ID = "extra_album_id";
    private AppRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        repository = AppRepository.getInstance(this);
        String albumId = getIntent().getStringExtra(EXTRA_ALBUM_ID);

        // Gọi hàm lấy Album (Lưu ý xem bên dưới)
        Album album = repository.getAlbumById(albumId);
        if (album == null) {
            finish();
            return;
        }

        // Hiển thị thông tin cơ bản
        ((ImageView) findViewById(R.id.img_cover)).setImageResource(ResourceUtils.anyDrawable(this, album.coverRes));
        ((TextView) findViewById(R.id.tv_title)).setText(album.title);
        ((TextView) findViewById(R.id.tv_subtitle)).setText(album.artistName + " • " + album.year);
        ((TextView) findViewById(R.id.tv_description)).setText(album.description);

        // Nút quay lại
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Tạm thời vô hiệu hóa nút Share và danh sách bài hát (RecyclerView)
        // để luồng Search hoạt động trơn tru mà không bị lỗi thiếu hàm.
    }
}