package com.melodix.app.ViewModel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.melodix.app.Model.Profile;
import com.melodix.app.Repository.ProfileRepository;

public class ProfileViewModel extends AndroidViewModel {
    private ProfileRepository repository;

    // ĐÃ SỬA: Khởi tạo ngay để LiveData không bao giờ bị null
    private final MutableLiveData<Profile> profile = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutStatus = new MutableLiveData<>(false);

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new ProfileRepository(application);
    }

    public LiveData<Boolean> getLogoutStatus() {
        return logoutStatus;
    }

    // ĐÃ SỬA 1: Hàm getProfile() giờ đây chỉ làm đúng 1 việc là trả về LiveData
    // Tuyệt đối không gọi API ở hàm này để tuân thủ chuẩn MVVM
    public LiveData<Profile> getProfile() {
        return profile;
    }

    // ĐÃ SỬA 2: Đưa toàn bộ logic gọi mạng vào đây.
    public void loadProfileInfo() {
        String uid = repository.getCurrentUserId();
        Log.e("DEBUG_PROFILE", "0. UID lấy từ bộ nhớ máy là: [" + uid + "]");

        if (!uid.isEmpty()) {
            LiveData<Profile> repoLiveData = repository.fetchProfileById(uid);

            Observer<Profile> observer = new Observer<Profile>() {
                @Override
                public void onChanged(Profile fetchedProfile) {
                    profile.setValue(fetchedProfile);
                    repoLiveData.removeObserver(this);
                }
            };
            repoLiveData.observeForever(observer);
        } else {
            Log.e("DEBUG_PROFILE", "LỖI: UID trống trơn -> Đẩy null về cho Activity");
            profile.setValue(null);
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