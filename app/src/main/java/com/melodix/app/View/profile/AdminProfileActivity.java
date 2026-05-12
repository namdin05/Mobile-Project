package com.melodix.app.View.profile;

import com.melodix.app.Constants;
import com.melodix.app.R;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Service.StorageAPIService;
import com.melodix.app.Utils.SessionManager;
import com.melodix.app.View.dialogs.ChangePasswordDialog;
import com.melodix.app.ViewModel.AuthViewModel;

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

public class AdminProfileActivity extends AppCompatActivity {

    private ImageView btnBack, imgProfileAvatar;
    private MaterialButton btnChangeAvatar;
    private TextInputEditText edtDisplayName;
    private MaterialButton btnSaveChanges, btnChangePassword;

    private String adminUid = "";
    private Uri selectedImageUri = null;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_profile);

        
        btnBack = findViewById(R.id.btnBack);
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        edtDisplayName = findViewById(R.id.edtDisplayName);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        

        adminUid = SessionManager.getInstance(getApplicationContext()).getUserId();

        
        String currentName = getIntent().getStringExtra("CURRENT_NAME");
        String currentAvatarUrl = getIntent().getStringExtra("CURRENT_AVATAR");

        if (currentName != null) edtDisplayName.setText(currentName);
        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
            Glide.with(this).load(currentAvatarUrl).into(imgProfileAvatar);
        }

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        
                        selectedImageUri = result.getData().getData();
                        
                        Glide.with(this).load(selectedImageUri).into(imgProfileAvatar);
                    }
                }
        );

        
        btnBack.setOnClickListener(v -> finish());

        
        btnChangeAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        
        btnSaveChanges.setOnClickListener(v -> {
            String newName = edtDisplayName.getText().toString().trim();
            if (newName.isEmpty()) {
                edtDisplayName.setError("Tên không được để trống");
                return;
            }

            btnSaveChanges.setEnabled(false);
            btnSaveChanges.setText("Đang lưu...");

            
            if (selectedImageUri != null) {
                uploadImageAndSaveProfile(newName);
            } else {
                
                updateDatabaseOnly(newName, null);
            }


        });

        btnChangePassword.setOnClickListener(v -> {
            ChangePasswordDialog.newInstance().show(getSupportFragmentManager(), "change_password");
        });

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    private void uploadImageAndSaveProfile(String newName) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] imageBytes = byteBuffer.toByteArray();

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);

            String fileName = adminUid + "_" + System.currentTimeMillis() + ".jpg";

            StorageAPIService apiService = RetrofitClient.getStorage(getApplicationContext()).create(StorageAPIService.class);

            apiService.uploadFileToStorage(
                            "image/jpeg",
                            "true",
                            Constants.AVATAR_BUCKET.replace("/", ""),
                            fileName,
                            requestBody
                    )
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {

                                String newImageUrl = Constants.STORAGE_BASE_URL + Constants.AVATAR_BUCKET + fileName;

                                SessionManager.getInstance(getApplicationContext()).updateAvatar(newImageUrl);

                                updateDatabaseOnly(newName, newImageUrl);
                            } else {
                                Toast.makeText(AdminProfileActivity.this, "Lỗi tải ảnh lên Storage", Toast.LENGTH_SHORT).show();
                                resetButton();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(AdminProfileActivity.this, "Lỗi mạng khi upload", Toast.LENGTH_SHORT).show();
                            resetButton();
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi xử lý file ảnh", Toast.LENGTH_SHORT).show();
            resetButton();
        }
    }

    private void updateDatabaseOnly(String newName, String newAvatarUrl) {
        ProfileAPIService apiService = RetrofitClient.getClient(getApplicationContext()).create(ProfileAPIService.class);
        String idFilter = "eq." + adminUid;

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("display_name", newName);
        if (newAvatarUrl != null) {
            updateData.put("avatar_url", newAvatarUrl);
        }

        apiService.updateProfile(idFilter, updateData).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    SessionManager.getInstance(getApplicationContext()).updateDisplayName(newName);

                    Toast.makeText(AdminProfileActivity.this, "Lưu thông tin thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AdminProfileActivity.this, "Lỗi cập nhật Database", Toast.LENGTH_SHORT).show();
                    resetButton();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(AdminProfileActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                resetButton();
            }
        });
    }

    private void resetButton() {
        btnSaveChanges.setEnabled(true);
        btnSaveChanges.setText("Save Changes");
    }
}