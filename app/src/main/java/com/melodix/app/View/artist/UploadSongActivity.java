package com.melodix.app.View.artist;

import android.content.Intent;
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

import com.melodix.app.BuildConfig;
import com.melodix.app.Model.SongRequestUpload;
import com.melodix.app.R;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Model.SessionManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadSongActivity extends AppCompatActivity {

    private EditText edtSongTitle;
    private View btnPickCover, btnPickAudio; // Đổi sang View để ép kiểu được cả CardView
    private Button btnSubmitUpload;
    private View btnAddCollab; // Đổi sang View vì dùng ImageButton trong XML

    // Thêm các biến quản lý UI Premium
    private View layoutCoverPlaceholder;
    private android.widget.ImageView imgCoverPreview;
    private TextView tvAudioTitle;
    private android.widget.ImageView imgAudioIcon;
    private TextView tvAudioStatus;

    private Uri coverUri = null;
    private Uri audioUri = null;

    private ArtistAPIService apiService;
    private SessionManager sessionManager;
    private com.google.android.material.chip.ChipGroup chipGroupCollab;

    private List<String> selectedArtistIds = new ArrayList<>();

    // 1. CÔNG CỤ CHỌN FILE TỪ ĐIỆN THOẠI (Cập nhật để đổi UI khi chọn xong)
    private final ActivityResultLauncher<String> pickCoverLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    coverUri = uri;
                    // Bật ảnh Preview và tắt cái placeholder (icon máy ảnh) đi
                    imgCoverPreview.setVisibility(View.VISIBLE);
                    imgCoverPreview.setImageURI(uri);
                    layoutCoverPlaceholder.setVisibility(View.GONE);
                }
            }
    );

    private final ActivityResultLauncher<String> pickAudioLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    audioUri = uri;
                    // Đổi màu giao diện báo hiệu đã có file âm thanh
                    tvAudioTitle.setText("Đã đính kèm âm thanh");
                    tvAudioTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white));
                    tvAudioStatus.setText("Sẵn sàng để phát hành");
                    tvAudioStatus.setTextColor(android.graphics.Color.parseColor("#1DB954"));
                    imgAudioIcon.setColorFilter(android.graphics.Color.parseColor("#1DB954"));
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_song);

        // Khởi tạo
        apiService = RetrofitClient.getClient().create(ArtistAPIService.class);
        sessionManager = SessionManager.getInstance(this);

        View btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            finish();
        });



        String currentUserId = sessionManager.getCurrentUser().getId();
        if (currentUserId != null) {
            selectedArtistIds.add(currentUserId);
        }

        // Ánh xạ ID chuẩn với bản XML mới
        edtSongTitle = findViewById(R.id.edt_song_title);
        btnPickCover = findViewById(R.id.btn_pick_cover);
        btnPickAudio = findViewById(R.id.btn_pick_audio);
        btnSubmitUpload = findViewById(R.id.btn_submit_upload);
        btnAddCollab = findViewById(R.id.btn_add_collab);
        chipGroupCollab = findViewById(R.id.chip_group_collab);

        // Ánh xạ các UI nâng cao
        layoutCoverPlaceholder = findViewById(R.id.layout_cover_placeholder);
        imgCoverPreview = findViewById(R.id.img_cover_preview);
        tvAudioTitle = findViewById(R.id.tv_audio_title);
        imgAudioIcon = findViewById(R.id.img_audio_icon);
        tvAudioStatus = findViewById(R.id.tv_audio_status);

        // Bắt sự kiện click
        btnPickCover.setOnClickListener(v -> pickCoverLauncher.launch("image/*"));
        btnPickAudio.setOnClickListener(v -> pickAudioLauncher.launch("audio/*")); // Hỗ trợ mp3, m4a, wav
        btnAddCollab.setOnClickListener(v -> openCollabSearchDialog());
        btnSubmitUpload.setOnClickListener(v -> startUploadProcess());
    }


    // 2. HÀM XỬ LÝ LUỒNG UPLOAD 3 BƯỚC
    private void startUploadProcess() {
        String songTitle = edtSongTitle.getText().toString().trim();

        if (songTitle.isEmpty() || coverUri == null || audioUri == null) {
            Toast.makeText(this, "Vui lòng nhập đủ tên bài hát, chọn ảnh và nhạc!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Khóa nút để tránh user bấm nhiều lần
        btnSubmitUpload.setEnabled(false);
        btnSubmitUpload.setText("ĐANG XỬ LÝ (1/3)...");

        // Chuẩn bị thông tin mạng
        String apiKey = BuildConfig.API_KEY;
        String token = "Bearer " + BuildConfig.API_KEY; // Ở app thật, thay bằng Token của User lấy từ Session

        long timestamp = System.currentTimeMillis();
        String coverFileName = "cover_" + timestamp + ".jpg";
        String audioFileName = "song_" + timestamp + ".mp3";

        // BƯỚC 1: UPLOAD ẢNH BÌA
        byte[] coverBytes = readBytesFromUri(coverUri);
        if (coverBytes == null) { showError(); return; }

        RequestBody coverBody = RequestBody.create(MediaType.parse("image/jpeg"), coverBytes);

        apiService.uploadCover(apiKey, token, "image/jpeg", coverFileName, coverBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Lấy link ảnh public
                    String coverPublicUrl = BuildConfig.BASE_URL + "storage/v1/object/public/cover_song/" + coverFileName;

                    // BƯỚC 2: UPLOAD NHẠC
                    btnSubmitUpload.setText("ĐANG TẢI NHẠC (2/3)...");
                    uploadAudioStep(apiKey, token, audioFileName, coverPublicUrl, songTitle);
                } else {
                    showError();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) { showError(); }
        });
    }

    private void uploadAudioStep(String apiKey, String token, String audioFileName, String coverPublicUrl, String songTitle) {
        byte[] audioBytes = readBytesFromUri(audioUri);
        if (audioBytes == null) { showError(); return; }

        RequestBody audioBody = RequestBody.create(MediaType.parse("audio/mpeg"), audioBytes);

        apiService.uploadAudio(apiKey, token, "audio/mpeg", audioFileName, audioBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Lấy link nhạc public
                    String audioPublicUrl = BuildConfig.BASE_URL + "storage/v1/object/public/song/" + audioFileName;

                    // BƯỚC 3: GỌI HÀM RPC ĐỂ LƯU DATABASE
                    btnSubmitUpload.setText("ĐANG LƯU DỮ LIỆU (3/3)...");
                    submitToDatabase(apiKey, token, songTitle, coverPublicUrl, audioPublicUrl);
                } else {
                    showError();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) { showError(); }
        });
    }

    private void submitToDatabase(String apiKey, String token, String songTitle, String coverUrl, String audioUrl) {

        // 1. Tự động lấy số giây của file MP3
        int duration = getAudioDuration(audioUri);

        // 2. Dữ liệu tạm thời (Vì chúng ta chưa làm UI cho việc chọn Album, Lyric, Genre)
        String albumId = null;
        String lyricsUrl = null;
        java.util.List<Integer> selectedGenreIds = new java.util.ArrayList<>();
        // selectedGenreIds.add(1); // (Tùy chọn) Gỡ comment dòng này nếu bạn muốn test thử việc nhét vào thể loại ID = 1

        // 3. Khởi tạo cục Request với ĐẦY ĐỦ 8 tham số
        SongRequestUpload requestBody = new SongRequestUpload(
                songTitle,
                coverUrl,
                audioUrl,
                duration,
                albumId,
                lyricsUrl,
                selectedArtistIds,
                selectedGenreIds
        );

        // Gọi hàm submitSongWithArtists
        apiService.submitSongWithArtists(apiKey, token, requestBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(UploadSongActivity.this, "Tải lên hoàn tất! Bài hát đang chờ duyệt.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    // In lỗi ra Logcat để kiểm tra nguyên nhân file bị tàng hình
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Lỗi không xác định";
                        android.util.Log.e("UPLOAD_ERROR", "Lỗi API: " + errorMsg);
                        showError();
                    } catch (Exception e) {
                        showError();
                    }
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                android.util.Log.e("UPLOAD_ERROR", "Lỗi Mạng: " + t.getMessage());
                showError();
            }
        });
    }
    // 3. HÀM HỖ TRỢ: ĐỌC FILE TỪ ĐIỆN THOẠI RA MẢNG BYTE (BINARY)
    private byte[] readBytesFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showError() {
        Toast.makeText(this, "Có lỗi xảy ra trong quá trình tải lên!", Toast.LENGTH_SHORT).show();
        btnSubmitUpload.setEnabled(true);
        btnSubmitUpload.setText("TẢI LÊN HỆ THỐNG");
    }

    // Hàm gọi Dialog tìm kiếm ca sĩ (Chuẩn bị cho bước sau)
    private void openCollabSearchDialog() {
        // TODO: Mở BottomSheetDialog tìm kiếm ở đây
        Toast.makeText(this, "Tính năng tìm kiếm ca sĩ đang được xây dựng...", Toast.LENGTH_SHORT).show();
    }
    // Hàm này sẽ được gọi khi bạn tìm và chọn được 1 ca sĩ trong Dialog
    private void addArtistChip(String artistId, String artistName) {
        // Kiểm tra xem ca sĩ này đã được thêm chưa (tránh thêm trùng 2 lần)
        if (selectedArtistIds.contains(artistId)) {
            Toast.makeText(this, "Nghệ sĩ này đã được thêm rồi!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Thêm ID vào danh sách để chuẩn bị gửi lên Server
        selectedArtistIds.add(artistId);

        // Tạo giao diện Thẻ tên (Chip)
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
        chip.setText(artistName);
        chip.setCloseIconVisible(true); // Hiện nút X

        // Cài đặt màu sắc cho chuẩn ngầu
        chip.setChipBackgroundColorResource(R.color.mdx_surface); // Dùng màu nền tối
        chip.setTextColor(getResources().getColor(android.R.color.white));

        // Sự kiện khi bấm nút X để xóa ca sĩ
        chip.setOnCloseIconClickListener(v -> {
            chipGroupCollab.removeView(chip); // Xóa thẻ khỏi màn hình
            selectedArtistIds.remove(artistId); // Xóa ID khỏi danh sách gửi đi
        });

        // Đẩy thẻ lên màn hình
        chipGroupCollab.addView(chip);
    }
    // Hàm lấy thời lượng bài hát tự động
    private int getAudioDuration(Uri audioUri) {
        try {
            android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
            mmr.setDataSource(this, audioUri);
            String durationStr = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            return Integer.parseInt(durationStr) / 1000; // Đổi từ mili-giây sang giây
        } catch (Exception e) {
            return 0; // Nếu lỗi trả về 0
        }
    }
    // ==========================================
    // CHIÊU CUỐI: HẠ BÀN PHÍM HOÀN HẢO MỌI VỊ TRÍ
    // ==========================================
    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}