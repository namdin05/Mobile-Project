package com.melodix.app.View.auth;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.melodix.app.Constants;
import com.melodix.app.R;
import com.melodix.app.Utils.LoadingDialog;
import com.melodix.app.ViewModel.AuthViewModel;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class RegisterActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;

    private EditText edtFullName, edtEmail, edtPassword, edtConfirmPassword;
    private Button btnSignUp;
    private ImageView btnBack, imgAvatar;
    private TextView tvGoToLogin;
    private LoadingDialog loadingDialog;

    private byte[] imageBytes = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    Glide.with(RegisterActivity.this)
                            .load(selectedImageUri)
                            .into(imgAvatar);

                    imageBytes = getBytesFromUri(selectedImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        loadingDialog = new LoadingDialog();
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtRegisterEmail);
        edtPassword = findViewById(R.id.edtRegisterPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnBack = findViewById(R.id.btnBack);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
        imgAvatar = findViewById(R.id.imgAvatar);

        btnBack.setOnClickListener(v -> finish());
        tvGoToLogin.setOnClickListener(v -> finish());

        imgAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnSignUp.setOnClickListener(v -> {
            String fullName = edtFullName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < Constants.MINIMUM_LENGTH_PASSWORD) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất " + Constants.MINIMUM_LENGTH_PASSWORD + " ký tự", Toast.LENGTH_SHORT).show();
                return;
            }

            if (imageBytes == null) {
                Toast.makeText(this, "Vui lòng chọn ảnh đại diện", Toast.LENGTH_SHORT).show();
                return;
            }

            loadingDialog.showLoading(this);

            authViewModel.register(email, password, fullName).observe(this, new Observer<String>() {
                @Override
                public void onChanged(String result) {
                    loadingDialog.hideLoading();
                    if (result.startsWith("ERROR")) {
                        Toast.makeText(RegisterActivity.this, result, Toast.LENGTH_SHORT).show();
                    } else {
                        String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putString("PENDING_AVATAR" + email, base64Image)
                                .putString("PENDING_FULL_NAME" + email, fullName)
                                .apply();

                        Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Hãy kiểm tra Email để xác thực.", Toast.LENGTH_LONG).show();

                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    }
                }
            });
        });
    }

    private byte[] getBytesFromUri(Uri uri) {
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
}