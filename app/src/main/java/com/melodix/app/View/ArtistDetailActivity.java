package com.melodix.app.View;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.melodix.app.Model.Artist;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;

public class ArtistDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ARTIST_ID = "extra_artist_id";
    private AppRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_detail);

        repository = AppRepository.getInstance(this);
        String artistId = getIntent().getStringExtra(EXTRA_ARTIST_ID);

        if (artistId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID Nghệ sĩ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Nút quay lại
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // GỌI API LẤY CHI TIẾT NGHỆ SĨ (Async Mode)
        repository.getArtistByIdAsync(artistId, new AppRepository.ArtistCallback() {
            @Override
            public void onSuccess(Artist artist) {
                if (isFinishing() || isDestroyed()) return;

                ImageView imgAvatar = findViewById(R.id.img_avatar);
                TextView tvName = findViewById(R.id.tv_name);
                TextView tvBio = findViewById(R.id.tv_bio);

                // Điền tên
                tvName.setText(artist.name);

                // UX Đỉnh cao: Glide tự động cắt hình tròn (circleCrop) và load mượt mà
                Glide.with(ArtistDetailActivity.this)
                        .load(artist.avatarRes)
                        .circleCrop() // Ép thành hình tròn hoàn hảo kiểu Spotify
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(imgAvatar);

                // Xử lý Tiểu sử (Bio): Nếu không có thì tàng hình luôn
                if (TextUtils.isEmpty(artist.bio)) {
                    tvBio.setVisibility(View.GONE);
                } else {
                    tvBio.setVisibility(View.VISIBLE);
                    tvBio.setText(artist.bio);
                }

                // Tạm thời các danh sách Songs/Albums/Related đang bị ẩn trong XML (visibility="gone")
                // Nếu sau này bạn làm tính năng lấy bài hát của Artist, chỉ cần gọi API ở đây
                // và setVisibility(View.VISIBLE) cho rv_songs và tv_songs_title là xong!
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ArtistDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}