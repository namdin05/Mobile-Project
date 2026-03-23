package com.melodix.app.ViewModel;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Model.PlayerUiState;
import com.melodix.app.Model.Song;
import com.melodix.app.Service.MelodixPlayerManager;

import java.util.List;

public class PlayerViewModel extends AndroidViewModel {

    public interface PlayerCommandCallback {
        void onSuccess();
        void onError(String message);
    }

    private final MelodixPlayerManager playerManager;

    private final MutableLiveData<PlayerUiState> playerState =
            new MutableLiveData<>(PlayerUiState.idle());

    private final MutableLiveData<String> playerMessage =
            new MutableLiveData<>(null);

    private final MelodixPlayerManager.PlayerStateListener stateListener =
            state -> playerState.postValue(state == null ? PlayerUiState.idle() : state);

    public PlayerViewModel(@NonNull Application application) {
        super(application);
        playerManager = MelodixPlayerManager.getInstance(application.getApplicationContext());
        playerManager.addListener(stateListener);
        connectPlayer();
    }

    public LiveData<PlayerUiState> getPlayerState() {
        return playerState;
    }

    public LiveData<String> getPlayerMessage() {
        return playerMessage;
    }

    public void clearPlayerMessage() {
        playerMessage.postValue(null);
    }

    public void connectPlayer() {
        playerManager.connect(new MelodixPlayerManager.ActionCallback() {
            @Override
            public void onSuccess() {
                playerState.postValue(playerManager.getCurrentState());
            }

            @Override
            public void onError(String message) {
                if (!TextUtils.isEmpty(message)) {
                    playerMessage.postValue(message);
                }
            }
        });
    }

    public void playSongs(@Nullable List<Song> songs, int startIndex, @Nullable PlayerCommandCallback callback) {
        playerManager.playSongs(songs, startIndex, new MelodixPlayerManager.ActionCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(String message) {
                if (!TextUtils.isEmpty(message)) {
                    playerMessage.postValue(message);
                }

                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }

    public void togglePlayPause() {
        playerManager.togglePlayPause(new MelodixPlayerManager.ActionCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                if (!TextUtils.isEmpty(message)) {
                    playerMessage.postValue(message);
                }
            }
        });
    }

    public void skipToNext() {
        playerManager.skipToNext(new MelodixPlayerManager.ActionCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                if (!TextUtils.isEmpty(message)) {
                    playerMessage.postValue(message);
                }
            }
        });
    }

    public void skipToPrevious() {
        playerManager.skipToPrevious(new MelodixPlayerManager.ActionCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                if (!TextUtils.isEmpty(message)) {
                    playerMessage.postValue(message);
                }
            }
        });
    }

    public void seekTo(long positionMs) {
        playerManager.seekTo(positionMs, new MelodixPlayerManager.ActionCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                if (!TextUtils.isEmpty(message)) {
                    playerMessage.postValue(message);
                }
            }
        });
    }

    public void seekForward() {
        playerManager.seekForward(new MelodixPlayerManager.ActionCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                if (!TextUtils.isEmpty(message)) {
                    playerMessage.postValue(message);
                }
            }
        });
    }

    public void seekBackward() {
        playerManager.seekBackward(new MelodixPlayerManager.ActionCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                if (!TextUtils.isEmpty(message)) {
                    playerMessage.postValue(message);
                }
            }
        });
    }

    public void setPlaybackSpeed(float speed) {
        playerManager.setPlaybackSpeed(speed, new MelodixPlayerManager.ActionCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                if (!TextUtils.isEmpty(message)) {
                    playerMessage.postValue(message);
                }
            }
        });
    }

    public void stopPlayback() {
        playerManager.stopAndClear(new MelodixPlayerManager.ActionCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                if (!TextUtils.isEmpty(message)) {
                    playerMessage.postValue(message);
                }
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        playerManager.removeListener(stateListener);
    }
}