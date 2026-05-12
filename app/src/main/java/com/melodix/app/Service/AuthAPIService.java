package com.melodix.app.Service;

import com.melodix.app.Model.AuthResponse;
import com.melodix.app.Model.SignInRequest;
import com.melodix.app.Model.SignUpRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface AuthAPIService {
    @POST("token?grant_type=password")
    Call<AuthResponse> signInWithEmail(
            @Body SignInRequest request
    );

    @POST("token?grant_type=refresh_token")
    Call<AuthResponse> refreshToken(@Body java.util.Map<String, String> body);

    @POST("signup")
    Call<AuthResponse> signUpWithEmail(
            @Body SignUpRequest request
    );

    @GET("user")
    Call<ResponseBody> getUserInfo();

    @POST("recover")
    Call<Void> sendPasswordResetEmail(@Body java.util.Map<String, String> body);

    @PUT("user")
    Call<Void> updateUserPassword(
            @Header("Authorization") String token,
            @Body java.util.Map<String, String> body
    );

    @PUT("user")
    Call<Void> changePassword(
            @Body java.util.Map<String, String> body
    );

    @PUT("admin/users/{uid}")
    Call<Void> updateUserAdminStatus(
            @Path("uid") String uid,
            @Body java.util.Map<String, Object> body
    );

    
    @GET("admin/users/{uid}")
    Call<ResponseBody> getUserAdminDetails(@Path("uid") String uid);
}
