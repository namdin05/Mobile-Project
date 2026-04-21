package com.melodix.app.Service;

import com.melodix.app.Model.Profile;

import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProfileAPIService  {

    @GET("profiles")
    Call<List<Profile>> getAllProfiles();

    // Lấy thông tin của 1 user cụ thể dựa vào ID
    @retrofit2.http.Headers("Cache-Control: no-cache")
    @GET("profiles?select=display_name,avatar_url, role")
    Call<List<Profile>> getProfileById(
            @Query("id") String idFilter // Truyền vào dạng "eq.uuid-của-admin"
    );

    // 2. Hàm Cập nhật (PATCH) thông tin profile
    @PATCH("profiles")
    Call<ResponseBody> updateProfile(
            @Query("id") String idFilter, // eq.[UID]
            @Body java.util.Map<String, Object> bodyData // Gửi HashMap cho tiện
    );

    @PATCH("profiles")
    Call<Void> updateFcmToken(
            @Query(value = "id", encoded = true) String idFilter, // THÊM encoded = true VÀO ĐÂY
            @Body Map<String, Object> bodyData
    );
    @retrofit2.http.HEAD("follows")
    Call<Void> getFollowerCount(
            @retrofit2.http.Header("Prefer") String preferCount, // Bắt buộc truyền "count=exact"
            @retrofit2.http.Query("artist_id") String artistIdQuery
    );
    @retrofit2.http.HEAD("follows")
    Call<Void> getFollowingCount(
            @retrofit2.http.Header("Prefer") String preferCount,
            @retrofit2.http.Query("follower_id") String followerIdQuery
    );
    @retrofit2.http.GET("follows?select=follower_id")
    Call<List<Object>> checkFollowStatus(
            @retrofit2.http.Query("follower_id") String followerIdEq,
            @retrofit2.http.Query("artist_id") String artistIdEq
    );

    // 4. Nhấn Follow (Thêm record)
    @retrofit2.http.POST("follows")
    Call<Void> followUser(@retrofit2.http.Body java.util.Map<String, String> followData);

    // 5. Bỏ Follow (Xóa record)
    @retrofit2.http.DELETE("follows")
    Call<Void> unfollowUser(
            @retrofit2.http.Query("follower_id") String followerIdEq,
            @retrofit2.http.Query("artist_id") String artistIdEq
    );


    // Bỏ dòng @Headers đi, để Retrofit tự lo!
// Trở về sự cơ bản, không thêm mắm dặm muối
    @retrofit2.http.POST("user_request_to_artist")
    retrofit2.Call<okhttp3.ResponseBody> requestArtistRole(
            @retrofit2.http.Body java.util.Map<String, Object> body // Dùng Map như cũ
    );

    // 1. API: Kiểm tra xem user có đang xin xỏ không
    @retrofit2.http.GET("user_request_to_artist")
    retrofit2.Call<java.util.List<Object>> checkArtistRequestStatus(
            @retrofit2.http.Query("user_id") String userIdQuery
    );

    // 2. API: Hủy (Xóa) yêu cầu
    @retrofit2.http.DELETE("user_request_to_artist")
    retrofit2.Call<okhttp3.ResponseBody> cancelArtistRequest(
            @retrofit2.http.Query("user_id") String userIdQuery
    );
}
