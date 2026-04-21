package com.melodix.app.Repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Model.Comment;
import com.melodix.app.Utils.SessionManager; // IMPORT SESSION MỚI
import com.melodix.app.Service.CommentAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentRepository {
    private CommentAPIService apiService;
    private Context context; // Giữ lại context để gọi SessionManager

    public CommentRepository(Context context) {
        this.context = context;
        apiService = RetrofitClient.getClient(context).create(CommentAPIService.class);
    }

    public void getCommentsBySong(String songId, MutableLiveData<List<Comment>> commentsLiveData, MutableLiveData<String> message) {
        apiService.getCommentsBySong("eq." + songId, "created_at.desc").enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentsLiveData.postValue(response.body());
                } else {
                    message.postValue("Không tải được bình luận: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {
                message.postValue("Lỗi mạng khi tải bình luận: " + t.getMessage());
            }
        });
    }

    public void postComment(String songId, String content, MutableLiveData<Boolean> isSuccess, MutableLiveData<String> message) {

        // =======================================================
        // 1. TỰ ĐỘNG LẤY ID TỪ SESSION MANAGER (Cực kỳ nhàn nhã)
        // =======================================================
        String currentUserId = SessionManager.getInstance(context).getUserId();

        // 2. Chốt chặn an toàn: Tránh lỗi UUID rỗng đâm sập Server
        if (currentUserId == null || currentUserId.isEmpty()) {
            message.postValue("Phiên đăng nhập đã hết hạn. Vui lòng đăng xuất và đăng nhập lại!");
            isSuccess.postValue(false);
            return;
        }

        Log.e("COMMENT_DEBUG", "Posting - SongId: " + songId + ", UserId: " + currentUserId + ", Content: " + content);

        // 3. Đóng gói dữ liệu gửi lên
        Map<String, Object> body = new HashMap<>();
        body.put("song_id", songId);
        body.put("user_id", currentUserId);
        body.put("content", content);

        // 4. Bắn API
        apiService.postComment(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    message.postValue("Bình luận đã được đăng!");
                    isSuccess.postValue(true);
                } else {
                    message.postValue("Đăng bình luận thất bại: " + response.code());
                    isSuccess.postValue(false);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                message.postValue("Lỗi kết nối: " + t.getMessage());
                isSuccess.postValue(false);
            }
        });
    }
}