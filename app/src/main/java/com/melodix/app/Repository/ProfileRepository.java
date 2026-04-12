package com.melodix.app.Repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Profile;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileRepository {

    private ProfileAPIService profileAPIService;
    private final MutableLiveData<List<Profile>> _profiles = new MutableLiveData<>();
    public LiveData<List<Profile>> profiles = _profiles;

    private ProfileRepository() {
        profileAPIService = RetrofitClient.getClient().create(ProfileAPIService.class);
        fetchAllProfiles();
    }

    private void fetchAllProfiles() {
        profileAPIService.getAllProfiles(
                BuildConfig.API_KEY,
                "Bearer " + BuildConfig.SERVICE_KEY
        ).enqueue(new Callback<List<Profile>>() {

            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _profiles.setValue(response.body());
                } else {
                    _profiles.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                _profiles.setValue(null);
            }
        });
    }





}
