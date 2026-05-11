package com.melodix.app.Service;

import com.melodix.app.Model.Profile;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ProfileAPIService  {

    // ===== GET METHODS =====
    @GET("profiles")
    Call<List<Profile>> getAllProfiles();

    @GET("profiles")
    Call<List<Profile>> getProfileByEmail(
            @Query("username") String emailFilter
    );

    @Headers("Cache-Control: no-cache")
    @GET("profiles?select=display_name,avatar_url, role")
    Call<List<Profile>> getProfileById(
            @Query("id") String idFilter
    );

    // ===== PATCH METHODS =====
    @PATCH("profiles")
    Call<ResponseBody> updateProfile(
            @Query("id") String idFilter,
            @Body java.util.Map<String, Object> bodyData
    );

    @PATCH("profiles")
    Call<Void> updateFcmToken(
            @Query(value = "id", encoded = true) String idFilter,
            @Body Map<String, Object> bodyData
    );

    @HEAD("follows")
    Call<Void> getFollowerCount(
            @Header("Prefer") String preferCount, // Bắt buộc truyền "count=exact"
            @Query("artist_id") String artistIdQuery
    );
    @HEAD("follows")
    Call<Void> getFollowingCount(
            @Header("Prefer") String preferCount,
            @Query("follower_id") String followerIdQuery
    );
    @GET("follows?select=follower_id")
    Call<List<Object>> checkFollowStatus(
            @Query("follower_id") String followerIdEq,
            @Query("artist_id") String artistIdEq
    );

    // 4. Nhấn Follow (Thêm record)
    @POST("follows")
    Call<Void> followUser(@Body Map<String, String> followData);

    // 5. Bỏ Follow (Xóa record)
    @DELETE("follows")
    Call<Void> unfollowUser(
            @Query("follower_id") String followerIdEq,
            @Query("artist_id") String artistIdEq
    );

    @POST("user_request_to_artist")
    Call<okhttp3.ResponseBody> requestArtistRole(
            @Body Map<String, Object> body
    );

    @GET("user_request_to_artist")
    retrofit2.Call<List<Object>> checkArtistRequestStatus(
            @retrofit2.http.Query("user_id") String userIdQuery
    );

    @DELETE("user_request_to_artist")
    Call<ResponseBody> cancelArtistRequest(
            @Query("user_id") String userIdQuery
    );
}
