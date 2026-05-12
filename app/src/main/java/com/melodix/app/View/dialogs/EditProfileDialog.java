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
import com.melodix.app.Constants;
import com.melodix.app.Model.Profile;
import com.melodix.app.R;
import com.melodix.app.Repository.ProfileRepository;
import com.melodix.app.Service.StorageAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Utils.SessionManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileDialog {

    private final Context context;
    private final Profile currentProfile;
    private final OnProfileUpdatedListener listener;
    private Uri selectedAvatarUri = null;

    private ImageView imgPreview;
    private EditText edtDisplayName;
    private Button btnSave;
    private Dialog dialog;
    private ActivityResultLauncher<String> imagePickerLauncher;

    public interface OnProfileUpdatedListener {
        void onProfileUpdated(Profile updatedProfile);
    }

    public EditProfileDialog(Context context, Profile profile,
                             OnProfileUpdatedListener listener,
                             ActivityResultLauncher<String> launcher) {
        this.context = context;
        this.currentProfile = profile;
        this.listener = listener;
        this.imagePickerLauncher = launcher;
    }

    public void show() {
        View dialogView = View.inflate(context, R.layout.dialog_edit_profile, null);

        edtDisplayName = dialogView.findViewById(R.id.edt_display_name);
        imgPreview = dialogView.findViewById(R.id.img_avatar_preview);
        Button btnPickAvatar = dialogView.findViewById(R.id.btn_pick_avatar);
        btnSave = dialogView.findViewById(R.id.btn_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        
        edtDisplayName.setText(currentProfile.getDisplayName() != null ? currentProfile.getDisplayName() : "");

        if (currentProfile.getAvatarUrl() != null && !currentProfile.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(currentProfile.getAvatarUrl())
                    .circleCrop()
                    .placeholder(R.drawable.circle_placeholder)
                    .into(imgPreview);
        }

        dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnPickAvatar.setOnClickListener(v -> {
            if (imagePickerLauncher != null) {
                imagePickerLauncher.launch("image/*");
            }
        });

        btnSave.setOnClickListener(v -> {
            String newName = edtDisplayName.getText().toString().trim();
            if (newName.isEmpty()) {
                edtDisplayName.setError("Tên không được để trống");
                return;
            }

            btnSave.setEnabled(false);
            btnSave.setText("Đang lưu...");

            if (selectedAvatarUri != null) {
                uploadAvatarThenUpdate(newName);
            } else {
                updateProfileOnServer(newName, null);
            }
        });

        btnCancel.setOnClickListener(v -> dismissDialog());

        dialog.show();
    }

    public void setSelectedAvatarUri(Uri uri) {
        this.selectedAvatarUri = uri;
        if (imgPreview != null && uri != null) {
            Glide.with(context)
                    .load(uri)
                    .circleCrop()
                    .into(imgPreview);
        }
    }

    private void uploadAvatarThenUpdate(String displayName) {
        try {
            InputStream is = context.getContentResolver().openInputStream(selectedAvatarUri);
            if (is == null) {
                updateProfileOnServer(displayName, null);
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            byte[] imageBytes = baos.toByteArray();
            is.close();

            String fileName = "avatar_" + UUID.randomUUID() + ".jpg";

            StorageAPIService storageService = RetrofitClient.getStorage(context)
                    .create(StorageAPIService.class);

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);

            storageService.uploadFileToStorage(
                    "image/jpeg",
                    "true",
                    Constants.AVATAR_BUCKET.replace("/", ""),
                    fileName,
                    requestBody
            ).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    String avatarUrl = Constants.STORAGE_BASE_URL + Constants.AVATAR_BUCKET + fileName;
                    updateProfileOnServer(displayName, avatarUrl);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    updateProfileOnServer(displayName, null);
                }
            });

        } catch (Exception e) {
            updateProfileOnServer(displayName, null);
        }
    }

    private void updateProfileOnServer(String displayName, String avatarUrl) {
        ProfileRepository repo = new ProfileRepository(context);

        Map<String, Object> updates = new HashMap<>();
        updates.put("display_name", displayName);
        if (avatarUrl != null) {
            updates.put("avatar_url", avatarUrl);
        }

        
        repo.updateProfile(currentProfile.getId(), updates);

        
        Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();

        currentProfile.setDisplayName(displayName);
        if (avatarUrl != null) currentProfile.setAvatarUrl(avatarUrl);

        SessionManager.getInstance(context).updateProfileInfo(displayName, avatarUrl);

        if (listener != null) listener.onProfileUpdated(currentProfile);
        dismissDialog();
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