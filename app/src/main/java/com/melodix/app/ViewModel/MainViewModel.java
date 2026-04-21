package com.melodix.app.ViewModel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.melodix.app.Model.AppUser;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.AppRepository;
import java.util.ArrayList;

public class MainViewModel extends AndroidViewModel {
    private final AppRepository repository;
    public final MutableLiveData<AppUser> currentUser = new MutableLiveData<>();
    public final MutableLiveData<ArrayList<Song>> trending = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = AppRepository.getInstance(application);
        refresh();
    }

    public void refresh() {
//        currentUser.setValue(repository.getCurrentUser());
        // Thay vì gọi Top Trending, giờ ta gọi All Approved Songs
        trending.setValue(repository.getAllApprovedSongs());
    }
}