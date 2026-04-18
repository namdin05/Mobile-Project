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

    @GET("rest/v1/profiles")
    Call<List<Profile>> getAllProfiles(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token
    );

    // Lấy thông tin của 1 user cụ thể dựa vào ID
    @GET("rest/v1/profiles?select=display_name,avatar_url")
    Call<List<Profile>> getProfileById(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("id") String idFilter // Truyền vào dạng "eq.uuid-của-admin"
    );

    // Hàm Upload đa năng cho mọi Bucket
    @POST("storage/v1/object/{bucketName}/{fileName}")
    Call<ResponseBody> uploadFileToStorage(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Header("Content-Type") String contentType,
            @Header("x-upsert") String upsert,
            @Path("bucketName") String bucketName, // <-- Thêm biến này
            @Path("fileName") String fileName,
            @Body RequestBody file
    );

    // 2. Hàm Cập nhật (PATCH) thông tin profile
    @PATCH("rest/v1/profiles")
    Call<ResponseBody> updateProfile(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("id") String idFilter, // eq.[UID]
            @Body java.util.Map<String, Object> bodyData // Gửi HashMap cho tiện
    );

    @PATCH("/rest/v1/profiles")
    Call<Void> updateFcmToken(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query(value = "id", encoded = true) String idFilter, // THÊM encoded = true VÀO ĐÂY
            @Body Map<String, Object> bodyData
    );


    // 1. Đếm số người ĐANG THEO DÕI user này (Followers)
    @retrofit2.http.HEAD("follow")
    Call<Void> getFollowerCount(
            @retrofit2.http.Header("Prefer") String preferCount, // Bắt buộc truyền "count=exact"
            @retrofit2.http.Query("artist_id") String artistIdQuery
    );

    // 2. Đếm số người mà user này ĐANG ĐI THEO DÕI (Following)
    @retrofit2.http.HEAD("follow")
    Call<Void> getFollowingCount(
            @retrofit2.http.Header("Prefer") String preferCount,
            @retrofit2.http.Query("follower_id") String followerIdQuery
    );

    // 3. Kiểm tra xem mình đã follow user này chưa
    @retrofit2.http.GET("follow?select=follower_id")
    Call<List<Object>> checkFollowStatus(
            @retrofit2.http.Query("follower_id") String followerIdEq,
            @retrofit2.http.Query("artist_id") String artistIdEq
    );

    // 4. Nhấn Follow (Thêm record)
    @retrofit2.http.POST("follow")
    Call<Void> followUser(@retrofit2.http.Body java.util.Map<String, String> followData);

    // 5. Bỏ Follow (Xóa record)
    @retrofit2.http.DELETE("follow")
    Call<Void> unfollowUser(
            @retrofit2.http.Query("follower_id") String followerIdEq,
            @retrofit2.http.Query("artist_id") String artistIdEq
    );

}
