package com.melodix.app.ViewModel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.melodix.app.Model.LoginResult;
import com.melodix.app.Repository.auth.AuthRepository;

import org.jetbrains.annotations.NotNull;

public class AuthViewModel extends AndroidViewModel {

    private AuthRepository repository;

    public AuthViewModel(@NotNull Application application) {
        super(application);
        // Khởi tạo Repo với Application Context an toàn
        repository = new AuthRepository(application);
    }

    // ĐÃ SỬA: Xóa tham số Context ở đây đi.
    // Dùng trực tiếp getApplication() có sẵn của AndroidViewModel để truyền xuống Repo
    public LiveData<LoginResult> login(String email, String password) {
        return repository.signIn(email, password, getApplication());
    }

    public LiveData<String> register(String email, String password, String fullName) {
        return repository.signUp(email, password, fullName);
    }

    // Hàm này xử lý Token nhận được từ Google/Facebook
    public LiveData<LoginResult> handleSocialLoginToken(String token) {
        // ĐÃ SỬA: Truyền getApplication() xuống để Repo có môi trường gọi SharedPreferences
        return repository.handleSocialLogin(token, getApplication());
    }
}