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

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.melodix.app.BuildConfig;
import com.melodix.app.Constants;
import com.melodix.app.Model.SessionManager;
import com.melodix.app.R;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
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

    private Uri coverUri = null;
    private String artistId;

    // Trình chọn ảnh
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

        // Đã xóa edtYear vì không cần bắt người dùng nhập nữa

        btnPickCover = findViewById(R.id.btn_pick_cover);
        layoutPlaceholder = findViewById(R.id.layout_placeholder);
        imgCoverPreview = findViewById(R.id.img_cover_preview);
        btnCreate = findViewById(R.id.btn_create_album);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnPickCover.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnCreate.setOnClickListener(v -> handleCreateAlbum());
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
            uploadCoverAndSave(title); // Không cần truyền year nữa
        } else {
            // Không up ảnh bìa thì lưu database thẳng luôn
            saveToDatabase(title, null); // Không cần truyền year nữa
        }
    }

    private void uploadCoverAndSave(String title) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(coverUri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] imageBytes = byteBuffer.toByteArray();

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);
            String fileName = "album_" + System.currentTimeMillis() + ".jpg";

            ProfileAPIService storageService = RetrofitClient.getClient().create(ProfileAPIService.class);
            String apiKey = BuildConfig.API_KEY;
            String token = "Bearer " + BuildConfig.API_KEY;

            storageService.uploadFileToStorage(
                    apiKey, token, "image/jpeg", "true",
                    Constants.ALBUM_COVER_BUCKET.replace("/", ""),
                    fileName, requestBody
            ).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String coverUrl = Constants.STORAGE_BASE_URL + Constants.ALBUM_COVER_BUCKET + fileName;
                        saveToDatabase(title, coverUrl);
                    } else {
                        showError("Lỗi upload ảnh: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    showError("Lỗi mạng khi upload ảnh");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi đọc file ảnh");
        }
    }

    private void saveToDatabase(String title, String coverUrl) {
        Map<String, Object> albumData = new HashMap<>();
        albumData.put("title", title);
        albumData.put("artist_id", artistId);

        // ĐÂY LÀ PHẦN PHÉP THUẬT: Tự động lấy năm hiện tại của máy
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        albumData.put("release_year", currentYear);

        if (coverUrl != null) {
            albumData.put("cover_url", coverUrl);
        }

        ArtistAPIService apiService = RetrofitClient.getClient().create(ArtistAPIService.class);
        apiService.createAlbum(BuildConfig.API_KEY, "Bearer " + BuildConfig.API_KEY, albumData)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(CreateAlbumActivity.this, "Đã tạo Album thành công!", Toast.LENGTH_LONG).show();
                            resetForm();
                        } else {
                            showError("Lỗi lưu Database");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        showError("Lỗi kết nối máy chủ");
                    }
                });
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        btnCreate.setEnabled(true);
        btnCreate.setText("TẠO ALBUM");
    }

    private void resetForm(){
        btnCreate.setEnabled(true);
        btnCreate.setText("TẠO ALBUM");

        edtTitle.setText("");

        coverUri = null;
        imgCoverPreview.setVisibility(View.GONE);
        layoutPlaceholder.setVisibility(View.VISIBLE);
    }
}