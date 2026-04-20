package com.melodix.app.ViewModel.admin;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.melodix.app.Model.Profile;
import com.melodix.app.Repository.ProfileRepository;

import java.util.List;

public class AdminUserViewModel extends AndroidViewModel {
    private ProfileRepository profileRepository;
    private LiveData<List<Profile>> allProfile;

    public AdminUserViewModel(@NonNull Application application) {
        super(application);

        profileRepository = new ProfileRepository(application);
    }

    public LiveData<List<Profile>> getAllProfiles() {
        if (allProfile == null) {
            allProfile = profileRepository.fetchAllProfiles();
        }
        return allProfile;
    }
}
