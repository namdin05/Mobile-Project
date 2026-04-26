package com.melodix.app.View.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.melodix.app.Constants;
import com.melodix.app.Utils.LoadingDialog;
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
    private TextView tvGoToRegister, tvForgotPassword;

    private LoadingDialog loadingDialog; // Khai báo

    private static final String BASE_URL = BuildConfig.BASE_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadingDialog = new LoadingDialog();

        // =========================================================
        // 1. KIỂM TRA DEEP LINK TRƯỚC KHI LÀM BẤT CỨ VIỆC GÌ
        // =========================================================
        Intent intent = getIntent();
        Uri uri = intent.getData();

        // Cờ đánh dấu xem app có đang được mở từ Email/Web không
        boolean isFromDeepLink = (uri != null && "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getHost().contains("github.io"));

        SessionManager sessionManager = SessionManager.getInstance(this);

        // =========================================================
        // 2. CHỈ AUTO-LOGIN NẾU KHÔNG PHẢI MỞ TỪ LINK EMAIL
        // =========================================================
        if (!isFromDeepLink && sessionManager.hasSession()) {
            String role = sessionManager.getRole();
            navigateToNextScreen(role);
            finish();
            return; // Thoát ngay không load UI nữa
        }

        setContentView(R.layout.activity_login);
        handleRedirectLink(getIntent());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLoginEmail = findViewById(R.id.btnLoginEmail);
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
        btnLoginFacebook = findViewById(R.id.btnLoginFacebook);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        tvForgotPassword.setOnClickListener(v -> {
            showForgotPasswordDialog();
        });

        btnLoginEmail.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass = edtPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                loadingDialog.showLoading(LoginActivity.this);
                authViewModel.login(email, pass).observe(this, loginResult -> {
                    if (loginResult.isSuccess()) {
                        String role = loginResult.getRole();
                        String userId = loginResult.getUserId();

                        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", MODE_PRIVATE);
                        String pendingAvatarBase64 = prefs.getString("PENDING_AVATAR", null);
                        String pendingFullName = prefs.getString("PENDING_FULL_NAME", null);

                        if (pendingAvatarBase64 != null && pendingFullName != null) {
                            Toast.makeText(LoginActivity.this, "Đang thiết lập hồ sơ của bạn...", Toast.LENGTH_SHORT).show();

                            // 1. Giải mã ngược chuỗi Base64 thành byte[]
                            byte[] imageBytes = android.util.Base64.decode(pendingAvatarBase64, android.util.Base64.DEFAULT);

                            // 2. Gọi hàm Upload
                            authViewModel.uploadPendingAvatar(userId, imageBytes, pendingFullName).observe(this, uploadResult -> {

                                // 3. Upload xong thì XÓA DỮ LIỆU TẠM đi để lần sau đăng nhập không bị up lại
                                prefs.edit().remove("PENDING_AVATAR").remove("PENDING_FULL_NAME").apply();

                                // 4. Chuyển vào màn hình chính
                                navigateToNextScreen(role);
                            });

                        } else {
                            // Nếu không có ảnh nào đang chờ (Đăng nhập bình thường), vào thẳng app
                            navigateToNextScreen(role);
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleRedirectLink(intent);
    }

    private void navigateToNextScreen(String role) {
        if ("admin".equals(role)) {
            Toast.makeText(LoginActivity.this, "Xin chào Quản trị viên!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, AdminActivity.class));
        } else {
            Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }
        loadingDialog.hideLoading();
    }

    private void showForgotPasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Khôi phục mật khẩu");
        builder.setMessage("Vui lòng nhập email đăng ký của bạn:");

        // Tạo một ô nhập liệu
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                Toast.makeText(this, "Đang xử lý...", Toast.LENGTH_SHORT).show();

                authViewModel.resetPassword(email).observe(this, result -> {
                    if ("SUCCESS".equals(result)) {
                        Toast.makeText(this, "Vui lòng kiểm tra email để đặt lại mật khẩu!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        Intent intent = getIntent();
//        Uri uri = intent.getData();
//
//        // ĐÃ SỬA: Kiểm tra chính xác scheme là "melodix" và host là "callback"
//        if (uri != null && "melodix".equals(uri.getScheme()) && "callback".equals(uri.getHost())) {
//
//            String fragment = uri.getFragment();
//            if (fragment != null) {
//                String[] params = fragment.split("&");
//                String accessToken = null;
//
//                for (String param : params) {
//                    if (param.startsWith("access_token=")) {
//                        accessToken = param.split("=")[1];
//                        break;
//                    }
//                }
//
//                if (accessToken != null) {
//                    intent.setData(null); // Xóa link đi để khỏi lặp lại
//
//                    Toast.makeText(this, "Waiting...", Toast.LENGTH_SHORT).show();
//                    Log.e("SOCIAL_LOGIN", "0. Đã chộp được Token: " + accessToken);
//
//                    authViewModel.handleSocialLoginToken(accessToken).observe(this, loginResult -> {
//                        if (loginResult.isSuccess()) {
//                            String role = loginResult.getRole();
//
//                            if ("admin".equals(role)) {
//                                Toast.makeText(this, "Hi! Admin.", Toast.LENGTH_SHORT).show();
//                                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
//                            } else {
//                                Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show();
//                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
//                            }
//                            finish();
//                        } else {
//                            Toast.makeText(LoginActivity.this, loginResult.getErrorMessage(), Toast.LENGTH_LONG).show();
//                        }
//                    });
//                }
//            }
//        }
//    }

//    private void socialLogin(String provider) {
//        String authUrl = BASE_URL + "auth/v1/authorize?provider=" + provider + "&redirect_to=melodix://callback";
//        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
//        startActivity(browserIntent);
//    }

    private void socialLogin(String provider) {
        // Đảm bảo link này khớp 100% với cái bạn khai báo trong Manifest và Supabase
        String redirectUrl = Constants.MELODIX_AUTH;

        String authUrl = BASE_URL + "auth/v1/authorize?provider=" + provider + "&redirect_to=" + Uri.encode(redirectUrl);

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        startActivity(browserIntent);
    }

    private void handleRedirectLink(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) return;

        // Kiểm tra xem link có phải từ trang GitHub của mình không
        if ("https".equals(uri.getScheme()) && uri.getHost().contains("github.io")) {

            String fragment = uri.getFragment(); // Lấy chuỗi phía sau dấu '#'

            if (fragment != null) {
                String accessToken = null;
                String refreshToken = null;
                String type = null;

                String[] params = fragment.split("&");
                for (String param : params) {
                    if (param.startsWith("access_token=")) {
                        accessToken = param.split("=")[1];
                    } else if (param.startsWith("refresh_token=")) {
                        refreshToken = param.split("=")[1]; // 2. Bắt lấy refresh_token
                    } else if (param.startsWith("type=")) {
                        type = param.split("=")[1];
                    }
                }

                if (accessToken != null) {
                    // Xóa data để tránh bị gọi lại nhiều lần nếu xoay màn hình
                    intent.setData(null);

                    if ("recovery".equals(type)) {
                        Toast.makeText(this, "Hãy nhập mật khẩu mới", Toast.LENGTH_SHORT).show();

                        Intent resetIntent = new Intent(this, ResetPasswordActivity.class);
                        resetIntent.putExtra("RECOVERY_TOKEN", accessToken);
                        startActivity(resetIntent);

                        // CỰC KỲ QUAN TRỌNG: Dừng hàm lại ngay tại đây!
                        return;
                    }

                    Toast.makeText(this, "Đang xác thực phiên đăng nhập...", Toast.LENGTH_SHORT).show();

                    // Tận dụng lại hàm handleSocialLoginToken đã viết để lấy Profile và lưu Session
                    authViewModel.handleSocialLoginToken(accessToken, refreshToken).observe(this, loginResult -> {
                        if (loginResult.isSuccess()) {
                            String role = loginResult.getRole();
                            navigateToNextScreen(role);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Lỗi lấy thông tin: " + loginResult.getErrorMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }
    }
}