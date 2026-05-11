package com.melodix.app.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Model.Album;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.AlbumRepository;

import java.util.List;

public class AlbumViewModel extends AndroidViewModel {

    private AlbumRepository albumRepository;
    private MutableLiveData<String> updateStatusResult = new MutableLiveData<>();

    public AlbumViewModel(@NonNull Application application) {
        super(application);
        albumRepository = new AlbumRepository(application);
    }

    public LiveData<List<Album>> getAllAlbums() {
        return albumRepository.getAllAlbums();
    }

    public LiveData<List<Song>> getSongsByAlbumId(String id) {
        return albumRepository.getSongsByAlbumId(id);
    }

    public void updateAlbumStatus(String albumId, String newStatus) {

        // Gọi sang Repository và tự định nghĩa cách xử lý kết quả
        albumRepository.updateAlbumStatus(albumId, newStatus, new retrofit2.Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Cập nhật thành công -> Báo cho Fragment chữ "approved" hoặc "rejected"
                    updateStatusResult.setValue(newStatus);
                } else {
                    // Lỗi từ server -> Báo chữ "error"
                    updateStatusResult.setValue("error");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                // Lỗi đứt mạng -> Báo chữ "error"
                updateStatusResult.setValue("error");
            }
        });
    }

    public LiveData<String> getUpdateStatusResult() {
        return updateStatusResult;
    }

}
