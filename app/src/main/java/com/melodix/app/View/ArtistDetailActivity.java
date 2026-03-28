package com.melodix.app.View;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.melodix.app.Model.Artist;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.ResourceUtils;

public class ArtistDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ARTIST_ID = "extra_artist_id";
    private AppRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_detail);

        repository = AppRepository.getInstance(this);
        String artistId = getIntent().getStringExtra(EXTRA_ARTIST_ID);

        Artist artist = repository.getArtistById(artistId);
        if (artist == null) {
            finish();
            return;
        }

        // Hiển thị thông tin cơ bản
        ((ImageView) findViewById(R.id.img_avatar)).setImageResource(ResourceUtils.anyDrawable(this, artist.avatarRes));
        ((TextView) findViewById(R.id.tv_name)).setText(artist.name);
        ((TextView) findViewById(R.id.tv_bio)).setText(artist.bio);

        // Nút quay lại
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Ẩn nút Follow vì ta đã lược bỏ chức năng này
        android.view.View followButton = findViewById(R.id.btn_follow);
        if (followButton != null) {
            followButton.setVisibility(android.view.View.GONE);
        }

        // Tạm thời vô hiệu hóa các danh sách Bài hát (rv_songs), Album (rv_albums),
        // Nghệ sĩ liên quan (rv_related) để luồng Search chạy mượt mà nhất.
    }
}