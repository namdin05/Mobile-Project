package com.melodix.app.View.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.melodix.app.View.admin.AdminActivity;
import com.melodix.app.BuildConfig;
import com.melodix.app.MainActivity;
import com.melodix.app.Utils.SessionManager; // IMPORT CLASS SESSION
import com.melodix.app.R;
import com.melodix.app.ViewModel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private EditText edtEmail, edtPassword;
    private Button btnLoginEmail, btnLoginGoogle, btnLoginFacebook;
    private TextView tvGoToRegister;

    private static final String BASE_URL = BuildConfig.BASE_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // =========================================================
        // KIỂM TRA AUTO-LOGIN BẰNG SESSION MANAGER
        // =========================================================
        SessionManager sessionManager = SessionManager.getInstance(this);

        // In log ra để xem rốt cuộc két sắt đang thiếu cái gì
        Log.e("DEBUG_SESSION", "Có Session không: " + sessionManager.hasSession());
        Log.e("DEBUG_SESSION", "User ID: " + sessionManager.getUserId());
        Log.e("DEBUG_SESSION", "Token: " + sessionManager.getAccessToken());
        if (sessionManager.hasSession()) {
            String role = sessionManager.getRole();
            if ("admin".equals(role)) {
                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
            } else {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            }
            finish();
            return; // Thoát ngay không load UI nữa
        }

        setContentView(R.layout.activity_login);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLoginEmail = findViewById(R.id.btnLoginEmail);
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
        btnLoginFacebook = findViewById(R.id.btnLoginFacebook);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        btnLoginEmail.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass = edtPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                authViewModel.login(email, pass).observe(this, loginResult -> {
                    if (loginResult.isSuccess()) {
                        String role = loginResult.getRole();

                        if ("admin".equals(role)) {
                            Toast.makeText(LoginActivity.this, "Xin chào Quản trị viên!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                        } else {
                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        }
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, loginResult.getErrorMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        btnLoginGoogle.setOnClickListener(v -> socialLogin("google"));
        btnLoginFacebook.setOnClickListener(v -> socialLogin("facebook"));

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        Uri uri = intent.getData();

        // ĐÃ SỬA: Kiểm tra chính xác scheme là "melodix" và host là "callback"
        if (uri != null && "melodix".equals(uri.getScheme()) && "callback".equals(uri.getHost())) {

            String fragment = uri.getFragment();
            if (fragment != null) {
                String[] params = fragment.split("&");
                String accessToken = null;

                for (String param : params) {
                    if (param.startsWith("access_token=")) {
                        accessToken = param.split("=")[1];
                        break;
                    }
                }

                if (accessToken != null) {
                    intent.setData(null); // Xóa link đi để khỏi lặp lại

                    Toast.makeText(this, "Waiting...", Toast.LENGTH_SHORT).show();
                    Log.e("SOCIAL_LOGIN", "0. Đã chộp được Token: " + accessToken);

                    authViewModel.handleSocialLoginToken(accessToken).observe(this, loginResult -> {
                        if (loginResult.isSuccess()) {
                            String role = loginResult.getRole();

                            if ("admin".equals(role)) {
                                Toast.makeText(this, "Hi! Admin.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                            } else {
                                Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            }
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, loginResult.getErrorMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }
    }

    private void socialLogin(String provider) {
        String authUrl = BASE_URL + "auth/v1/authorize?provider=" + provider + "&redirect_to=melodix://callback";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        startActivity(browserIntent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}