package com.melodix.app.ViewModel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
    public LiveData<LoginResult> handleSocialLoginToken(String accessToken, String refreshToken) {
        // ĐÃ SỬA: Truyền getApplication() xuống để Repo có môi trường gọi SharedPreferences
        return repository.handleSocialLogin(accessToken, refreshToken, getApplication());
    }

    // Thêm vào AuthViewModel.java
    public MutableLiveData<String> uploadPendingAvatar(String userId, byte[] imageData, String fullName) {
        MutableLiveData<String> result = new MutableLiveData<>();
        repository.uploadPendingAvatarAndProfile(userId, imageData, fullName, result);
        return result;
    }

    public LiveData<String> resetPassword(String email) {
        return repository.resetPassword(email);
    }

    public LiveData<String> updateNewPassword(String accessToken, String newPassword) {
        return repository.updateNewPassword(accessToken, newPassword);
    }

    public LiveData<String> changePassword(String newPassword) {
        return repository.changePassword(newPassword);
    }
}