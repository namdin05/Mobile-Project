package com.melodix.app.View.artist;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.melodix.app.R;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Service.StorageAPIService;
import com.melodix.app.View.dialogs.AlbumSelectionDialog;
import com.melodix.app.View.dialogs.CollabSelectionDialog;
import com.melodix.app.View.dialogs.GenreSelectionDialog;
import com.melodix.app.ViewModel.UploadSongViewModel;
import java.util.ArrayList;
import java.util.List;


public class UploadSongActivity extends AppCompatActivity {

    private EditText edtSongTitle;
    private View btnPickCover, btnPickAudio;
    private Button btnSubmitUpload;
    private View btnAddCollab;

    private View layoutCoverPlaceholder;
    private android.widget.ImageView imgCoverPreview;
    private TextView tvAudioTitle;
    private android.widget.ImageView imgAudioIcon;
    private TextView tvAudioStatus;

    private Uri coverUri = null;
    private Uri audioUri = null;
    private ArtistAPIService apiService;
    private StorageAPIService storageService; // THÊM STORAGE SERVICE
    private com.google.android.material.chip.ChipGroup chipGroupCollab;

    private View btnSelectAlbum, btnAddGenre;
    private TextView tvSelectedAlbum;
    private com.google.android.material.chip.ChipGroup chipGroupGenre;

    private String selectedAlbumId = null;
    private final List<Integer> selectedGenreIds = new ArrayList<>();
    private final List<String> selectedArtistIds = new ArrayList<>();
    private UploadSongViewModel uploadViewModel;
    private View btnPickLyric;
    private TextView tvLyricTitle, tvLyricStatus;
    private android.widget.ImageView imgLyricIcon;
    private Uri lyricUri = null; // Biến chứa file lời bài hát
    private boolean isEditMode = false;
    private String editSongId = null;
    private String existingCoverUrl = null;

    // ĐÃ SỬA: Dùng String để lưu currentUserId lấy từ SharedPreferences
    private String currentUserId;

