package com.melodix.app.Service;

import com.melodix.app.Model.AuthResponse;
import com.melodix.app.Model.SignInRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AuthAPIService {
    @POST("auth/v1/token?grant_type=password")
    Call<AuthResponse> signInWithEmail(
            @Header("apikey") String apiKey,
            @Body SignInRequest request
    );
}
