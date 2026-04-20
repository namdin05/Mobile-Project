package com.melodix.app.ViewModel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.messaging.FirebaseMessaging;
import com.melodix.app.Model.Profile;
import com.melodix.app.Repository.ProfileRepository;

public class ProfileViewModel extends AndroidViewModel {
    private ProfileRepository repository;

    private MutableLiveData<Profile> profile = new MutableLiveData<>();
    private MutableLiveData<Boolean> logoutStatus = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        // Khởi tạo Repo
        repository = new ProfileRepository(application);
    }

    public LiveData<Profile> getProfile() {
        return profile;
    }

    public LiveData<Boolean> getLogoutStatus() {
        return logoutStatus;
    }

    public void loadProfileInfo() {
        // Tận dụng 2 hàm đã có sẵn bên ProfileRepository
        String uid = repository.getCurrentUserId();

        if (!uid.isEmpty()) {
            repository.fetchProfileById(uid).observeForever(profile -> {
                this.profile.setValue(profile);
            });
        } else {
            this.profile.setValue(null);
        }
    }

    public void updateTokenToServer(String token) {
        repository.updateTokenToServer(token);
    }

    public void performLogout() {
        repository.clearSession();
        logoutStatus.setValue(true);
    }
}
