package com.melodix.app.View.artist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.melodix.app.BuildConfig;
import com.melodix.app.Constants;
import com.melodix.app.Model.SessionManager;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateAlbumActivity extends AppCompatActivity {

    private EditText edtTitle;
    private View btnPickCover, layoutPlaceholder;
    private ImageView imgCoverPreview;
    private MaterialButton btnCreate;

    // Giao diện chọn bài hát
    private RecyclerView rvAvailableSongs;
    private com.melodix.app.View.adapters.SongSelectionAdapter songSelectionAdapter;
    private List<Song> allMySongs = new ArrayList<>();
    private final List<String> selectedSongIds = new ArrayList<>();

    private Uri coverUri = null;
    private String artistId;

    // ==========================================
    // BIẾN QUẢN LÝ CHẾ ĐỘ EDIT
    // ==========================================
    private boolean isEditMode = false;
    private String editAlbumId = null;
    private String existingCoverUrl = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    coverUri = result.getData().getData();
                    layoutPlaceholder.setVisibility(View.GONE);
                    imgCoverPreview.setVisibility(View.VISIBLE);
                    Glide.with(this).load(coverUri).into(imgCoverPreview);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_album);

        artistId = SessionManager.getInstance(this).getCurrentUser().getId();

        edtTitle = findViewById(R.id.edt_album_title);
        btnPickCover = findViewById(R.id.btn_pick_cover);
        layoutPlaceholder = findViewById(R.id.layout_placeholder);
        imgCoverPreview = findViewById(R.id.img_cover_preview);
        btnCreate = findViewById(R.id.btn_create_album);
        rvAvailableSongs = findViewById(R.id.rv_available_songs);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnPickCover.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnCreate.setOnClickListener(v -> handleCreateAlbum());

        // ==========================================
        // KIỂM TRA XEM CÓ PHẢI ĐANG CHỈNH SỬA KHÔNG
        // ==========================================
        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);

        if (isEditMode) {
            editAlbumId = getIntent().getStringExtra("EDIT_ALBUM_ID");
            String oldTitle = getIntent().getStringExtra("EDIT_ALBUM_TITLE");
            existingCoverUrl = getIntent().getStringExtra("EDIT_ALBUM_COVER");

            // Đổi Giao diện sang chế độ Edit
            edtTitle.setText(oldTitle);
            btnCreate.setText("CẬP NHẬT ALBUM");

            if (existingCoverUrl != null && !existingCoverUrl.isEmpty()) {
                layoutPlaceholder.setVisibility(View.GONE);
                imgCoverPreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(existingCoverUrl).into(imgCoverPreview);
            }

            // Ẩn danh sách chọn bài hát vì Edit Album chỉ sửa Tên/Ảnh
            rvAvailableSongs.setVisibility(View.GONE);
            // Nếu bạn có TextView tiêu đề "Danh sách bài hát", bạn có thể findViewById và ẩn nó đi luôn
        } else {
            // Nếu là TẠO MỚI thì mới load danh sách bài hát
            setupSongSelectionList();
            fetchMySongs();
        }
    }

    private void setupSongSelectionList() {
        rvAvailableSongs.setLayoutManager(new LinearLayoutManager(this));
        songSelectionAdapter = new com.melodix.app.View.adapters.SongSelectionAdapter(this, allMySongs, selectedSongIds);
        rvAvailableSongs.setAdapter(songSelectionAdapter);
    }

    private void fetchMySongs() {
        AppRepository.getInstance(this).getMyUploadSongs(artistId, new AppRepository.SongListCallback() {
            @Override
            public void onSuccess(ArrayList<Song> songs) {
                if (isFinishing()) return;
                List<Song> availableSongs = new ArrayList<>();
                for (Song s : songs) {
                    boolean isSingle = (s.getAlbumId() == null || s.getAlbumId().isEmpty() || s.getAlbumId().equals("null"));
                    boolean isNotRejected = (s.getStatus() == null || !s.getStatus().equalsIgnoreCase("rejected"));
                    if (isSingle && isNotRejected){
                        availableSongs.add(s);
                    }
                }
                songSelectionAdapter.updateData(new ArrayList<>(availableSongs));
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CreateAlbumActivity.this, "Chi tiết lỗi: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleCreateAlbum() {
        String title = edtTitle.getText().toString().trim();
        if (title.isEmpty()) {
            edtTitle.setError("Tên album là bắt buộc");
            return;
        }

        btnCreate.setEnabled(false);
        btnCreate.setText("ĐANG XỬ LÝ...");

        if (coverUri != null) {
            uploadCoverAndSave(title); // Có chọn ảnh mới thì up ảnh mới
        } else {
            // Không chọn ảnh mới. Nếu là Edit thì lấy lại ảnh cũ
            saveToDatabase(title, isEditMode ? existingCoverUrl : null);
        }
    }

    private void uploadCoverAndSave(String title) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(coverUri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) { byteBuffer.write(buffer, 0, len); }
            byte[] imageBytes = byteBuffer.toByteArray();

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);
            String fileName = "album_" + System.currentTimeMillis() + ".jpg";

            ProfileAPIService storageService = RetrofitClient.getClient().create(ProfileAPIService.class);
            storageService.uploadFileToStorage(
                    BuildConfig.API_KEY, "Bearer " + BuildConfig.API_KEY, "image/jpeg", "true",
                    Constants.ALBUM_COVER_BUCKET.replace("/", ""), fileName, requestBody
            ).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String coverUrl = Constants.STORAGE_BASE_URL + Constants.ALBUM_COVER_BUCKET + fileName;
                        saveToDatabase(title, coverUrl);
                    } else { showError("Lỗi upload ảnh"); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) { showError("Lỗi mạng upload ảnh"); }
            });
        } catch (Exception e) { showError("Lỗi đọc file ảnh"); }
    }

    // ==========================================
    // LOGIC LƯU DỮ LIỆU ĐÃ ĐƯỢC NÂNG CẤP
    // ==========================================
    private void saveToDatabase(String title, String coverUrl) {
        Map<String, Object> albumData = new HashMap<>();

        if (isEditMode) {
            // TH1: LÀ CHẾ ĐỘ CẬP NHẬT (Gọi API PATCH)
            albumData.put("title", title);
            if (coverUrl != null) albumData.put("cover_url", coverUrl);

            ArtistAPIService dbService = RetrofitClient.getSupabaseClient().create(ArtistAPIService.class);
            dbService.updateAlbum("eq." + editAlbumId, albumData).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(CreateAlbumActivity.this, "Đã cập nhật Album!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        showError("Lỗi cập nhật Album: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    showError("Lỗi kết nối mạng");
                }
            });

        } else {
            // TH2: LÀ CHẾ ĐỘ TẠO MỚI (Gọi API RPC)
            albumData.put("p_title", title);
            albumData.put("p_artist_id", artistId);
            albumData.put("p_year", java.util.Calendar.getInstance().get(java.util.Calendar.YEAR));
            albumData.put("p_description", "");

            if (coverUrl != null) albumData.put("p_cover_url", coverUrl);
            else albumData.put("p_cover_url", null);

            albumData.put("p_existing_song_ids", selectedSongIds);

            ArtistAPIService apiService = RetrofitClient.getClient().create(ArtistAPIService.class);
            apiService.createAlbumWithSongs(BuildConfig.API_KEY, "Bearer " + BuildConfig.API_KEY, albumData)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(CreateAlbumActivity.this, "Đã gửi Album! Đang chờ duyệt.", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                showError("Lỗi lưu Database RPC");
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            showError("Lỗi kết nối máy chủ");
                        }
                    });
        }
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        btnCreate.setEnabled(true);
        btnCreate.setText(isEditMode ? "CẬP NHẬT ALBUM" : "TẠO ALBUM");
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
}