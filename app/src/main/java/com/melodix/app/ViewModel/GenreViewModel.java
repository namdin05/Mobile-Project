package com.melodix.app.ViewModel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Model.Genre;
import com.melodix.app.Repository.GenreRepository;

import java.util.List;

public class GenreViewModel extends AndroidViewModel {
    private GenreRepository genreRepository;

    private LiveData<List<Genre>> genres;

    // Thêm 2 "Cái loa" báo trạng thái về cho Fragment
    private final MutableLiveData<Boolean> actionSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> actionMessage = new MutableLiveData<>();

    public GenreViewModel(@NonNull Application application) {
        super(application);
        genreRepository = new GenreRepository(application);
    }

    public LiveData<List<Genre>> getAllGenres() {
        if (genres == null) {
            genres = genreRepository.fetchGenres();
        }
        return genres;
    }

    // Hàm gọi ép tải lại list khi thêm/sửa/xóa xong
    public void refreshGenres() {
        genres = genreRepository.fetchGenres();
    }

    // Các getter cho LiveData trạng thái
    public LiveData<Boolean> getActionSuccess() { return actionSuccess; }
    public LiveData<String> getActionMessage() { return actionMessage; }

    // Gọi chức năng Lưu
    public void saveGenre(String genreId, String name, String imageUrl, byte[] imageBytes) {
        genreRepository.saveGenreToDb(genreId, name, imageUrl, imageBytes, actionSuccess, actionMessage);
    }

    // Gọi chức năng Xóa
    public void deleteGenre(String genreId) {
        genreRepository.softDeleteGenre(genreId, actionSuccess, actionMessage);
    }

    public void restoreGenre(String genreId) {
        genreRepository.restoreGenre(genreId, actionSuccess, actionMessage);
    }
}