package com.melodix.app.Repository.auth;

import androidx.lifecycle.MutableLiveData;
import com.melodix.app.Model.AuthResponse;
import com.melodix.app.Model.SignInRequest;
import com.melodix.app.Service.AuthAPIService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AuthRepository {
    private static final String BASE_URL = "https://ggektdtrjagrmfnimmaw.supabase.co/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdnZWt0ZHRyamFncm1mbmltbWF3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQxNzkyNzMsImV4cCI6MjA4OTc1NTI3M30.KoI-b-p-NblH7ZVdAw-y93ZGB3JCAWSK-w5MtKNPCWw"; // Thay Key của bạn vào

    private AuthAPIService apiService;

    public AuthRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(AuthAPIService.class);
    }

    // Trả về MutableLiveData để ViewModel quan sát
    public MutableLiveData<String> signInWithEmail(String email, String password) {
        MutableLiveData<String> loginResult = new MutableLiveData<>();
        SignInRequest request = new SignInRequest(email, password);

        apiService.signInWithEmail(API_KEY, request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Thành công: Trả về Token
                    loginResult.setValue(response.body().getAccessToken());
                } else {
                    // Thất bại: Trả về null hoặc câu thông báo lỗi
                    loginResult.setValue("ERROR: Sai tài khoản hoặc mật khẩu");
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                loginResult.setValue("ERROR: Lỗi kết nối mạng");
            }
        });

        return loginResult;
    }
}