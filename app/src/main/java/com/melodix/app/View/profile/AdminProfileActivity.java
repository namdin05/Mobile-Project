package com.melodix.app.View.profile;

import com.melodix.app.BuildConfig;
import com.melodix.app.Constants;
import com.melodix.app.R;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
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
    private Uri selectedImageUri = null; // Biến lưu ảnh vừa chọn từ thư viện

    // Khai báo công cụ bắt sự kiện chọn ảnh
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_profile);

        // Ánh xạ View
        btnBack = findViewById(R.id.btnBack);
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        edtDisplayName = findViewById(R.id.edtDisplayName);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Lấy UID của Admin hiện tại

        adminUid = SessionManager.getInstance(getApplicationContext()).getUserId();

        // Nhận dữ liệu cũ từ AdminActivity
        String currentName = getIntent().getStringExtra("CURRENT_NAME");
        String currentAvatarUrl = getIntent().getStringExtra("CURRENT_AVATAR");

        if (currentName != null) edtDisplayName.setText(currentName);
        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
            Glide.with(this).load(currentAvatarUrl).into(imgProfileAvatar);
        }

        // ==========================================
        // 1. CÀI ĐẶT CÔNG CỤ CHỌN ẢNH TỪ THƯ VIỆN
        // ==========================================
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // Lấy đường dẫn ảnh vừa chọn
                        selectedImageUri = result.getData().getData();
                        // Tạm thời hiển thị ảnh đó lên giao diện (chưa upload)
                        Glide.with(this).load(selectedImageUri).into(imgProfileAvatar);
                    }
                }
        );

        // Nút Back
        btnBack.setOnClickListener(v -> finish());

        // Nút Mở thư viện ảnh
        btnChangeAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // Nút Lưu thay đổi
        btnSaveChanges.setOnClickListener(v -> {
            String newName = edtDisplayName.getText().toString().trim();
            if (newName.isEmpty()) {
                edtDisplayName.setError("Tên không được để trống");
                return;
            }

            btnSaveChanges.setEnabled(false);
            btnSaveChanges.setText("Đang lưu...");

            // Nếu user có chọn ảnh mới -> Phải Upload ảnh trước
            if (selectedImageUri != null) {
                uploadImageAndSaveProfile(newName);
            } else {
                // Nếu chỉ đổi tên, không đổi ảnh -> Cập nhật Database luôn
                updateDatabaseOnly(newName, null);
            }
        });

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    // ==========================================
    // 2. LOGIC UPLOAD ẢNH LÊN STORAGE
    // ==========================================
    private void uploadImageAndSaveProfile(String newName) {
        try {
            // Chuyển ảnh từ thẻ nhớ thành mảng byte nhị phân để gửi qua mạng
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] imageBytes = byteBuffer.toByteArray();

            // Đóng gói mảng byte thành RequestBody
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);

            // Tạo tên file an toàn (Kèm thời gian để chống lỗi Cache)
            String fileName = adminUid + "_" + System.currentTimeMillis() + ".jpg";

            StorageAPIService apiService = RetrofitClient.getStorage(getApplicationContext()).create(StorageAPIService.class);

            // Gọi API ném ảnh lên Supabase
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

    // ==========================================
    // 3. LOGIC CẬP NHẬT TÊN VÀ LINK VÀO DATABASE
    // ==========================================
    private void updateDatabaseOnly(String newName, String newAvatarUrl) {
        ProfileAPIService apiService = RetrofitClient.getClient(getApplicationContext()).create(ProfileAPIService.class);
        String idFilter = "eq." + adminUid;

        // Chuẩn bị dữ liệu cập nhật
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("display_name", newName);
        if (newAvatarUrl != null) {
            updateData.put("avatar_url", newAvatarUrl);
        }

        apiService.updateProfile(idFilter, updateData).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminProfileActivity.this, "Lưu thông tin thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Thành công thì đóng màn hình
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

    // Nơi bạn xử lý sự kiện click vào nút Đổi Mật Khẩu (ví dụ trong ProfileFragment hoặc SettingsActivity)
    private void showChangePasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this); // Dùng requireContext() nếu ở Fragment
        builder.setTitle("Đổi mật khẩu");

        // Tạo layout gồm 2 ô nhập liệu
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText edtNewPass = new EditText(this);
        edtNewPass.setHint("Mật khẩu mới (Tối thiểu 6 ký tự)");
        edtNewPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(edtNewPass);

        final EditText edtConfirmPass = new EditText(this);
        edtConfirmPass.setHint("Xác nhận mật khẩu mới");
        edtConfirmPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(edtConfirmPass);

        builder.setView(layout);

        builder.setPositiveButton("Cập nhật", (dialog, which) -> {
            String newPass = edtNewPass.getText().toString();
            String confirmPass = edtConfirmPass.getText().toString();

            if (newPass.length() < 6) {
                Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Đang xử lý...", Toast.LENGTH_SHORT).show();

            // Gọi ViewModel
            authViewModel.changePassword(newPass).observe(this, result -> {
                if ("SUCCESS".equals(result)) {
                    Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                    // Tùy chọn: Có thể ép người dùng đăng xuất để họ login lại bằng MK mới
                } else {
                    Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                }
            });
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}