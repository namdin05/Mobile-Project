package com.melodix.app.ViewModel.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.melodix.app.Constants;
import com.melodix.app.Model.Genre;
import com.melodix.app.Repository.genre.AdminGenreRepository;

import java.util.List;
import java.util.UUID;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminGenreViewModel extends ViewModel {

    private final AdminGenreRepository repository;
    private final MutableLiveData<List<Genre>> genresLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> messageLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSuccessLiveData = new MutableLiveData<>();

    public AdminGenreViewModel() {
        repository = new AdminGenreRepository();
    }

    public LiveData<List<Genre>> getGenresLiveData() { return genresLiveData; }
    public LiveData<String> getMessageLiveData() { return messageLiveData; }
    public LiveData<Boolean> getIsSuccessLiveData() { return isSuccessLiveData; }

    public void loadGenres() {
        repository.fetchGenres(genresLiveData, messageLiveData);
    }

    // Xử lý logic Lưu (Bao gồm cả việc quyết định có up ảnh hay không)
    public void saveGenre(String genreId, String name, String oldImageUrl, byte[] newImageBytes) {
        if (newImageBytes != null) {
            // Trường hợp 1: Có ảnh mới -> Upload ảnh trước
            String fileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + ".jpg";
            repository.uploadImage(newImageBytes, fileName, new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String newImageUrl = Constants.STORAGE_BASE_URL + Constants.GENRE_COVER_BUCKET + fileName;
                        // Ảnh lên thành công -> Lưu DB
                        saveToDatabase(genreId, name, newImageUrl);
                    } else {
                        messageLiveData.postValue("Lỗi Upload ảnh lên Storage");
                        isSuccessLiveData.postValue(false);
                    }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    messageLiveData.postValue("Lỗi mạng khi Upload");
                    isSuccessLiveData.postValue(false);
                }
            });
        } else {
            // Trường hợp 2: Không có ảnh mới -> Lưu DB luôn
            saveToDatabase(genreId, name, oldImageUrl);
        }
    }

    private void saveToDatabase(String genreId, String name, String imageUrl) {
        repository.saveGenreToDb(genreId, name, imageUrl, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    messageLiveData.postValue("Lưu thành công!");
                    isSuccessLiveData.postValue(true);
                    loadGenres(); // Load lại danh sách mới
                } else {
                    messageLiveData.postValue("Lỗi Database");
                    isSuccessLiveData.postValue(false);
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                messageLiveData.postValue("Lỗi mạng Database");
                isSuccessLiveData.postValue(false);
            }
        });
    }

    public void deleteGenre(String genreId) {
        repository.softDeleteGenre(genreId, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    messageLiveData.postValue("Đã ẩn Thể loại!");
                    loadGenres();
                } else {
                    messageLiveData.postValue("Lỗi Database khi xóa");
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                messageLiveData.postValue("Lỗi mạng khi xóa");
            }
        });
    }
}