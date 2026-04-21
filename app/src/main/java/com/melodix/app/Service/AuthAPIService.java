package com.melodix.app.Service;

import com.melodix.app.Model.AuthResponse;
import com.melodix.app.Model.Profile;
import com.melodix.app.Model.SignInRequest;
import com.melodix.app.Model.SignUpRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import java.util.List;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AuthAPIService {
    @POST("token?grant_type=password")
    Call<AuthResponse> signInWithEmail(
            @Body SignInRequest request
    );

    @POST("signup")
    Call<AuthResponse> signUpWithEmail(
            @Body SignUpRequest request
    );

    @GET("user")
    Call<ResponseBody> getUserInfo(
    );
}
