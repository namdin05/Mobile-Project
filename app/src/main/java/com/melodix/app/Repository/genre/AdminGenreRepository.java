package com.melodix.app.Repository.genre;

import androidx.lifecycle.MutableLiveData;

import com.melodix.app.BuildConfig;
import com.melodix.app.Constants;
import com.melodix.app.Model.Genre;
import com.melodix.app.Service.AdminAPIService;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminGenreRepository {
    private final AdminAPIService apiService;
    private final ProfileAPIService storageService; // Dùng chung service upload ảnh
    private final String apiKey = BuildConfig.SERVICE_KEY;
    private final String token = "Bearer " + BuildConfig.SERVICE_KEY;

    public AdminGenreRepository() {
        apiService = RetrofitClient.getClient().create(AdminAPIService.class);
        storageService = RetrofitClient.getClient().create(ProfileAPIService.class);
    }

    // 1. Kéo danh sách
    public void fetchGenres(MutableLiveData<List<Genre>> genresLiveData, MutableLiveData<String> messageLiveData) {
        apiService.getAllGenres(apiKey, token).enqueue(new Callback<List<Genre>>() {
            @Override
            public void onResponse(Call<List<Genre>> call, Response<List<Genre>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Genre> activeGenres = new ArrayList<>();
                    for (Genre genre : response.body()) {
                        if (genre.isVisible()) activeGenres.add(genre);
                    }
                    genresLiveData.postValue(activeGenres);
                } else {
                    messageLiveData.postValue("Lỗi tải dữ liệu từ Database");
                }
            }

            @Override
            public void onFailure(Call<List<Genre>> call, Throwable t) {
                messageLiveData.postValue("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    // 2. Upload Ảnh lên Storage
    public void uploadImage(byte[] imageBytes, String fileName, Callback<ResponseBody> callback) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);
        storageService.uploadFileToStorage(
                apiKey, token, "image/jpeg", "true",
                Constants.GENRE_COVER_BUCKET.replace("/", ""),
                fileName, requestBody
        ).enqueue(callback);
    }

    // 3. Lưu dữ liệu vào Database (Create hoặc Update)
    public void saveGenreToDb(String genreId, String name, String imageUrl, Callback<ResponseBody> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("image_url", imageUrl);
        data.put("is_visible", true);

        if (genreId != null) {
            // Update
            apiService.updateGenre(apiKey, token, "eq." + genreId, data).enqueue(callback);
        } else {
            // Create
            apiService.createGenre(apiKey, token, data).enqueue(callback);
        }
    }

    // 4. Xóa mềm (Soft Delete)
    public void softDeleteGenre(String genreId, Callback<ResponseBody> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("is_visible", false);
        apiService.updateGenre(apiKey, token, "eq." + genreId, data).enqueue(callback);
    }
}