package com.melodix.app.Repository;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.melodix.app.Model.SongLyrics;
import com.melodix.app.Utils.AppConstants;

public class LyricsRepository {

    private static final String TAG = "LyricsRepository";

    private final CollectionReference lyricsCollection;

    public LyricsRepository() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        lyricsCollection = firestore.collection(AppConstants.LYRICS_COLLECTION);
    }

    public void loadLyrics(@NonNull String songId, @NonNull RepositoryCallback<SongLyrics> callback) {
        try {
            if (TextUtils.isEmpty(songId)) {
                callback.onSuccess(new SongLyrics());
                return;
            }

            String safeSongId = songId.trim();

            lyricsCollection.document(safeSongId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        try {
                            if (documentSnapshot != null && documentSnapshot.exists()) {
                                SongLyrics lyrics = SongLyrics.fromDocument(documentSnapshot);
                                if (lyrics.isActive()) {
                                    callback.onSuccess(lyrics);
                                } else {
                                    callback.onSuccess(new SongLyrics());
                                }
                            } else {
                                fallbackQueryBySongId(safeSongId, callback);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Primary lyrics document parse failed.", e);
                            fallbackQueryBySongId(safeSongId, callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Primary lyrics document load failed.", e);
                        fallbackQueryBySongId(safeSongId, callback);
                    });
        } catch (Exception e) {
            Log.e(TAG, "loadLyrics exception.", e);
            callback.onError("Không thể tải lời bài hát.");
        }
    }

    private void fallbackQueryBySongId(String songId, RepositoryCallback<SongLyrics> callback) {
        try {
            lyricsCollection.whereEqualTo("songId", songId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        try {
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                SongLyrics lyrics = SongLyrics.fromDocument(querySnapshot.getDocuments().get(0));
                                if (lyrics.isActive()) {
                                    callback.onSuccess(lyrics);
                                } else {
                                    callback.onSuccess(new SongLyrics());
                                }
                            } else {
                                callback.onSuccess(new SongLyrics());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "fallbackQueryBySongId parse failed.", e);
                            callback.onSuccess(new SongLyrics());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "fallbackQueryBySongId failed.", e);
                        callback.onError(mapFirestoreError(e));
                    });
        } catch (Exception e) {
            Log.e(TAG, "fallbackQueryBySongId exception.", e);
            callback.onError("Không thể tải lời bài hát.");
        }
    }

    private String mapFirestoreError(Exception exception) {
        try {
            if (exception == null) {
                return "Không thể tải lời bài hát.";
            }

            String message = exception.getMessage();
            if (!TextUtils.isEmpty(message)) {
                String lower = message.toLowerCase();

                if (lower.contains("permission")) {
                    return "Firestore chưa cấp quyền đọc collection lyrics.";
                }

                if (lower.contains("network")) {
                    return "Lỗi mạng khi tải lyrics.";
                }

                return message;
            }
        } catch (Exception ignored) {
        }

        return "Không thể tải lời bài hát.";
    }
}