    private final ActivityResultLauncher<String> pickCoverLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    coverUri = uri;
                    imgCoverPreview.setVisibility(View.VISIBLE);
                    imgCoverPreview.setImageURI(uri);
                    layoutCoverPlaceholder.setVisibility(View.GONE);
                }
            }
    );
    private final ActivityResultLauncher<String> pickLyricLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    lyricUri = uri;
                    tvLyricTitle.setText("Đã đính kèm lời bài hát");
                    tvLyricTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white));
                    tvLyricStatus.setText("Sẵn sàng đồng bộ Karaoke");
                    tvLyricStatus.setTextColor(android.graphics.Color.parseColor("#1DB954"));
                    imgLyricIcon.setColorFilter(android.graphics.Color.parseColor("#1DB954"));
                }
            }
    );
    private final ActivityResultLauncher<String> pickAudioLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    audioUri = uri;
                    tvAudioTitle.setText("Đã đính kèm âm thanh mới");
                    tvAudioTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white));
                    tvAudioStatus.setText("Sẵn sàng để phát hành");
                    tvAudioStatus.setTextColor(android.graphics.Color.parseColor("#1DB954"));
                    imgAudioIcon.setColorFilter(android.graphics.Color.parseColor("#1DB954"));
                }
            }
    );

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_song);

        // ĐÃ SỬA: Lấy USER_ID từ SharedPreferences thay vì SessionManager
        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("USER_ID", null);

        View btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // ĐÃ SỬA: Dùng currentUserId
        if (currentUserId != null) {
            selectedArtistIds.add(currentUserId);
        }

        edtSongTitle = findViewById(R.id.edt_song_title);
        btnPickCover = findViewById(R.id.btn_pick_cover);
        btnPickAudio = findViewById(R.id.btn_pick_audio);
        btnSubmitUpload = findViewById(R.id.btn_submit_upload);
        btnAddCollab = findViewById(R.id.btn_add_collab);
        chipGroupCollab = findViewById(R.id.chip_group_collab);

        layoutCoverPlaceholder = findViewById(R.id.layout_cover_placeholder);
        imgCoverPreview = findViewById(R.id.img_cover_preview);
        tvAudioTitle = findViewById(R.id.tv_audio_title);
        imgAudioIcon = findViewById(R.id.img_audio_icon);
        tvAudioStatus = findViewById(R.id.tv_audio_status);

        btnSelectAlbum = findViewById(R.id.btn_select_album);
        tvSelectedAlbum = findViewById(R.id.tv_selected_album);
        btnAddGenre = findViewById(R.id.btn_add_genre);
        chipGroupGenre = findViewById(R.id.chip_group_genre);

        btnPickLyric = findViewById(R.id.btn_pick_lyric);
        tvLyricTitle = findViewById(R.id.tv_lyric_title);
        tvLyricStatus = findViewById(R.id.tv_lyric_status);
        imgLyricIcon = findViewById(R.id.img_lyric_icon);

        // Bấm nút mở thư mục
        btnPickLyric.setOnClickListener(v -> pickLyricLauncher.launch("*/*"));

        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);
        if (isEditMode) {
            editSongId = getIntent().getStringExtra("EDIT_SONG_ID");
            String oldTitle = getIntent().getStringExtra("EDIT_SONG_TITLE");
            existingCoverUrl = getIntent().getStringExtra("EDIT_SONG_COVER");

            edtSongTitle.setText(oldTitle);
            btnSubmitUpload.setText("CẬP NHẬT TÁC PHẨM");

            if (existingCoverUrl != null && !existingCoverUrl.isEmpty()) {
                layoutCoverPlaceholder.setVisibility(View.GONE);
                imgCoverPreview.setVisibility(View.VISIBLE);
                com.bumptech.glide.Glide.with(this).load(existingCoverUrl).into(imgCoverPreview);
            }

            tvAudioTitle.setText("Original audio attached");
            tvAudioStatus.setText("Select only if you want to replace the current MP3");
        }

        btnSelectAlbum.setOnClickListener(v -> {
            // Gọi class vừa tách ra, truyền Context, UserID và cái phễu (Listener) để hứng dữ liệu
            AlbumSelectionDialog dialog = new AlbumSelectionDialog(this, currentUserId, (albumId, albumTitle) -> {
                // Hứng dữ liệu khi người dùng bấm chọn
                selectedAlbumId = albumId;
                tvSelectedAlbum.setText(albumTitle);
            });
            dialog.show(); // Bật dialog lên
        });
        btnAddGenre.setOnClickListener(v -> {
            // Gọi class Dialog vừa tạo
            GenreSelectionDialog dialog = new GenreSelectionDialog(this, (genreId, genreName) -> {
                // Hứng dữ liệu trả về và xử lý logic UI cũ
                if (!selectedGenreIds.contains(genreId)) {
                    selectedGenreIds.add(genreId);
                    addGenreChip(genreId, genreName); // Vẽ cái Chip lên màn hình
                }
            });
            dialog.show();
        });

        btnAddCollab.setOnClickListener(v -> {
            // Gọi class CollabSelectionDialog vừa dọn ra
            CollabSelectionDialog dialog = new CollabSelectionDialog(this, (artistId, artistName) -> {
                // Hứng dữ liệu và vẽ cái Chip Nghệ sĩ lên màn hình
                addArtistChip(artistId, artistName);
            });
            dialog.show();
        });

        // Khai báo biến ở đầu class

        // Trong hàm onCreate(), nhét đoạn này vào:
        uploadViewModel = new androidx.lifecycle.ViewModelProvider(this).get(com.melodix.app.ViewModel.UploadSongViewModel.class);

        // Lắng nghe trạng thái chữ (Đang up ảnh, đang up nhạc...)
        uploadViewModel.getUploadStatus().observe(this, status -> {
            btnSubmitUpload.setText(status);
        });

        // Lắng nghe lỗi
        uploadViewModel.getErrorMessage().observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            btnSubmitUpload.setEnabled(true);
            btnSubmitUpload.setText(isEditMode ? "CẬP NHẬT TÁC PHẨM" : "PHÁT HÀNH TÁC PHẨM");
        });

        // Lắng nghe thành công
        uploadViewModel.getUploadSuccess().observe(this, isSuccess -> {
            if (isSuccess != null && isSuccess) {
                Toast.makeText(this, isEditMode ? "Cập nhật thành công!" : "Tải lên hoàn tất! Bài hát đang chờ duyệt.", Toast.LENGTH_LONG).show();
                if (isEditMode) {
                    finish();
                } else {
                    resetForm();
                }
            }
        });

        btnSubmitUpload.setOnClickListener(v -> {
            String songTitle = edtSongTitle.getText().toString().trim();

            if (songTitle.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên bài hát!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isEditMode && selectedGenreIds.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 thể loại!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isEditMode && (coverUri == null || audioUri == null)) {
                Toast.makeText(this, "Vui lòng chọn ảnh bìa và file nhạc!", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmitUpload.setEnabled(false); // Khóa nút chống spam

            // Chuyền bóng cho ViewModel xử lý ngầm
            // Nhớ chêm thêm thằng lyricUri vào nhen sếp
            uploadViewModel.startUploadProcess(songTitle, coverUri, audioUri, lyricUri, existingCoverUrl,
                    isEditMode, editSongId, selectedAlbumId,
                    selectedGenreIds, selectedArtistIds);
        });
        // 👇 THÊM LẠI 2 DÒNG NÀY ĐỂ BẤM ĐƯỢC NÚT CHỌN FILE NHÉ SẾP 👇
        btnPickCover.setOnClickListener(v -> pickCoverLauncher.launch("image/*"));
        btnPickAudio.setOnClickListener(v -> pickAudioLauncher.launch("audio/*"));


    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void addGenreChip(int genreId, String genreName) {
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
        chip.setText(genreName);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.mdx_primary);
        chip.setTextColor(getResources().getColor(android.R.color.white));

        chip.setOnCloseIconClickListener(v -> {
            chipGroupGenre.removeView(chip);
            selectedGenreIds.remove(Integer.valueOf(genreId));
        });

        chipGroupGenre.addView(chip);
    }

    private void addArtistChip(String artistId, String artistName) {
        if (selectedArtistIds.contains(artistId)) {
            Toast.makeText(this, "Nghệ sĩ này đã được thêm rồi!", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedArtistIds.add(artistId);

        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
        chip.setText(artistName);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.mdx_surface);
        chip.setTextColor(getResources().getColor(android.R.color.black));

        chip.setOnCloseIconClickListener(v -> {
            chipGroupCollab.removeView(chip);
            selectedArtistIds.remove(artistId);
        });

        chipGroupCollab.addView(chip);
    }
    private void resetForm() {
        btnSubmitUpload.setEnabled(true);
        btnSubmitUpload.setText("PHÁT HÀNH TÁC PHẨM");

        edtSongTitle.setText("");

        coverUri = null;
        imgCoverPreview.setVisibility(View.GONE);
        layoutCoverPlaceholder.setVisibility(View.VISIBLE);

        audioUri = null;
        tvAudioTitle.setText("Đính kèm file âm thanh");
        tvAudioTitle.setTextColor(android.graphics.Color.WHITE);
        tvAudioStatus.setText("Hỗ trợ định dạng MP3, WAV...");
        tvAudioStatus.setTextColor(android.graphics.Color.GRAY);
        imgAudioIcon.setColorFilter(android.graphics.Color.WHITE);

        selectedAlbumId = null;
        tvSelectedAlbum.setText("Single (Không thuộc album nào)");

        selectedGenreIds.clear();
        chipGroupGenre.removeAllViews();

        selectedArtistIds.clear();
        chipGroupCollab.removeAllViews();

        // ĐÃ SỬA: Dùng currentUserId
        if (currentUserId != null) {
            selectedArtistIds.add(currentUserId);
        }
    }
}
