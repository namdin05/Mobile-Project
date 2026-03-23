package com.melodix.app.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.melodix.app.Repository.auth.AuthRepository;

public class AuthViewModel extends ViewModel {

    private AuthRepository authRepository;

    public AuthViewModel() {
        authRepository = new AuthRepository();
    }

    // Hàm này được View gọi khi người dùng nhấn nút Đăng nhập
    public LiveData<String> login(String email, String password) {
        return authRepository.signInWithEmail(email, password);
    }

    // Hàm này xử lý Token nhận được từ Google/Facebook
    public void saveOAuthToken(String token) {
        // TODO: Xử lý lưu token vào SharedPreferences
    }
}