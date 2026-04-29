package com.melodix.app.ViewModel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Constants;
import com.melodix.app.Model.SongRequestUpload;
import com.melodix.app.Service.ArtistAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Service.StorageAPIService;
import com.melodix.app.Utils.MediaUtils;
import com.melodix.app.Utils.StringUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadSongViewModel extends AndroidViewModel {

    private final ArtistAPIService apiService;
    private final StorageAPIService storageService;

    // CÁC "LOA PHÁT THANH"
    private final MutableLiveData<String> uploadStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> uploadSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public UploadSongViewModel(@NonNull Application application) {
        super(application);
        apiService = RetrofitClient.getClient(application).create(ArtistAPIService.class);
        storageService = RetrofitClient.getStorage(application).create(StorageAPIService.class);
    }

    public LiveData<String> getUploadStatus() { return uploadStatus; }
    public LiveData<Boolean> getUploadSuccess() { return uploadSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    // =========================================================================
    // NHỊP 1: BẮT ĐẦU TẢI ẢNH BÌA
    // =========================================================================
    public void startUploadProcess(String songTitle, Uri coverUri, Uri audioUri, Uri lyricUri, String existingCoverUrl,
                                   boolean isEditMode, String editSongId, String selectedAlbumId,
                                   List<Integer> selectedGenreIds, List<String> selectedArtistIds) {

        uploadStatus.setValue(isEditMode ? "ĐANG CẬP NHẬT..." : "ĐANG TẢI LÊN (1/4)...");

        if (coverUri != null) {
            String coverFileName = "cover_" + System.currentTimeMillis() + ".jpg";
            uploadFileToSupabase(coverUri, Constants.SONG_COVER_BUCKET, coverFileName, "image/jpeg", new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String newCoverUrl = Constants.STORAGE_BASE_URL + Constants.SONG_COVER_BUCKET + coverFileName;
                        // Ảnh xong -> Gọi Nhạc
                        uploadAudioStep(newCoverUrl, songTitle, audioUri, lyricUri, isEditMode, editSongId, selectedAlbumId, selectedGenreIds, selectedArtistIds);
                    } else {
                        errorMessage.setValue("Lỗi tải ảnh bìa lên server!");
                    }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    errorMessage.setValue("Lỗi mạng khi tải ảnh bìa!");
                }
            });
        } else {
            // Không có ảnh -> Gọi Nhạc
            uploadAudioStep(existingCoverUrl, songTitle, audioUri, lyricUri, isEditMode, editSongId, selectedAlbumId, selectedGenreIds, selectedArtistIds);
        }
    }

    // =========================================================================
    // NHỊP 2: TẢI FILE NHẠC
    // =========================================================================
    private void uploadAudioStep(String finalCoverUrl, String songTitle, Uri audioUri, Uri lyricUri,
                                 boolean isEditMode, String editSongId, String selectedAlbumId,
                                 List<Integer> selectedGenreIds, List<String> selectedArtistIds) {
        if (audioUri != null) {
            uploadStatus.setValue("ĐANG TẢI NHẠC (2/4)...");
            String slugTitle = StringUtils.generateSlug(songTitle);
            String audioFileName = slugTitle + ".mp3";

            uploadFileToSupabase(audioUri, Constants.SONG_AUDIO_BUCKET, audioFileName, "audio/mpeg", new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String newAudioUrl = Constants.STORAGE_BASE_URL + Constants.SONG_AUDIO_BUCKET + audioFileName;
                        // Nhạc xong -> GỌI LYRIC (Thay vì gọi DB như code cũ)
                        uploadLyricStep(finalCoverUrl, songTitle, audioUri, newAudioUrl, lyricUri, isEditMode, editSongId, selectedAlbumId, selectedGenreIds, selectedArtistIds);
                    } else {
                        errorMessage.setValue("Lỗi tải file nhạc lên server!");
                    }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    errorMessage.setValue("Lỗi mạng khi tải file nhạc!");
                }
            });
        } else {
            // Không up nhạc -> GỌI LYRIC
            uploadLyricStep(finalCoverUrl, songTitle, audioUri, null, lyricUri, isEditMode, editSongId, selectedAlbumId, selectedGenreIds, selectedArtistIds);
        }
    }

    // =========================================================================
    // NHỊP 3: TẢI FILE LYRIC (.LRC)
    // =========================================================================
    private void uploadLyricStep(String finalCoverUrl, String songTitle, Uri audioUri, String finalAudioUrl, Uri lyricUri,
                                 boolean isEditMode, String editSongId, String selectedAlbumId,
                                 List<Integer> selectedGenreIds, List<String> selectedArtistIds) {

        if (lyricUri != null) {
            uploadStatus.setValue("ĐANG TẢI LỜI BÀI HÁT (3/4)...");
            String slugTitle = StringUtils.generateSlug(songTitle);
            String lyricFileName = slugTitle + ".lrc";

            uploadFileToSupabase(lyricUri, Constants.SONG_LYRIC_BUCKET, lyricFileName,"text/plain", new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String newLyricUrl = Constants.STORAGE_BASE_URL + Constants.SONG_LYRIC_BUCKET + lyricFileName;
                        // Lyric xong -> Lưu Database
                        submitToDatabase(songTitle, finalCoverUrl, finalAudioUrl, audioUri, newLyricUrl, isEditMode, editSongId, selectedAlbumId, selectedGenreIds, selectedArtistIds);
                    } else {
                        errorMessage.setValue("Lỗi tải file lời bài hát!");
                    }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    errorMessage.setValue("Lỗi mạng khi tải lời bài hát!");
                }
            });
        } else {
            // Không có Lyric -> Truyền null và Lưu Database
            submitToDatabase(songTitle, finalCoverUrl, finalAudioUrl, audioUri, null, isEditMode, editSongId, selectedAlbumId, selectedGenreIds, selectedArtistIds);
        }
    }

    // =========================================================================
    // NHỊP 4: LƯU DATABASE
    // =========================================================================
    private void submitToDatabase(String songTitle, String coverUrl, String audioUrl, Uri audioUri, String lyricUrl,
                                  boolean isEditMode, String editSongId, String selectedAlbumId,
                                  List<Integer> selectedGenreIds, List<String> selectedArtistIds) {

        uploadStatus.setValue(isEditMode ? "ĐANG LƯU DỮ LIỆU..." : "ĐANG LƯU DỮ LIỆU (4/4)...");

        if (isEditMode) {
            Map<String, Object> songData = new HashMap<>();
            songData.put("title", songTitle);
            if (coverUrl != null) songData.put("cover_url", coverUrl);
            if (audioUrl != null) {
                songData.put("audio_url", audioUrl);
                songData.put("duration_seconds", MediaUtils.getAudioDuration(getApplication(), audioUri));
            }
            if (selectedAlbumId != null) songData.put("album_id", selectedAlbumId);

            // Cập nhật DB cột lyrics_lrc_url
            if (lyricUrl != null) songData.put("lyrics_lrc_url", lyricUrl);

            apiService.updateSong("eq." + editSongId, songData).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) uploadSuccess.setValue(true);
                    else errorMessage.setValue("Cập nhật dữ liệu thất bại!");
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    errorMessage.setValue("Lỗi mạng khi lưu dữ liệu!");
                }
            });

        } else {
            int duration = audioUri != null ? MediaUtils.getAudioDuration(getApplication(), audioUri) : 0;
            // Nhét lyricUrl vào constructor
            SongRequestUpload requestBody = new SongRequestUpload(
                    songTitle, coverUrl, audioUrl, duration, selectedAlbumId, lyricUrl, selectedArtistIds, selectedGenreIds
            );

            apiService.submitSongWithArtists(requestBody).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) uploadSuccess.setValue(true);
                    else errorMessage.setValue("Lưu bài hát mới thất bại!");
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    errorMessage.setValue("Lỗi mạng khi lưu bài mới!");
                }
            });
        }
    }

    // =========================================================================
    // HÀM GỌI API SUPABASE STORAGE CHUNG
    // =========================================================================
    private void uploadFileToSupabase(Uri fileUri, String bucketName, String fileName, String mimeType, Callback<ResponseBody> callback) {
        RequestBody requestBody = new RequestBody() {
            @Override
            public MediaType contentType() { return MediaType.parse(mimeType); }
            @Override
            public void writeTo(okio.BufferedSink sink) throws java.io.IOException {
                try (InputStream is = getApplication().getContentResolver().openInputStream(fileUri)) {
                    if (is != null) sink.writeAll(okio.Okio.source(is));
                }
            }
        };
        storageService.uploadFileToStorage(mimeType, "true", bucketName.replace("/", ""), fileName, requestBody).enqueue(callback);
    }
}