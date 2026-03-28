package com.melodix.app.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.melodix.app.Model.AddToPlaylistRequest;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.Model.PlaylistCreateRequest;
import com.melodix.app.Model.PlaylistUpdateRequest;
import com.melodix.app.Repository.PlaylistRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistViewModel extends ViewModel {

    private final PlaylistRepository repository;

    private final MutableLiveData<List<Playlist>> myPlaylists = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final MutableLiveData<List<PlaylistSong>> playlistSongs = new MutableLiveData<>();

    public PlaylistViewModel() {
        repository = new PlaylistRepository();
    }

    // ==================== PLAYLIST ====================

    public LiveData<List<Playlist>> getMyPlaylists() {
        return myPlaylists;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gọi API lấy tất cả playlist của user
     */
    public void loadMyPlaylists(String userId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        repository.getMyPlaylists(userId, new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    myPlaylists.setValue(response.body());
                } else {
                    errorMessage.setValue("Không thể tải danh sách playlist");
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    // ==================== TẠO PLAYLIST ====================

    public void createPlaylist(PlaylistCreateRequest request, final OnOperationCompleteListener listener) {
        repository.createPlaylist(request, new Callback<Playlist>() {
            @Override
            public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onSuccess("Tạo playlist thành công");
                } else {
                    listener.onError("Không thể tạo playlist");
                }
            }

            @Override
            public void onFailure(Call<Playlist> call, Throwable t) {
                listener.onError("Lỗi kết nối khi tạo playlist");
            }
        });
    }

    // ==================== QUẢN LÝ BÀI HÁT TRONG PLAYLIST ====================

    public LiveData<List<PlaylistSong>> getPlaylistSongs() {
        return playlistSongs;
    }

    public void loadPlaylistSongs(String playlistId) {
        isLoading.setValue(true);

        repository.getPlaylistSongs(playlistId, new Callback<List<PlaylistSong>>() {
            @Override
            public void onResponse(Call<List<PlaylistSong>> call, Response<List<PlaylistSong>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    playlistSongs.setValue(response.body());
                } else {
                    errorMessage.setValue("Không thể tải bài hát trong playlist");
                }
            }

            @Override
            public void onFailure(Call<List<PlaylistSong>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Lỗi kết nối");
            }
        });
    }

    public void addSongToPlaylist(String playlistId, String songId, int orderIndex, final OnOperationCompleteListener listener) {
        AddToPlaylistRequest request = new AddToPlaylistRequest(playlistId, songId, orderIndex);
        repository.addSongToPlaylist(request, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    listener.onSuccess("Đã thêm bài hát vào playlist");
                } else {
                    listener.onError("Không thể thêm bài hát");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError("Lỗi kết nối");
            }
        });
    }

    public void removeSongFromPlaylist(String playlistId, String songId, final OnOperationCompleteListener listener) {
        repository.removeSongFromPlaylist(playlistId, songId, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    listener.onSuccess("Đã xóa bài hát khỏi playlist");
                } else {
                    listener.onError("Không thể xóa bài hát");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError("Lỗi kết nối");
            }
        });
    }

    public interface OnOperationCompleteListener {
        void onSuccess(String message);
        void onError(String error);
    }
}