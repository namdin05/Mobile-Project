package com.melodix.app.ViewModel.admin;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.melodix.app.Model.AuditLog;
import com.melodix.app.Repository.admin.AdminRepository;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AdminLogViewModel extends AndroidViewModel {
    private AdminRepository repository;

    public AdminLogViewModel(@NotNull Application application) {
        super(application);
        repository = new AdminRepository(application);
    }

    public LiveData<List<AuditLog>> fetchAuditLogs() {
        return repository.fetchAuditLogs();
    }
}
