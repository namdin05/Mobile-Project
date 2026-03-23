package com.melodix.app.Repository;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.melodix.app.Model.Album;
import com.melodix.app.Model.Genre;
import com.melodix.app.Model.HomeBanner;
import com.melodix.app.Model.HomeDashboard;
import com.melodix.app.Model.Song;
import com.melodix.app.Utils.AppConstants;

import java.util.ArrayList;
import java.util.List;

public class HomeRepository {

    private static final String TAG = "HomeRepository";

    private final CollectionReference bannersCollection;
    private final CollectionReference songsCollection;
    private final CollectionReference albumsCollection;
    private final CollectionReference genresCollection;

    public HomeRepository() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        bannersCollection = firestore.collection(AppConstants.HOME_BANNERS_COLLECTION);
        songsCollection = firestore.collection(AppConstants.SONGS_COLLECTION);
        albumsCollection = firestore.collection(AppConstants.ALBUMS_COLLECTION);
        genresCollection = firestore.collection(AppConstants.GENRES_COLLECTION);
    }

    public void loadHomeDashboard(@NonNull RepositoryCallback<HomeDashboard> callback) {
        HomeDashboard dashboard = new HomeDashboard();
        HomeLoadState loadState = new HomeLoadState();

        loadBanners(new RepositoryCallback<List<HomeBanner>>() {
            @Override
            public void onSuccess(List<HomeBanner> data) {
                dashboard.setBanners(data);
                loadTrendingSongsAndContinue(dashboard, loadState, callback);
            }

            @Override
            public void onError(String message) {
                loadState.lastError = message;
                dashboard.setBanners(new ArrayList<>());
                loadTrendingSongsAndContinue(dashboard, loadState, callback);
            }
        });
    }

    private void loadTrendingSongsAndContinue(HomeDashboard dashboard,
                                              HomeLoadState loadState,
                                              RepositoryCallback<HomeDashboard> callback) {
        loadTrendingSongs(new RepositoryCallback<List<Song>>() {
            @Override
            public void onSuccess(List<Song> data) {
                dashboard.setTrendingSongs(data);
                loadGenresAndContinue(dashboard, loadState, callback);
            }

            @Override
            public void onError(String message) {
                loadState.lastError = message;
                dashboard.setTrendingSongs(new ArrayList<>());
                loadGenresAndContinue(dashboard, loadState, callback);
            }
        });
    }

    private void loadGenresAndContinue(HomeDashboard dashboard,
                                       HomeLoadState loadState,
                                       RepositoryCallback<HomeDashboard> callback) {
        loadGenres(new RepositoryCallback<List<Genre>>() {
            @Override
            public void onSuccess(List<Genre> data) {
                dashboard.setGenres(data);
                loadNewAlbumsAndFinish(dashboard, loadState, callback);
            }

            @Override
            public void onError(String message) {
                loadState.lastError = message;
                dashboard.setGenres(new ArrayList<>());
                loadNewAlbumsAndFinish(dashboard, loadState, callback);
            }
        });
    }

    private void loadNewAlbumsAndFinish(HomeDashboard dashboard,
                                        HomeLoadState loadState,
                                        RepositoryCallback<HomeDashboard> callback) {
        loadNewAlbums(new RepositoryCallback<List<Album>>() {
            @Override
            public void onSuccess(List<Album> data) {
                dashboard.setNewAlbums(data);
                finishLoad(dashboard, loadState, callback);
            }

            @Override
            public void onError(String message) {
                loadState.lastError = message;
                dashboard.setNewAlbums(new ArrayList<>());
                finishLoad(dashboard, loadState, callback);
            }
        });
    }

    private void finishLoad(HomeDashboard dashboard,
                            HomeLoadState loadState,
                            RepositoryCallback<HomeDashboard> callback) {
        try {
            if (dashboard == null) {
                callback.onSuccess(new HomeDashboard());
                return;
            }

            if (dashboard.isEmpty() && !TextUtils.isEmpty(loadState.lastError)) {
                callback.onError(loadState.lastError);
                return;
            }

            callback.onSuccess(dashboard);
        } catch (Exception e) {
            Log.e(TAG, "finishLoad failed.", e);
            callback.onError("Không thể tổng hợp dữ liệu trang chủ.");
        }
    }

    private void loadBanners(@NonNull RepositoryCallback<List<HomeBanner>> callback) {
        try {
            bannersCollection
                    .orderBy("order", Query.Direction.ASCENDING)
                    .limit(AppConstants.HOME_QUERY_BUFFER_LIMIT)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<HomeBanner> list = new ArrayList<>();

                        try {
                            if (querySnapshot != null) {
                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                    HomeBanner banner = HomeBanner.fromDocument(document);
                                    if (banner.isActive()) {
                                        list.add(banner);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Banner parse error.", e);
                        }

                        callback.onSuccess(takeFirstItems(list, AppConstants.HOME_BANNER_LIMIT));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "loadBanners failed.", e);
                        callback.onError(mapFirestoreError(e, "Không thể tải banner trang chủ."));
                    });
        } catch (Exception e) {
            Log.e(TAG, "loadBanners exception.", e);
            callback.onError("Không thể tải banner trang chủ.");
        }
    }

    private void loadTrendingSongs(@NonNull RepositoryCallback<List<Song>> callback) {
        try {
            songsCollection
                    .orderBy("trendingScore", Query.Direction.DESCENDING)
                    .limit(AppConstants.HOME_QUERY_BUFFER_LIMIT)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<Song> list = new ArrayList<>();

                        try {
                            if (querySnapshot != null) {
                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                    Song song = Song.fromDocument(document);
                                    if (song.isActive()) {
                                        list.add(song);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Trending songs parse error.", e);
                        }

                        callback.onSuccess(takeFirstItems(list, AppConstants.HOME_TRENDING_LIMIT));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "loadTrendingSongs failed.", e);
                        callback.onError(mapFirestoreError(e, "Không thể tải danh sách Top Trending."));
                    });
        } catch (Exception e) {
            Log.e(TAG, "loadTrendingSongs exception.", e);
            callback.onError("Không thể tải danh sách Top Trending.");
        }
    }

    private void loadGenres(@NonNull RepositoryCallback<List<Genre>> callback) {
        try {
            genresCollection
                    .orderBy("name", Query.Direction.ASCENDING)
                    .limit(AppConstants.HOME_QUERY_BUFFER_LIMIT)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<Genre> list = new ArrayList<>();

                        try {
                            if (querySnapshot != null) {
                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                    Genre genre = Genre.fromDocument(document);
                                    if (genre.isActive()) {
                                        list.add(genre);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Genres parse error.", e);
                        }

                        callback.onSuccess(takeFirstItems(list, AppConstants.HOME_GENRE_LIMIT));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "loadGenres failed.", e);
                        callback.onError(mapFirestoreError(e, "Không thể tải danh sách thể loại."));
                    });
        } catch (Exception e) {
            Log.e(TAG, "loadGenres exception.", e);
            callback.onError("Không thể tải danh sách thể loại.");
        }
    }

    private void loadNewAlbums(@NonNull RepositoryCallback<List<Album>> callback) {
        try {
            albumsCollection
                    .orderBy("releaseTimestamp", Query.Direction.DESCENDING)
                    .limit(AppConstants.HOME_QUERY_BUFFER_LIMIT)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<Album> list = new ArrayList<>();

                        try {
                            if (querySnapshot != null) {
                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                    Album album = Album.fromDocument(document);
                                    if (album.isActive()) {
                                        list.add(album);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Albums parse error.", e);
                        }

                        callback.onSuccess(takeFirstItems(list, AppConstants.HOME_ALBUM_LIMIT));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "loadNewAlbums failed.", e);
                        callback.onError(mapFirestoreError(e, "Không thể tải danh sách album mới."));
                    });
        } catch (Exception e) {
            Log.e(TAG, "loadNewAlbums exception.", e);
            callback.onError("Không thể tải danh sách album mới.");
        }
    }

    private <T> List<T> takeFirstItems(List<T> source, int limit) {
        List<T> result = new ArrayList<>();

        try {
            if (source == null || source.isEmpty() || limit <= 0) {
                return result;
            }

            for (int i = 0; i < source.size() && result.size() < limit; i++) {
                result.add(source.get(i));
            }
        } catch (Exception e) {
            Log.e(TAG, "takeFirstItems failed.", e);
        }

        return result;
    }

    private String mapFirestoreError(Exception exception, String fallbackMessage) {
        try {
            if (exception == null) {
                return fallbackMessage;
            }

            String message = exception.getMessage();
            if (!TextUtils.isEmpty(message)) {
                String lower = message.toLowerCase();

                if (lower.contains("permission")) {
                    return "Firestore chưa cấp quyền đọc dữ liệu trang chủ.";
                }

                if (lower.contains("network")) {
                    return "Không thể tải dữ liệu trang chủ vì lỗi mạng.";
                }

                return message;
            }
        } catch (Exception ignored) {
        }

        return fallbackMessage;
    }

    private static class HomeLoadState {
        String lastError;
    }
}