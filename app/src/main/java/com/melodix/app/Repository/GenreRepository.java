package com.melodix.app.Repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.melodix.app.Constants;
import com.melodix.app.Model.Genre;
import com.melodix.app.Service.GenreAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Service.StorageAPIService; // Nhớ import Service Storage của bạn

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GenreRepository {
    private GenreAPIService genreAPIService;
    private StorageAPIService storageService;

    public GenreRepository(Context context) {
        // Dùng AdminClient cho quyền ghi/xóa
        genreAPIService = RetrofitClient.getClient(context).create(GenreAPIService.class);
        storageService = RetrofitClient.getStorage(context).create(StorageAPIService.class);
    }

    public MutableLiveData<List<Genre>> fetchGenres() {
        MutableLiveData<List<Genre>> genres = new MutableLiveData<>();

        genreAPIService.getGenres().enqueue(new Callback<List<Genre>>() {
            @Override
            public void onResponse(Call<List<Genre>> call, Response<List<Genre>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    genres.setValue(response.body());
                } else {
                    genres.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Genre>> call, Throwable t) {
                Log.e("GENRE_API", "Lỗi mạng: " + t.getMessage());
                genres.setValue(null);
            }
        });
        return genres;
    }

    // Gộp quá trình xử lý ảnh và DB vào cùng 1 hàm, trả kết quả về qua LiveData
    public void saveGenreToDb(String genreId, String name, String oldImageUrl, byte[] newImageBytes,
                              MutableLiveData<Boolean> isSuccessLiveData, MutableLiveData<String> messageLiveData) {
        if (newImageBytes != null) {
            // Có ảnh mới -> Upload ảnh trước
            String fileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + ".jpg";
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), newImageBytes);

            storageService.uploadFileToStorage(
                    "image/jpeg", "true",
                    Constants.GENRE_COVER_BUCKET.replace("/", ""),
                    fileName, requestBody
            ).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String newImageUrl = Constants.STORAGE_BASE_URL + Constants.GENRE_COVER_BUCKET + fileName;
                        // Ảnh lên thành công -> Lưu vào Database
                        saveToDatabase(genreId, name, newImageUrl, isSuccessLiveData, messageLiveData);
                    } else {
                        messageLiveData.postValue("Lỗi Upload ảnh lên Storage");
                        isSuccessLiveData.postValue(false);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    messageLiveData.postValue("Lỗi mạng khi Upload ảnh");
                    isSuccessLiveData.postValue(false);
                }
            });
        } else {
            // Không có ảnh mới -> Lưu DB luôn
            saveToDatabase(genreId, name, oldImageUrl, isSuccessLiveData, messageLiveData);
        }
    }

    // Tách riêng hàm lưu DB cho gọn (Đã bỏ apiKey, token vì Interceptor đã lo)
    private void saveToDatabase(String genreId, String name, String imageUrl,
                                MutableLiveData<Boolean> isSuccessLiveData, MutableLiveData<String> messageLiveData) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("cover_url", imageUrl);
        data.put("is_visible", true);

        // ĐÃ SỬA: Đổi Void thành ResponseBody cho khớp với Interface
        Callback<ResponseBody> dbCallback = new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    messageLiveData.postValue("Lưu thể loại thành công!");
                    isSuccessLiveData.postValue(true);
                } else {
                    messageLiveData.postValue("Lỗi lưu dữ liệu: " + response.code());
                    isSuccessLiveData.postValue(false);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                messageLiveData.postValue("Lỗi mạng: " + t.getMessage());
                isSuccessLiveData.postValue(false);
            }
        };

        if (genreId != null) {
            genreAPIService.updateGenre("eq." + genreId, data).enqueue(dbCallback); // Update
        } else {
            genreAPIService.createGenre(data).enqueue(dbCallback); // Create
        }
    }


    public void softDeleteGenre(String genreId, MutableLiveData<Boolean> isSuccessLiveData, MutableLiveData<String> messageLiveData) {
        Map<String, Object> data = new HashMap<>();
        data.put("is_visible", false);

        // ĐÃ SỬA: Đổi Void thành ResponseBody
        genreAPIService.updateGenre("eq." + genreId, data).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    messageLiveData.postValue("Đã xóa thể loại!");
                    isSuccessLiveData.postValue(true);
                } else {
                    messageLiveData.postValue("Lỗi khi xóa!");
                    isSuccessLiveData.postValue(false);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                messageLiveData.postValue("Lỗi mạng khi xóa!");
                isSuccessLiveData.postValue(false);
            }
        });
    }

    public void restoreGenre(String genreId, MutableLiveData<Boolean> isSuccessLiveData, MutableLiveData<String> messageLiveData) {
        Map<String, Object> data = new HashMap<>();
        data.put("is_visible", true);

        // ĐÃ SỬA: Đổi Void thành ResponseBody
        genreAPIService.updateGenre("eq." + genreId, data).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    messageLiveData.postValue("Đã khôi phục thể loại!");
                    isSuccessLiveData.postValue(true);
                } else {
                    messageLiveData.postValue("Lỗi khi khôi phục!");
                    isSuccessLiveData.postValue(false);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                messageLiveData.postValue("Lỗi mạng!");
                isSuccessLiveData.postValue(false);
            }
        });
    }
}