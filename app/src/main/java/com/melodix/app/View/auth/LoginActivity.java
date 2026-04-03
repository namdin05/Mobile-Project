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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.melodix.app.AdminActivity;
import com.melodix.app.ArtistActivity;
import com.melodix.app.BuildConfig;
import com.melodix.app.MainActivity;
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
        setContentView(R.layout.activity_login);

        // Khởi tạo ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLoginEmail = findViewById(R.id.btnLoginEmail);
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
        btnLoginFacebook = findViewById(R.id.btnLoginFacebook);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        // 1. Xử lý đăng nhập Email
        btnLoginEmail.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass = edtPassword.getText().toString().trim();
            if (!email.isEmpty() && !pass.isEmpty()) {
                btnLoginEmail.setEnabled(false); // khoa de ko bi spam, tao nhieu luong
                // GỌI VIEW MODEL VÀ QUAN SÁT KẾT QUẢ (LIVEDATA)
                authViewModel.login(email, pass, this).observe(LoginActivity.this, loginResult -> {
                    // BÊN TRONG HÀM authViewModel.login(...).observe(...)
                    if (loginResult.isSuccess()) {
                        // KIỂM TRA PHÂN QUYỀN TẠI ĐÂY
                        String role = loginResult.getRole();
                        Log.d("ROLE", role);

                        if ("admin".equals(role)) {
                            // 1. ADMIN -> Mở màn hình duyệt nhạc
                            Toast.makeText(this, "Xin chào Quản trị viên!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, AdminActivity.class));

                        } else if ("artist".equals(role)) {
                            // 2. ARTIST -> Mở không gian làm việc của Nghệ sĩ
                            Toast.makeText(this, "Chào mừng Nghệ sĩ trở lại!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, ArtistActivity.class));

                        } else {
                            // 3. USER (Mặc định) -> Mở trang nghe nhạc bình thường
                            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        }

                        finish(); // Đóng màn hình đăng nhập
                    } else {
                        // Báo lỗi
                        Toast.makeText(LoginActivity.this, loginResult.getErrorMessage(), Toast.LENGTH_LONG).show();
                        btnLoginEmail.setEnabled(true);
                    }
                });
            }
        });

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // 2. Xử lý đăng nhập Mạng xã hội (Cần dùng Intent nên giữ ở View)
        btnLoginGoogle.setOnClickListener(v -> socialLogin("google"));
        btnLoginFacebook.setOnClickListener(v -> socialLogin("facebook"));
    }

    private void socialLogin(String provider) {
        String authUrl = BASE_URL + "auth/v1/authorize?provider=" + provider + "&redirect_to=melodix://callback";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        startActivity(browserIntent);
    }

    // 3. Hứng Token khi Google/Facebook trả về App
    @Override
    protected void onResume() {
        super.onResume();
        Uri uri = getIntent().getData();
        if (uri != null && uri.getScheme().equals("melodix")) {
            // Dữ liệu Supabase trả về thường nằm ở dạng Fragment (#) thay vì Query (?)
            String fragment = uri.getFragment();
            if (fragment != null && fragment.contains("access_token=")) {
                // Tách chuỗi để lấy Access Token
                String[] params = fragment.split("&");
                for (String param : params) {
                    if (param.startsWith("access_token=")) {
                        String accessToken = param.split("=")[1];
                        Log.d("MELODIX_OAUTH", "Google/FB Token: " + accessToken);
                        Toast.makeText(this, "Đăng nhập MXH thành công!", Toast.LENGTH_SHORT).show();

                        // Chuyển sang màn hình chính
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                        break;
                    }
                }
            }
        }
    }
}