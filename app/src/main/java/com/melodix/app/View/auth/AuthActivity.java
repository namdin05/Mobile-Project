package com.melodix.app.View.auth;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;
import androidx.lifecycle.ViewModelProvider;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.melodix.app.Data.Resource;
import com.melodix.app.R;
import com.melodix.app.Utils.InputValidator;
import com.melodix.app.View.home.HomeActivity;
import com.melodix.app.ViewModel.AuthViewModel;
import com.melodix.app.databinding.ActivityAuthBinding;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AuthActivity extends AppCompatActivity {

    private static final String TAG = "AuthActivity";

    private ActivityAuthBinding binding;
    private AuthViewModel authViewModel;
    private CallbackManager callbackManager;
    private CredentialManager credentialManager;
    private final Executor credentialExecutor = Executors.newSingleThreadExecutor();

    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            binding = ActivityAuthBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            credentialManager = CredentialManager.create(this);
            authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

            setupFacebookCallback();
            setupActions();
            observeViewModel();
            updateModeUi(true);
        } catch (Exception e) {
            Log.e(TAG, "onCreate failed.", e);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                navigateToHome();
            }
        } catch (Exception e) {
            Log.e(TAG, "onStart auth check failed.", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            if (callbackManager != null) {
                callbackManager.onActivityResult(requestCode, resultCode, data);
            }
        } catch (Exception e) {
            Log.e(TAG, "Facebook onActivityResult failed.", e);
        }
    }

    private void setupActions() {
        binding.btnTabLogin.setOnClickListener(v -> updateModeUi(true));
        binding.btnTabRegister.setOnClickListener(v -> updateModeUi(false));

        binding.btnLogin.setOnClickListener(v -> handleEmailLogin());
        binding.btnRegister.setOnClickListener(v -> handleEmailRegister());

        binding.btnGoogle.setOnClickListener(v -> beginGoogleSignIn(true));
        binding.btnFacebook.setOnClickListener(v -> beginFacebookSignIn());
    }

    private void setupFacebookCallback() {
        try {
            callbackManager = CallbackManager.Factory.create();

            LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    try {
                        if (loginResult == null || loginResult.getAccessToken() == null) {
                            showLoading(false);
                            showMessage("Không nhận được access token từ Facebook.");
                            return;
                        }

                        String accessToken = loginResult.getAccessToken().getToken();
                        if (TextUtils.isEmpty(accessToken)) {
                            showLoading(false);
                            showMessage("Access token Facebook không hợp lệ.");
                            return;
                        }

                        authViewModel.loginWithFacebook(accessToken);
                    } catch (Exception e) {
                        Log.e(TAG, "Facebook onSuccess handling failed.", e);
                        showLoading(false);
                        showMessage("Đăng nhập Facebook thất bại.");
                    }
                }

                @Override
                public void onCancel() {
                    showLoading(false);
                    showMessage("Bạn đã hủy đăng nhập Facebook.");
                }

                @Override
                public void onError(@NonNull FacebookException error) {
                    Log.e(TAG, "Facebook login error.", error);
                    showLoading(false);
                    showMessage("Đăng nhập Facebook thất bại: " + safeMessage(error.getLocalizedMessage()));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "setupFacebookCallback failed.", e);
        }
    }

    private void observeViewModel() {
        authViewModel.getAuthState().observe(this, resource -> {
            if (resource == null) {
                return;
            }

            Resource.Status status = resource.getStatus();
            if (status == null) {
                showLoading(false);
                return;
            }

            switch (status) {
                case IDLE:
                    showLoading(false);
                    break;

                case LOADING:
                    showLoading(true);
                    break;

                case SUCCESS:
                    showLoading(false);
                    navigateToHome();
                    break;

                case ERROR:
                    showLoading(false);
                    showMessage(safeMessage(resource.getMessage()));
                    break;
            }
        });
    }

    private void handleEmailLogin() {
        try {
            String email = getTextSafely(binding.etLoginEmail);
            String password = getTextSafely(binding.etLoginPassword);

            String validationError = InputValidator.validateLogin(email, password);
            if (!TextUtils.isEmpty(validationError)) {
                showMessage(validationError);
                return;
            }

            authViewModel.loginWithEmail(email, password);
        } catch (Exception e) {
            Log.e(TAG, "handleEmailLogin failed.", e);
            showMessage("Không thể đăng nhập bằng email lúc này.");
        }
    }

    private void handleEmailRegister() {
        try {
            String fullName = getTextSafely(binding.etRegisterFullName);
            String email = getTextSafely(binding.etRegisterEmail);
            String password = getTextSafely(binding.etRegisterPassword);
            String confirmPassword = getTextSafely(binding.etRegisterConfirmPassword);

            String validationError = InputValidator.validateRegister(fullName, email, password, confirmPassword);
            if (!TextUtils.isEmpty(validationError)) {
                showMessage(validationError);
                return;
            }

            authViewModel.registerWithEmail(fullName, email, password);
        } catch (Exception e) {
            Log.e(TAG, "handleEmailRegister failed.", e);
            showMessage("Không thể tạo tài khoản lúc này.");
        }
    }

    private void beginGoogleSignIn(boolean filterAuthorizedAccounts) {
        showLoading(true);

        try {
            String webClientId = getGoogleWebClientId();
            if (TextUtils.isEmpty(webClientId)) {
                showLoading(false);
                showMessage("Thiếu default_web_client_id. Hãy bật Google Sign-In trong Firebase rồi tải lại google-services.json.");
                return;
            }

            GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(filterAuthorizedAccounts)
                    .setServerClientId(webClientId)
                    .build();

            GetCredentialRequest request = new GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build();

            credentialManager.getCredentialAsync(
                    this,
                    request,
                    new CancellationSignal(),
                    credentialExecutor,
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(@NonNull GetCredentialResponse result) {
                            handleGoogleCredentialResult(result);
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            Log.e(TAG, "Google getCredentialAsync failed.", e);

                            if (filterAuthorizedAccounts && e instanceof NoCredentialException) {
                                beginGoogleSignIn(false);
                                return;
                            }

                            runOnUiThread(() -> {
                                showLoading(false);
                                if (e instanceof NoCredentialException) {
                                    showMessage("Không tìm thấy tài khoản Google phù hợp trên thiết bị.");
                                } else {
                                    showMessage("Đăng nhập Google thất bại: " + safeMessage(e.getLocalizedMessage()));
                                }
                            });
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "beginGoogleSignIn exception.", e);
            showLoading(false);
            showMessage("Không thể khởi chạy đăng nhập Google.");
        }
    }

    private void handleGoogleCredentialResult(@NonNull GetCredentialResponse response) {
        try {
            Credential credential = response.getCredential();

            if (credential instanceof CustomCredential) {
                CustomCredential customCredential = (CustomCredential) credential;

                if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
                    GoogleIdTokenCredential googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(customCredential.getData());

                    String idToken = googleIdTokenCredential.getIdToken();

                    runOnUiThread(() -> {
                        if (TextUtils.isEmpty(idToken)) {
                            showLoading(false);
                            showMessage("Không lấy được Google ID token.");
                            return;
                        }

                        authViewModel.loginWithGoogle(idToken);
                    });
                    return;
                }
            }

            runOnUiThread(() -> {
                showLoading(false);
                showMessage("Credential trả về không đúng định dạng Google.");
            });
        } catch (GoogleIdTokenParsingException e) {
            Log.e(TAG, "GoogleIdTokenParsingException.", e);
            runOnUiThread(() -> {
                showLoading(false);
                showMessage("Không thể phân tích Google ID token.");
            });
        } catch (Exception e) {
            Log.e(TAG, "handleGoogleCredentialResult failed.", e);
            runOnUiThread(() -> {
                showLoading(false);
                showMessage("Đăng nhập Google thất bại.");
            });
        }
    }

    private void beginFacebookSignIn() {
        showLoading(true);

        try {
            try {
                LoginManager.getInstance().logOut();
            } catch (Exception ignored) {
            }

            LoginManager.getInstance().logInWithReadPermissions(
                    this,
                    Arrays.asList("email", "public_profile")
            );
        } catch (Exception e) {
            Log.e(TAG, "beginFacebookSignIn failed.", e);
            showLoading(false);
            showMessage("Không thể khởi chạy đăng nhập Facebook.");
        }
    }

    private String getGoogleWebClientId() {
        try {
            int resId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
            if (resId == 0) {
                return "";
            }

            String value = getString(resId);
            return value == null ? "" : value.trim();
        } catch (Exception e) {
            Log.e(TAG, "getGoogleWebClientId failed.", e);
            return "";
        }
    }

    private void updateModeUi(boolean loginMode) {
        try {
            isLoginMode = loginMode;

            binding.layoutLoginSection.setVisibility(loginMode ? View.VISIBLE : View.GONE);
            binding.layoutRegisterSection.setVisibility(loginMode ? View.GONE : View.VISIBLE);

            int selectedBg = getColor(R.color.spotify_green);
            int unselectedBg = getColor(R.color.spotify_mid_surface);
            int selectedText = getColor(R.color.spotify_black);
            int unselectedText = getColor(R.color.spotify_text_primary);

            binding.btnTabLogin.setBackgroundTintList(ColorStateList.valueOf(loginMode ? selectedBg : unselectedBg));
            binding.btnTabLogin.setTextColor(loginMode ? selectedText : unselectedText);

            binding.btnTabRegister.setBackgroundTintList(ColorStateList.valueOf(loginMode ? unselectedBg : selectedBg));
            binding.btnTabRegister.setTextColor(loginMode ? unselectedText : selectedText);
        } catch (Exception e) {
            Log.e(TAG, "updateModeUi failed.", e);
        }
    }

    private void showLoading(boolean loading) {
        try {
            binding.layoutLoadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);

            binding.btnTabLogin.setEnabled(!loading);
            binding.btnTabRegister.setEnabled(!loading);
            binding.btnLogin.setEnabled(!loading);
            binding.btnRegister.setEnabled(!loading);
            binding.btnGoogle.setEnabled(!loading);
            binding.btnFacebook.setEnabled(!loading);
        } catch (Exception e) {
            Log.e(TAG, "showLoading failed.", e);
        }
    }

    private void navigateToHome() {
        try {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "navigateToHome failed.", e);
        }
    }

    private void showMessage(String message) {
        try {
            Snackbar.make(binding.getRoot(), safeMessage(message), Snackbar.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "showMessage failed.", e);
        }
    }

    private String getTextSafely(@Nullable com.google.android.material.textfield.TextInputEditText editText) {
        try {
            if (editText == null || editText.getText() == null) {
                return "";
            }
            return editText.getText().toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "getTextSafely failed.", e);
            return "";
        }
    }

    private String safeMessage(String message) {
        return TextUtils.isEmpty(message) ? "Đã xảy ra lỗi không xác định." : message;
    }
}