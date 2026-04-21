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

import com.melodix.app.Constants;
import com.melodix.app.Model.SongRequestUpload;
import com.melodix.app.R;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.GenreAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Service.SearchAPIService;
import com.melodix.app.Service.StorageAPIService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        // ĐÃ SỬA: Phân biệt rõ Database Client và Storage Client
        apiService = RetrofitClient.getClient(getApplicationContext()).create(ArtistAPIService.class);
        storageService = RetrofitClient.getStorage(getApplicationContext()).create(StorageAPIService.class);

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

            tvAudioTitle.setText("Đã có file âm thanh gốc");
            tvAudioStatus.setText("Chỉ chọn lại nếu muốn thay file MP3 khác");
        }

        btnSelectAlbum.setOnClickListener(v -> openAlbumDialog());
        btnAddGenre.setOnClickListener(v -> openGenreDialog());
        btnPickCover.setOnClickListener(v -> pickCoverLauncher.launch("image/*"));
        btnPickAudio.setOnClickListener(v -> pickAudioLauncher.launch("audio/*"));
        btnAddCollab.setOnClickListener(v -> openCollabSearchDialog());
        btnSubmitUpload.setOnClickListener(v -> startUploadProcess());
    }

    // =================================================================================
    // HÀM DÙNG CHUNG ĐỂ UPLOAD BẤT KỲ FILE NÀO (DRY Principle)
    // =================================================================================
    private void uploadFileToSupabase(Uri fileUri, String bucketName, String fileName, String mimeType, Callback<ResponseBody> callback) {
        byte[] fileBytes = readBytesFromUri(fileUri);
        if (fileBytes == null) {
            showError("Không đọc được file!");
            return;
        }

        RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), fileBytes);

        // Thay thế dấu "/" để tránh lỗi path của Retrofit
        storageService.uploadFileToStorage(mimeType, "true", bucketName.replace("/", ""), fileName, requestBody)
                .enqueue(callback);
    }

    private void startUploadProcess() {
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

        btnSubmitUpload.setEnabled(false);
        btnSubmitUpload.setText(isEditMode ? "ĐANG CẬP NHẬT..." : "ĐANG XỬ LÝ (1/3)...");

        // BƯỚC 1: Xử lý Ảnh bìa
        if (coverUri != null) {
            String coverFileName = "cover_" + System.currentTimeMillis() + ".jpg";

            uploadFileToSupabase(coverUri, Constants.SONG_COVER_BUCKET, coverFileName, "image/jpeg", new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String newCoverUrl = Constants.STORAGE_BASE_URL + Constants.SONG_COVER_BUCKET + coverFileName;
                        uploadAudioStep(newCoverUrl, songTitle); // Chuyển sang bước Up nhạc
                    } else {
                        logErrorBody("UPLOAD_COVER_ERROR", response);
                        showError("Upload ảnh bìa thất bại");
                    }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    showError("Lỗi mạng khi upload ảnh bìa");
                }
            });
        } else {
            uploadAudioStep(existingCoverUrl, songTitle);
        }
    }

    private void uploadAudioStep(String finalCoverUrl, String songTitle) {
        // BƯỚC 2: Xử lý File Nhạc
        if (audioUri != null) {
            btnSubmitUpload.setText("ĐANG TẢI NHẠC (2/3)...");
            String slugTitle = generateSlug(songTitle);
            String audioFileName = slugTitle + ".mp3";
            byte[] audioBytes = readBytesFromUri(audioUri);

            uploadFileToSupabase(audioUri, Constants.SONG_AUDIO_BUCKET, audioFileName, "audio/mpeg", new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String newAudioUrl = Constants.STORAGE_BASE_URL + Constants.SONG_AUDIO_BUCKET + audioFileName;
                        submitToDatabase(songTitle, finalCoverUrl, newAudioUrl); // Chuyển sang bước Lưu Database
                    } else {
                        logErrorBody("UPLOAD_AUDIO_ERROR", response);
                        showError("Upload file nhạc thất bại");
                    }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    showError("Lỗi mạng khi upload file nhạc");
                }
            });
        } else {
            submitToDatabase(songTitle, finalCoverUrl, null);
        }
    }

    private void submitToDatabase(String songTitle, String coverUrl, String audioUrl) {
        btnSubmitUpload.setText(isEditMode ? "ĐANG LƯU DỮ LIỆU..." : "ĐANG LƯU DỮ LIỆU (3/3)...");

        if (isEditMode) {
            java.util.Map<String, Object> songData = new java.util.HashMap<>();
            songData.put("title", songTitle);
            if (coverUrl != null) songData.put("cover_url", coverUrl);

            if (audioUrl != null) {
                songData.put("audio_url", audioUrl);
                songData.put("duration_seconds", getAudioDuration(audioUri));
            }
            if (selectedAlbumId != null) {
                songData.put("album_id", selectedAlbumId);
            }

            apiService.updateSong("eq." + editSongId, songData).enqueue(new Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(UploadSongActivity.this, "Đã cập nhật bài hát!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        logErrorBody("UPDATE_SONG_ERROR", response);
                        showError("Cập nhật thất bại: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                    showError("Lỗi mạng khi cập nhật");
                }
            });

        } else {
            int duration = getAudioDuration(audioUri);
            SongRequestUpload requestBody = new SongRequestUpload(
                    songTitle, coverUrl, audioUrl, duration, selectedAlbumId, null, selectedArtistIds, selectedGenreIds
            );

            // ĐÃ SỬA: Xóa bỏ apiKey và token vì Interceptor đã tự động gắn rồi
            apiService.submitSongWithArtists(requestBody)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(UploadSongActivity.this, "Tải lên hoàn tất! Bài hát đang chờ duyệt.", Toast.LENGTH_LONG).show();
                                resetForm();
                            } else {
                                logErrorBody("UPLOAD_RPC_ERROR", response);
                                showError("Lưu dữ liệu bài hát thất bại");
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            showError("Lỗi mạng khi lưu dữ liệu");
                        }
                    });
        }
    }

    private byte[] readBytesFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()) {

            if (inputStream == null) return null;

            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;

            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();

        } catch (Exception e) {
            android.util.Log.e("READ_FILE_ERROR", "Không đọc được file: " + e.getMessage(), e);
            return null;
        }
    }

    private void logErrorBody(String tag, Response<?> response) {
        try {
            String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Không có error body";
            android.util.Log.e(tag, "HTTP " + response.code() + " | " + errorMsg);
        } catch (Exception e) {
            android.util.Log.e(tag, "Không đọc được error body", e);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        btnSubmitUpload.setEnabled(true);
        btnSubmitUpload.setText(isEditMode ? "CẬP NHẬT TÁC PHẨM" : "PHÁT HÀNH TÁC PHẨM");
    }

    private int getAudioDuration(Uri audioUri) {
        try {
            android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
            mmr.setDataSource(this, audioUri);
            String durationStr = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            return Integer.parseInt(durationStr) / 1000;
        } catch (Exception e) {
            android.util.Log.e("AUDIO_DURATION_ERROR", "Không lấy được duration", e);
            return 0;
        }
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

    // =========================================================================
    // Các hàm UI (Mở Dialog Chọn Album, Thể Loại, Nghệ sĩ...) GIỮ NGUYÊN BÊN DƯỚI
    // =========================================================================

    private void openAlbumDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetTheme);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(0, 40, 0, 40);
        scrollView.addView(container);
        dialog.setContentView(scrollView);

        android.graphics.drawable.GradientDrawable bgShape = new android.graphics.drawable.GradientDrawable();
        bgShape.setColor(android.graphics.Color.WHITE);
        bgShape.setCornerRadii(new float[]{60, 60, 60, 60, 0, 0, 0, 0});
        ((View) scrollView.getParent()).setBackgroundColor(android.graphics.Color.TRANSPARENT);
        container.setBackground(bgShape);

        TextView title = new TextView(this);
        title.setText("Chọn Album");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(android.graphics.Color.BLACK);
        title.setPadding(60, 20, 60, 30);
        container.addView(title);

        ArtistAPIService supabaseApi = RetrofitClient.getClient(getApplicationContext()).create(ArtistAPIService.class);

        // ĐÃ SỬA: Dùng biến currentUserId để truy vấn thay vì gọi sessionManager
        supabaseApi.getAlbumsByArtistId("eq." + currentUserId).enqueue(new Callback<java.util.List<com.melodix.app.Model.Album>>() {
            @Override
            public void onResponse(Call<java.util.List<com.melodix.app.Model.Album>> call, Response<java.util.List<com.melodix.app.Model.Album>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    container.addView(createPremiumDialogItem("🎵", "Single (Không thuộc album nào)", v -> {
                        selectedAlbumId = null;
                        tvSelectedAlbum.setText("Single (Không thuộc album nào)");
                        dialog.dismiss();
                    }));

                    View divider = new View(UploadSongActivity.this);
                    divider.setBackgroundColor(android.graphics.Color.parseColor("#E5E5EA"));
                    android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 3);
                    params.setMargins(60, 10, 60, 10);
                    divider.setLayoutParams(params);
                    container.addView(divider);

                    for (com.melodix.app.Model.Album album : response.body()) {
                        if(album.status != null && album.status.equalsIgnoreCase("rejected") ){
                            continue;
                        }
                        String displayTitle = album.title;
                        if (album.status != null && album.status.equalsIgnoreCase("pending")){
                            displayTitle = album.title + "( Pending Review )";
                        }else{
                            displayTitle = album.title + "( " + album.year + " )";
                        }

                        container.addView(createPremiumDialogItem("💽", displayTitle, v -> {
                            selectedAlbumId = album.id;
                            tvSelectedAlbum.setText(album.title);
                            dialog.dismiss();
                        }));
                    }
                }
            }
            @Override public void onFailure(Call<java.util.List<com.melodix.app.Model.Album>> call, Throwable t) {}
        });
        dialog.show();
    }

    private void openGenreDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetTheme);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(0, 40, 0, 40);
        scrollView.addView(container);
        dialog.setContentView(scrollView);

        android.graphics.drawable.GradientDrawable bgShape = new android.graphics.drawable.GradientDrawable();
        bgShape.setColor(android.graphics.Color.WHITE);
        bgShape.setCornerRadii(new float[]{60, 60, 60, 60, 0, 0, 0, 0});
        ((View) scrollView.getParent()).setBackgroundColor(android.graphics.Color.TRANSPARENT);
        container.setBackground(bgShape);

        TextView title = new TextView(this);
        title.setText("Music genre");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(android.graphics.Color.BLACK);
        title.setPadding(60, 20, 60, 30);
        container.addView(title);

        GenreAPIService genreApi = RetrofitClient.getClient(getApplicationContext()).create(GenreAPIService.class);
        genreApi.getGenres().enqueue(new Callback<java.util.List<com.melodix.app.Model.Genre>>() {
            @Override
            public void onResponse(Call<java.util.List<com.melodix.app.Model.Genre>> call, Response<java.util.List<com.melodix.app.Model.Genre>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (com.melodix.app.Model.Genre genre : response.body()) {
                        if (!genre.isVisible()) continue;

                        container.addView(createPremiumDialogItem("🎧", genre.getName(), v -> {
                            int gId = Integer.parseInt(genre.getId());
                            if (!selectedGenreIds.contains(gId)) {
                                selectedGenreIds.add(gId);
                                addGenreChip(gId, genre.getName());
                            }
                            dialog.dismiss();
                        }));
                    }
                }
            }
            @Override public void onFailure(Call<java.util.List<com.melodix.app.Model.Genre>> call, Throwable t) {}
        });
        dialog.show();
    }

    private void openCollabSearchDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetTheme);

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(0, 40, 0, 60);

        android.graphics.drawable.GradientDrawable bgShape = new android.graphics.drawable.GradientDrawable();
        bgShape.setColor(android.graphics.Color.WHITE);
        bgShape.setCornerRadii(new float[]{60, 60, 60, 60, 0, 0, 0, 0});
        container.setBackground(bgShape);

        EditText edtSearch = new EditText(this);
        edtSearch.setHint("🔍 Enter artist name...");
        edtSearch.setTextColor(android.graphics.Color.BLACK);
        edtSearch.setHintTextColor(android.graphics.Color.parseColor("#8E8E93"));
        edtSearch.setPadding(50, 40, 50, 40);
        edtSearch.setSingleLine(true);
        edtSearch.setTextSize(16);
        edtSearch.setBackgroundResource(android.R.color.transparent);

        android.widget.LinearLayout searchContainer = new android.widget.LinearLayout(this);
        android.graphics.drawable.GradientDrawable searchBg = new android.graphics.drawable.GradientDrawable();
        searchBg.setColor(android.graphics.Color.parseColor("#F2F2F7"));
        searchBg.setCornerRadius(30);
        searchContainer.setBackground(searchBg);

        android.widget.LinearLayout.LayoutParams searchParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(60, 20, 60, 40);
        searchContainer.setLayoutParams(searchParams);
        searchContainer.addView(edtSearch, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(searchContainer);

        android.widget.FrameLayout resultFrame = new android.widget.FrameLayout(this);
        android.widget.LinearLayout.LayoutParams frameParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        frameParams.setMargins(0, 0, 0, 40);
        resultFrame.setLayoutParams(frameParams);

        android.widget.LinearLayout resultContainer = new android.widget.LinearLayout(this);
        resultContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        resultFrame.addView(resultContainer);

        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        android.widget.FrameLayout.LayoutParams progressParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER);
        progressParams.topMargin = 50;
        progressBar.setLayoutParams(progressParams);
        progressBar.setVisibility(View.GONE);
        resultFrame.addView(progressBar);

        TextView tvEmpty = new TextView(this);
        tvEmpty.setText("Dữ liệu trống rỗng 🍃\nHãy thử tìm tên nghệ sĩ khác xem sao.");
        tvEmpty.setGravity(android.view.Gravity.CENTER);
        tvEmpty.setTextColor(android.graphics.Color.parseColor("#8E8E93"));
        tvEmpty.setTextSize(14);
        android.widget.FrameLayout.LayoutParams emptyParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER);
        emptyParams.topMargin = 100;
        tvEmpty.setLayoutParams(emptyParams);
        tvEmpty.setVisibility(View.GONE);
        resultFrame.addView(tvEmpty);

        container.addView(resultFrame);

        dialog.setContentView(container);
        ((View) container.getParent()).setBackgroundColor(android.graphics.Color.TRANSPARENT);

        SearchAPIService searchAPI = RetrofitClient.getClient(getApplicationContext()).create(SearchAPIService.class);

        android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        final Runnable[] searchRunnable = {null};

        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String query = s.toString().trim();

                if (searchRunnable[0] != null) {
                    searchHandler.removeCallbacks(searchRunnable[0]);
                }

                if (query.isEmpty()) {
                    resultContainer.removeAllViews();
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.GONE);
                    return;
                }

                resultContainer.removeAllViews();
                tvEmpty.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);

                searchRunnable[0] = () -> {
                    String formattedQuery = query.replaceAll("\\s+", " & ") + ":*";
                    String ftsQuery = "fts(simple)." + android.net.Uri.encode(formattedQuery);

                    searchAPI.searchArtists(ftsQuery).enqueue(new Callback<java.util.List<com.melodix.app.Model.Artist>>() {
                        @Override
                        public void onResponse(Call<java.util.List<com.melodix.app.Model.Artist>> call, Response<java.util.List<com.melodix.app.Model.Artist>> response) {
                            progressBar.setVisibility(View.GONE);

                            if (response.isSuccessful() && response.body() != null) {
                                if (response.body().isEmpty()) {
                                    tvEmpty.setVisibility(View.VISIBLE);
                                } else {
                                    for (com.melodix.app.Model.Artist artist : response.body()) {
                                        resultContainer.addView(createPremiumArtistDialogItem(artist.avatarRes, artist.name, v -> {
                                            addArtistChip(artist.id, artist.name);
                                            dialog.dismiss();
                                        }));
                                    }
                                }
                            } else {
                                tvEmpty.setText("Có lỗi xảy ra khi tìm kiếm 😢");
                                tvEmpty.setVisibility(View.VISIBLE);
                            }
                        }
                        @Override public void onFailure(Call<java.util.List<com.melodix.app.Model.Artist>> call, Throwable t) {
                            progressBar.setVisibility(View.GONE);
                            tvEmpty.setText("Lỗi kết nối mạng 📶");
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                    });
                };
                searchHandler.postDelayed(searchRunnable[0], 500);
            }
        });
        dialog.show();

        // FIX LỖI R Ở ĐÂY: Sử dụng getResources().getIdentifier() để lấy ID an toàn
        int bottomSheetId = getResources().getIdentifier("design_bottom_sheet", "id", getPackageName());
        if (bottomSheetId == 0) {
            // Nếu không tìm thấy bằng getPackageName (thường do là ID của thư viện), thử dùng package của material
            bottomSheetId = getResources().getIdentifier("design_bottom_sheet", "id", "com.google.android.material");
        }

        View bottomSheet = dialog.findViewById(bottomSheetId);

        if (bottomSheet != null) {
            com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);

            int screenHeight = android.content.res.Resources.getSystem().getDisplayMetrics().heightPixels;
            bottomSheet.getLayoutParams().height = (int) (screenHeight * 0.85);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
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

    private View createPremiumDialogItem(String icon, String text, View.OnClickListener onClick) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setPadding(60, 45, 60, 45);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        android.util.TypedValue outValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        layout.setBackgroundResource(outValue.resourceId);
        layout.setClickable(true);
        layout.setOnClickListener(onClick);

        TextView tvIcon = new TextView(this);
        tvIcon.setText(icon);
        tvIcon.setTextSize(22);
        tvIcon.setPadding(0, 0, 40, 0);

        TextView tvText = new TextView(this);
        tvText.setText(text);
        tvText.setTextColor(android.graphics.Color.parseColor("#1C1C1E"));
        tvText.setTextSize(16);
        tvText.setTypeface(null, android.graphics.Typeface.BOLD);

        layout.addView(tvIcon);
        layout.addView(tvText);

        return layout;
    }

    private View createPremiumArtistDialogItem(String avatarUrl, String text, View.OnClickListener onClick) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setPadding(60, 40, 60, 40);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        android.util.TypedValue outValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        layout.setBackgroundResource(outValue.resourceId);
        layout.setClickable(true);
        layout.setOnClickListener(onClick);

        android.widget.ImageView imgAvatar = new android.widget.ImageView(this);
        int avatarSize = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
        android.widget.LinearLayout.LayoutParams imgParams = new android.widget.LinearLayout.LayoutParams(avatarSize, avatarSize);
        imgParams.setMargins(0, 0, 40, 0);
        imgAvatar.setLayoutParams(imgParams);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_menu_for)
                    .circleCrop()
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        TextView tvText = new TextView(this);
        tvText.setText(text);
        tvText.setTextColor(android.graphics.Color.parseColor("#1C1C1E"));
        tvText.setTextSize(16);
        tvText.setTypeface(null, android.graphics.Typeface.BOLD);

        layout.addView(imgAvatar);
        layout.addView(tvText);

        return layout;
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
    // ==========================================
    // HÀM CHUYỂN TÊN CÓ DẤU THÀNH KHÔNG DẤU VIẾT LIỀN
    // Vd: "Chắc Ai Đó Sẽ Về!" -> "chacaidoseve"
    // ==========================================
    private String generateSlug(String input) {
        if (input == null || input.isEmpty()) return "unknown_song";

        // 1. Tách dấu ra khỏi chữ cái
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccents = pattern.matcher(normalized).replaceAll("");

        // 2. Xử lý riêng chữ Đ (vì Normalizer không xử lý được chữ này)
        noAccents = noAccents.replace("đ", "d").replace("Đ", "D");

        // 3. Xóa BỎ TOÀN BỘ khoảng trắng và ký tự đặc biệt, sau đó chuyển thành chữ thường
        return noAccents.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }
}
