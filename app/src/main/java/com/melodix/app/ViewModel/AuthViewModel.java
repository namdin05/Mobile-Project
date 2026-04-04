package com.melodix.app.ViewModel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.melodix.app.Model.LoginResult;
import com.melodix.app.Repository.auth.AuthRepository;

public class AuthViewModel extends ViewModel {

    private AuthRepository authRepository;

    public AuthViewModel() {
        authRepository = new AuthRepository();
    }

    // Hàm này được View gọi khi người dùng nhấn nút Đăng nhập
    public LiveData<LoginResult> login(String email, String password, Context context) {
        return authRepository.signIn(email, password, context);
    }

    public LiveData<String> register(String email, String password, String fullName) {
        return authRepository.signUp(email, password, fullName);
    }

    // Hàm này xử lý Token nhận được từ Google/Facebook
    public LiveData<LoginResult> handleSocialLoginToken(String token) {
        // Trả thẳng LiveData từ Repository lên cho Activity quan sát
        return authRepository.handleSocialLogin(token);
    }
}