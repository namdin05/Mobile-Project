package com.melodix.app.Service;

import com.melodix.app.Model.AuthResponse;
import com.melodix.app.Model.Profile;
import com.melodix.app.Model.SignInRequest;
import com.melodix.app.Model.SignUpRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import java.util.List;
import retrofit2.http.GET;
import retrofit2.http.Query;


public interface AuthAPIService {
    @POST("auth/v1/token?grant_type=password")
    Call<AuthResponse> signInWithEmail(
            @Header("apikey") String apiKey,
            @Body SignInRequest request
    );

    @POST("auth/v1/signup")
    Call<AuthResponse> signUpWithEmail(
            @Header("apikey") String apiKey,
            @Body SignUpRequest request
    );

    @GET("rest/v1/profiles")
    Call<List<Profile>> getProfile(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("id") String userIdFilter // Định dạng: eq.ID_CUA_USER
    );
}
