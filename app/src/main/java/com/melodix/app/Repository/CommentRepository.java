package com.melodix.app.Repository;

import android.content.Context;
import android.util.Log;

import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Comment;
import com.melodix.app.Service.CommentAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

public class CommentRepository {

    private final CommentAPIService apiService;
    private final String apiKey;

    public CommentRepository(Context context) {
        this.apiService = RetrofitClient.getClient().create(CommentAPIService.class);
        this.apiKey = BuildConfig.API_KEY;
    }

    // Lấy danh sách bình luận của 1 bài hát
    public void getCommentsBySong(String songId, Callback<List<Comment>> callback) {
        if (songId == null || songId.isEmpty()) {
            callback.onFailure(null, new Throwable("Song ID is null"));
            return;
        }

        String filter = "eq." + songId;

        Log.d("COMMENT_REPO", "Getting comments for song: " + songId);
        apiService.getCommentsBySong(
                apiKey,
                filter,
                "created_at.desc"
        ).enqueue(callback);
    }

    // Đăng bình luận mới
    public void postComment(String songId, String userId, String content, Callback<ResponseBody> callback) {
        if (songId == null || userId == null || content == null || content.trim().isEmpty()) {
            if (callback != null) {
                callback.onFailure(null, new Throwable("Invalid parameters"));
            }
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("song_id", songId);
        data.put("user_id", userId);
        data.put("content", content.trim());

        Log.d("COMMENT_REPO", "Posting data: " + data.toString());

        apiService.postComment(
                apiKey,
                "return=representation",
                data
        ).enqueue(callback);
    }
}