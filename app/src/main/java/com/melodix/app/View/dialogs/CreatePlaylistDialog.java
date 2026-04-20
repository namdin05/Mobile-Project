package com.melodix.app.View.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.melodix.app.BuildConfig;
import com.melodix.app.Constants;
import com.melodix.app.Model.Playlist;
import com.melodix.app.R;
import com.melodix.app.Repository.PlaylistRepository;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Service.StorageAPIService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePlaylistDialog {

    private final Context context;
    private final OnPlaylistCreatedListener listener;

    private Uri selectedCoverUri = null;
    private ImageView imgPreview;
    private Button btnCreate;

    private Dialog dialog;
    private ActivityResultLauncher<String> imagePickerLauncher;

    public interface OnPlaylistCreatedListener {
        void onPlaylistCreated(Playlist playlist);
    }

    public CreatePlaylistDialog(Context context, OnPlaylistCreatedListener listener, ActivityResultLauncher<String> launcher) {
        this.context = context;
        this.listener = listener;
        this.imagePickerLauncher = launcher;
    }

    public void show() {
        View dialogView = View.inflate(context, R.layout.dialog_create_playlist, null);

        EditText edtName = dialogView.findViewById(R.id.edt_playlist_name);
        imgPreview = dialogView.findViewById(R.id.img_cover_preview);
        Button btnPickCover = dialogView.findViewById(R.id.btn_pick_cover);
        btnCreate = dialogView.findViewById(R.id.btn_create);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnPickCover.setOnClickListener(v -> {
            if (imagePickerLauncher != null) {
                imagePickerLauncher.launch("image/*");
            } else {
                Toast.makeText(context, "Không thể mở thư viện ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        btnCreate.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            if (name.isEmpty()) {
                edtName.setError("Vui lòng nhập tên playlist");
                return;
            }

            btnCreate.setEnabled(false);
            btnCreate.setText("Đang tạo...");

            if (selectedCoverUri != null) {
                uploadCoverThenCreatePlaylist(name);
            } else {
                createPlaylistOnServer(name, null);
            }
        });

        btnCancel.setOnClickListener(v -> dismissDialog());

        dialog.show();
    }

    public void setSelectedCoverUri(Uri uri) {
        this.selectedCoverUri = uri;
        if (imgPreview != null && uri != null) {
            Glide.with(context)
                    .load(uri)
                    .placeholder(R.drawable.ic_music_placeholder)
                    .into(imgPreview);
        }
    }

    // Upload Ảnh
    private void uploadCoverThenCreatePlaylist(String playlistName) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(selectedCoverUri);
            if (inputStream == null) {
                createPlaylistOnServer(playlistName, null);
                return;
            }

            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] imageBytes = byteBuffer.toByteArray();
            inputStream.close();

            String fileName = "playlist_" + UUID.randomUUID() + ".jpg";

            StorageAPIService storageService = RetrofitClient.getStorage(context.getApplicationContext()).create(StorageAPIService.class);

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);

            String bucketName = "playlist_cover";

            android.util.Log.d("UPLOAD_DEBUG", "=== BẮT ĐẦU UPLOAD ===");
            android.util.Log.d("UPLOAD_DEBUG", "Bucket: " + bucketName);
            android.util.Log.d("UPLOAD_DEBUG", "File: " + fileName);
            android.util.Log.d("UPLOAD_DEBUG", "Using API KEY (no user token)");

            storageService.uploadFileToStorage(
                    "image/jpeg",
                    "true",
                    bucketName,
                    fileName,
                    requestBody
            ).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    android.util.Log.d("UPLOAD_DEBUG", "Response code: " + response.code());

                    if (response.isSuccessful()) {
                        String coverUrl = Constants.STORAGE_BASE_URL + Constants.PLAYLIST_COVER_BUCKET + fileName;
                        android.util.Log.d("UPLOAD_DEBUG", "THÀNH CÔNG - URL: " + coverUrl);
                        createPlaylistOnServer(playlistName, coverUrl);
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                            android.util.Log.e("UPLOAD_DEBUG", "LỖI: " + errorBody);
                            Toast.makeText(context, "Upload ảnh thất bại (Code " + response.code() + ")", Toast.LENGTH_LONG).show();
                        } catch (Exception ignored) {}
                        createPlaylistOnServer(playlistName, null);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    android.util.Log.e("UPLOAD_DEBUG", "onFailure: " + t.getMessage());
                    Toast.makeText(context, "Lỗi upload: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    createPlaylistOnServer(playlistName, null);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("UPLOAD_DEBUG", "Exception: " + e.getMessage());
            createPlaylistOnServer(playlistName, null);
        }
    }

    private void createPlaylistOnServer(String name, String coverUrl) {
        PlaylistRepository repo = new PlaylistRepository(context);

        repo.createPlaylist(name, coverUrl, new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                Log.d("CREATE_PLAYLIST", "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Playlist created = response.body().get(0);   // Lấy phần tử đầu tiên trong array

                    Log.d("CREATE_PLAYLIST", "Tạo thành công - ID: " + created.id);

                    if (listener != null) {
                        listener.onPlaylistCreated(created);
                    }

                    dismissDialog();
                    Toast.makeText(context, "Tạo playlist thành công!", Toast.LENGTH_SHORT).show();
                }
                else {
                    String errorMsg = "Unknown error";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}

                    Log.e("CREATE_PLAYLIST", "Thất bại - Code: " + response.code() + " - " + errorMsg);
                    Toast.makeText(context, "Tạo playlist thất bại", Toast.LENGTH_SHORT).show();
                    resetButton();
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                Log.e("CREATE_PLAYLIST", "onFailure: " + t.getMessage(), t);
                Toast.makeText(context, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                resetButton();
            }
        });
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    private void resetButton() {
        if (btnCreate != null) {
            btnCreate.setEnabled(true);
            btnCreate.setText("Tạo Playlist");
        }
    }
}