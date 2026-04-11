package com.melodix.app.View.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.melodix.app.BuildConfig;
import com.melodix.app.Constants;
import com.melodix.app.Model.Playlist;
import com.melodix.app.R;
import com.melodix.app.Repository.PlaylistRepository;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditPlaylistDialog {

    private final Context context;
    private final Playlist currentPlaylist;
    private final OnPlaylistUpdatedListener listener;

    private Uri selectedCoverUri = null;
    private ImageView imgPreview;
    private Button btnSave;

    private Dialog dialog;
    private ActivityResultLauncher<String> imagePickerLauncher;

    public interface OnPlaylistUpdatedListener {
        void onPlaylistUpdated(Playlist playlist);
    }

    public EditPlaylistDialog(Context context, Playlist playlist,
                              OnPlaylistUpdatedListener listener,
                              ActivityResultLauncher<String> launcher) {
        this.context = context;
        this.currentPlaylist = playlist;
        this.listener = listener;
        this.imagePickerLauncher = launcher;
    }

    public void show() {
        View dialogView = View.inflate(context, R.layout.dialog_edit_playlist, null);

        EditText edtName = dialogView.findViewById(R.id.edt_playlist_name);
        imgPreview = dialogView.findViewById(R.id.img_cover_preview);
        Button btnPickCover = dialogView.findViewById(R.id.btn_pick_cover);
        btnSave = dialogView.findViewById(R.id.btn_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        // Điền dữ liệu hiện tại
        edtName.setText(currentPlaylist.name != null ? currentPlaylist.name : "");

        if (currentPlaylist.coverRes != null && !currentPlaylist.coverRes.isEmpty()) {
            Glide.with(context)
                    .load(currentPlaylist.coverRes)
                    .placeholder(R.drawable.ic_music_placeholder)
                    .into(imgPreview);
        }

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

        btnSave.setOnClickListener(v -> {
            String newName = edtName.getText().toString().trim();
            if (newName.isEmpty()) {
                edtName.setError("Vui lòng nhập tên playlist");
                return;
            }

            btnSave.setEnabled(false);
            btnSave.setText("Đang lưu...");

            if (selectedCoverUri != null) {
                uploadCoverThenUpdate(newName);
            } else {
                updatePlaylistOnServer(newName, null);
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
    private void uploadCoverThenUpdate(String playlistName) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(selectedCoverUri);
            if (inputStream == null) {
                updatePlaylistOnServer(playlistName, null);
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

            com.melodix.app.Service.ProfileAPIService storageService =
                    com.melodix.app.Service.RetrofitClient.getClient()
                            .create(com.melodix.app.Service.ProfileAPIService.class);

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);

            String bucketName = "playlist_cover";

            storageService.uploadFileToStorage(
                    BuildConfig.API_KEY,
                    "Bearer " + BuildConfig.API_KEY,
                    "image/jpeg",
                    "true",
                    bucketName,
                    fileName,
                    requestBody
            ).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String coverUrl = Constants.STORAGE_BASE_URL + Constants.PLAYLIST_COVER_BUCKET + fileName;
                        updatePlaylistOnServer(playlistName, coverUrl);
                    } else {
                        updatePlaylistOnServer(playlistName, null);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    updatePlaylistOnServer(playlistName, null);
                }
            });

        } catch (Exception e) {
            updatePlaylistOnServer(playlistName, null);
        }
    }

    private void updatePlaylistOnServer(String name, String coverUrl) {
        PlaylistRepository repo = new PlaylistRepository(context);

        repo.updatePlaylist(currentPlaylist.id, name, coverUrl, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Đã cập nhật playlist!", Toast.LENGTH_SHORT).show();

                    // Cập nhật lại object
                    currentPlaylist.name = name;
                    if (coverUrl != null) currentPlaylist.coverRes = coverUrl;

                    if (listener != null) listener.onPlaylistUpdated(currentPlaylist);
                    dismissDialog();
                } else {
                    Toast.makeText(context, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                    resetButton();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                resetButton();
            }
        });
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    private void resetButton() {
        if (btnSave != null) {
            btnSave.setEnabled(true);
            btnSave.setText("Lưu thay đổi");
        }
    }
}