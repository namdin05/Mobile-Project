package com.melodix.admin.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.admin.repository.AdminRepository;
import com.melodix.admin.model.AppMetric;
import com.melodix.admin.model.ArtistRequest;

import java.util.List;

public class AdminStatViewModel extends AndroidViewModel {
    private AdminRepository repository;
    private LiveData<List<AppMetric>> allAppMetrics;

    private MutableLiveData<Boolean> actionSuccess = new MutableLiveData<>();
    private MutableLiveData<String> actionMessage = new MutableLiveData<>();
    
    public LiveData<Boolean> getActionSuccess() { return actionSuccess; }
    public LiveData<String> getActionMessage() { return actionMessage; }

    public AdminStatViewModel(@NonNull Application application) {
        super(application);
        repository = new AdminRepository(application);
    }

    public LiveData<List<AppMetric>> getAllAppMetrics() {
        if (allAppMetrics == null) {
            allAppMetrics = repository.fetchAllAppMetrics();
        }
        return allAppMetrics;
    }

    public LiveData<List<ArtistRequest>> getPendingArtistRequests() {
        return repository.fetchPendingArtistRequests();
    }

    public void processRequest(ArtistRequest request, String newStatus) {
        repository.processArtistRequest(request, newStatus, actionSuccess, actionMessage);
    }
}