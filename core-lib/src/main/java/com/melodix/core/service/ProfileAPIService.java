package com.melodix.core.service;

import com.melodix.core.model.Profile;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.DELETE;
import retrofit2.http.Query;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Headers;

public interface ProfileAPIService {

    @GET("profiles")
    Call<List<Profile>> getAllProfiles();

    @Headers("Cache-Control: no-cache")
    @GET("profiles?select=display_name,avatar_url,role")
    Call<List<Profile>> getProfileById(
            @Query("id") String idFilter
    );

    @PATCH("profiles")
    Call<ResponseBody> updateProfile(
            @Query("id") String idFilter,
            @Body Map<String, Object> bodyData
    );

    @PATCH("profiles")
    Call<Void> updateFcmToken(
            @Query(value = "id", encoded = true) String idFilter,
            @Body Map<String, Object> bodyData
    );

    @GET("profiles")
    Call<List<Profile>> getProfileByEmail(
            @Query("username") String emailFilter
    );

    @retrofit2.http.HEAD("follows")
    Call<Void> getFollowerCount(
            @Header("Prefer") String preferCount,
            @Query("artist_id") String artistIdQuery
    );

    @retrofit2.http.HEAD("follows")
    Call<Void> getFollowingCount(
            @Header("Prefer") String preferCount,
            @Query("follower_id") String followerIdQuery
    );

    @GET("follows?select=follower_id")
    Call<List<Object>> checkFollowStatus(
            @Query("follower_id") String followerIdEq,
            @Query("artist_id") String artistIdEq
    );

    @POST("follows")
    Call<Void> followUser(@Body Map<String, String> followData);

    @DELETE("follows")
    Call<Void> unfollowUser(
            @Query("follower_id") String followerIdEq,
            @Query("artist_id") String artistIdEq
    );

    @POST("user_request_to_artist")
    Call<ResponseBody> requestArtistRole(
            @Body Map<String, Object> body
    );

    @GET("user_request_to_artist")
    Call<List<Object>> checkArtistRequestStatus(
            @Query("user_id") String userIdQuery
    );

    @DELETE("user_request_to_artist")
    Call<ResponseBody> cancelArtistRequest(
            @Query("user_id") String userIdQuery
    );
}