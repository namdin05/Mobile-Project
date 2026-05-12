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
import com.melodix.app.Utils.SessionManager; 
import com.melodix.app.R;
import com.melodix.app.View.dialogs.ForgotPasswordDialog;
import com.melodix.app.ViewModel.AuthViewModel;

import java.net.URLDecoder;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private EditText edtEmail, edtPassword;
    private Button btnLoginEmail, btnLoginGoogle, btnLoginFacebook;
    private TextView tvGoToRegister, tvForgotPassword;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadingDialog = new LoadingDialog();

        Intent intent = getIntent();
        Uri uri = intent.getData();

        boolean isFromDeepLink = (uri != null && "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getHost().contains("github.io"));
        SessionManager sessionManager = SessionManager.getInstance(this);

        if (!isFromDeepLink && sessionManager.hasSession()) {
            String role = sessionManager.getRole();
            navigateToNextScreen(role);
            finish();
            return;
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
            ForgotPasswordDialog.newInstance().show(getSupportFragmentManager(), "forgot_password");
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

                        checkAndUploadPendingProfile(email, role, userId);
                    } else {
                        loadingDialog.hideLoading();
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
        finish();
    }

    private void checkAndUploadPendingProfile(String email, String role, String userId) {
        SharedPreferences prefs = getSharedPreferences("MelodixPrefs", MODE_PRIVATE);
        
        String pendingAvatarBase64 = prefs.getString("PENDING_AVATAR" + email, null);
        String pendingFullName = prefs.getString("PENDING_FULL_NAME" + email, null);

        if (pendingAvatarBase64 != null && pendingFullName != null) {
            byte[] imageBytes = android.util.Base64.decode(pendingAvatarBase64, android.util.Base64.DEFAULT);
            authViewModel.uploadPendingAvatar(userId, imageBytes, pendingFullName).observe(this, uploadResult -> {
                if (uploadResult != null && !uploadResult.startsWith("Lỗi")) {
                    
                    prefs.edit().remove("PENDING_AVATAR" + email).remove("PENDING_FULL_NAME" + email).apply();

                    
                    
                    SessionManager session = SessionManager.getInstance(LoginActivity.this);
                    session.saveLogInSession(
                            userId,
                            role,
                            session.getAccessToken(),
                            session.getRefreshToken(),
                            pendingFullName, 
                            uploadResult     
                    );

                    
                    navigateToNextScreen(role);
                } else {
                    
                    Toast.makeText(this, "Lỗi upload ảnh, bạn có thể cập nhật sau", Toast.LENGTH_SHORT).show();
                    navigateToNextScreen(role);
                }
            });
        } else {
            navigateToNextScreen(role);
        }
    }

    private void socialLogin(String provider) {
        String redirectUrl = Constants.MELODIX_AUTH;
        String authUrl = BuildConfig.BASE_URL + "auth/v1/authorize?provider=" + provider + "&redirect_to=" + Uri.encode(redirectUrl);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        startActivity(browserIntent);
    }

    private void handleRedirectLink(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) return;

        boolean isGithubLink = "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getHost().contains("github.io");
        boolean isFallbackLink = "melodix".equals(uri.getScheme()) && "auth".equals(uri.getHost());

        if (isGithubLink || isFallbackLink) {
            String fragment = uri.getFragment();
            if (fragment != null) {
                String accessToken = null;
                String refreshToken = null;
                String type = null;
                String errorDescription = null;

                String[] params = fragment.split("&");
                for (String param : params) {
                    if (param.startsWith("access_token=")) {
                        accessToken = param.split("=")[1];
                    } else if (param.startsWith("refresh_token=")) {
                        refreshToken = param.split("=")[1];
                    } else if (param.startsWith("type=")) {
                        type = param.split("=")[1];
                    } else if (param.startsWith("error_description=")) {
                        errorDescription = param.split("=")[1];
                    }
                }

                if (errorDescription != null) {
                    try {
                        String decodedError = URLDecoder.decode(errorDescription, "UTF-8").replace("+", " ");
                        if (decodedError.toLowerCase().contains("banned")) {
                            decodedError = "Tài khoản của bạn đã bị khóa bởi quản trị viên.";
                        }
                        Toast.makeText(this, decodedError, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Lỗi đăng nhập Social", Toast.LENGTH_SHORT).show();
                    }
                    intent.setData(null);
                    return;
                }

                if (accessToken != null) {
                    intent.setData(null);
                    if ("recovery".equals(type)) {
                        Intent resetIntent = new Intent(this, ResetPasswordActivity.class);
                        resetIntent.putExtra("RECOVERY_TOKEN", accessToken);
                        startActivity(resetIntent);
                        return;
                    } else if ("signup".equals(type)) {
                        Toast.makeText(this, "Xác thực Email thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    loadingDialog.showLoading(this);
                    authViewModel.handleSocialLoginToken(accessToken, refreshToken).observe(this, loginResult -> {
                        if (loginResult.isSuccess()) {
                            navigateToNextScreen(loginResult.getRole());
                        } else {
                            loadingDialog.hideLoading();
                            Toast.makeText(LoginActivity.this, loginResult.getErrorMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }
    }
}
