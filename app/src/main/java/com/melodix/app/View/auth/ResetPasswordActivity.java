package com.melodix.app.View.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.melodix.app.R;
import com.melodix.app.ViewModel.AuthViewModel;

public class ResetPasswordActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private String recoveryToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password); // Tự tạo file XML này nhé

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Lấy Token từ LoginActivity truyền sang
        recoveryToken = getIntent().getStringExtra("RECOVERY_TOKEN");

        EditText edtNewPassword = findViewById(R.id.edtNewPassword);
        EditText edtConfirm = findViewById(R.id.edtConfirmNewPassword);
        Button btnSave = findViewById(R.id.btnSavePassword);

        btnSave.setOnClickListener(v -> {
            String newPass = edtNewPassword.getText().toString();
            String confirm = edtConfirm.getText().toString();

            if (newPass.length() < 6) {
                Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirm)) {
                Toast.makeText(this, "Mật khẩu không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi API đổi mật khẩu
            authViewModel.updateNewPassword(recoveryToken, newPass).observe(this, result -> {
                if ("SUCCESS".equals(result)) {
                    Toast.makeText(this, "Đổi mật khẩu thành công! Hãy đăng nhập lại.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}