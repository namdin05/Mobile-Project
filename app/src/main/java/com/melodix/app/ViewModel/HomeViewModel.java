package com.melodix.app.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Data.Resource;
import com.melodix.app.Model.HomeDashboard;
import com.melodix.app.Repository.HomeRepository;
import com.melodix.app.Repository.RepositoryCallback;

public class HomeViewModel extends AndroidViewModel {

    private final HomeRepository homeRepository;
    private final MutableLiveData<Resource<HomeDashboard>> homeState =
            new MutableLiveData<>(Resource.<HomeDashboard>idle());

    private boolean hasLoadedOnce = false;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        homeRepository = new HomeRepository();
    }

    public LiveData<Resource<HomeDashboard>> getHomeState() {
        return homeState;
    }

    public boolean hasLoadedOnce() {
        return hasLoadedOnce;
    }

    public void loadHomeDashboard(boolean forceRefresh) {
        if (hasLoadedOnce && !forceRefresh) {
            return;
        }

        homeState.postValue(Resource.<HomeDashboard>loading());

        homeRepository.loadHomeDashboard(new RepositoryCallback<HomeDashboard>() {
            @Override
            public void onSuccess(HomeDashboard data) {
                hasLoadedOnce = true;
                homeState.postValue(Resource.success(data));
            }

            @Override
            public void onError(String message) {
                homeState.postValue(Resource.error(message));
            }
        });
    }
}