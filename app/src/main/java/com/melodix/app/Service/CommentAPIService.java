package com.melodix.app.Service;

import com.melodix.app.Model.Comment;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface CommentAPIService {

    // Lấy danh sách comment + thông tin user (join profiles)
    @GET("comments?select=*,profiles!comments_user_id_fkey(display_name,username,avatar_url)")
    Call<List<Comment>> getCommentsBySong(
            @Query("song_id") String songIdFilter,
            @Query("order") String order
    );

    // Đăng comment mới
    @POST("comments")
    Call<ResponseBody> postComment(
            //@Header("Prefer") String prefer,
            @Body Map<String, Object> data
    );
}