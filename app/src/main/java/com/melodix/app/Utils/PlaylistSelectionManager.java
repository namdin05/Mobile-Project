package com.melodix.app.Utils;

import android.content.Context;
import android.widget.Toast;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.PlaylistRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistSelectionManager {

    private final Context context;
    private final PlaylistRepository playlistRepository;
    private final Set<String> playlistIdsContainSong = new HashSet<>();

    public PlaylistSelectionManager(Context context) {
        this.context = context;
        this.playlistRepository = new PlaylistRepository(context);
    }

    public interface OnPlaylistsLoadedListener {
        void onPlaylistsLoaded(List<Playlist> playlists, Set<String> selectedIds);
        void onError(String error);
    }

    public interface OnSelectionChangedListener {
        void onSuccess(String message);
        void onError(String error);
    }

    public void loadUserPlaylistsWithSongStatus(String userId, String songId,
                                                OnPlaylistsLoadedListener listener) {
        playlistRepository.getUserPlaylists(userId, new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Playlist> playlists = response.body();
                    checkSongInPlaylists(playlists, songId, listener);
                } else {
                    listener.onError("Không thể tải danh sách playlist");
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                listener.onError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    private void checkSongInPlaylists(List<Playlist> playlists, String songId,
                                      OnPlaylistsLoadedListener listener) {
        playlistIdsContainSong.clear();

        if (playlists.isEmpty()) {
            listener.onPlaylistsLoaded(playlists, playlistIdsContainSong);
            return;
        }

        final int[] pendingChecks = {playlists.size()};

        for (Playlist playlist : playlists) {
            playlistRepository.getPlaylistSongs(playlist.id, new Callback<List<PlaylistSong>>() {
                @Override
                public void onResponse(Call<List<PlaylistSong>> call,
                                       Response<List<PlaylistSong>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (PlaylistSong ps : response.body()) {
                            if (ps.song != null && ps.song.getId().equals(songId)) {
                                playlistIdsContainSong.add(playlist.id);
                                break;
                            }
                        }
                    }
                    pendingChecks[0]--;
                    if (pendingChecks[0] == 0) {
                        listener.onPlaylistsLoaded(playlists, playlistIdsContainSong);
                    }
                }

                @Override
                public void onFailure(Call<List<PlaylistSong>> call, Throwable t) {
                    pendingChecks[0]--;
                    if (pendingChecks[0] == 0) {
                        listener.onPlaylistsLoaded(playlists, playlistIdsContainSong);
                    }
                }
            });
        }
    }

    public void addSongToPlaylist(String songId, String playlistId,
                                  OnSelectionChangedListener listener) {
        playlistRepository.addSongToPlaylist(playlistId, songId, 0,
                new Callback<okhttp3.ResponseBody>() {
                    @Override
                    public void onResponse(Call<okhttp3.ResponseBody> call,
                                           Response<okhttp3.ResponseBody> response) {
                        if (response.isSuccessful()) {
                            playlistIdsContainSong.add(playlistId);
                            listener.onSuccess("Đã thêm vào playlist");
                        } else {
                            listener.onError("Thêm thất bại");
                        }
                    }

                    @Override
                    public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                        listener.onError("Lỗi: " + t.getMessage());
                    }
                });
    }

    public void removeSongFromPlaylist(String songId, String playlistId,
                                       OnSelectionChangedListener listener) {
        playlistRepository.removeSongFromPlaylist(playlistId, songId,
                new Callback<okhttp3.ResponseBody>() {
                    @Override
                    public void onResponse(Call<okhttp3.ResponseBody> call,
                                           Response<okhttp3.ResponseBody> response) {
                        if (response.isSuccessful()) {
                            playlistIdsContainSong.remove(playlistId);
                            listener.onSuccess("Đã xóa khỏi playlist");
                        } else {
                            listener.onError("Xóa thất bại");
                        }
                    }

                    @Override
                    public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                        listener.onError("Lỗi: " + t.getMessage());
                    }
                });
    }
}