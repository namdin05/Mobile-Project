package com.melodix.app.Service;


import com.melodix.app.BuildConfig; // Import BuildConfig hôm bữa chúng ta cấu hình
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // Biến lưu trữ "cái điện thoại" dùng chung
    private static Retrofit retrofit = null;

    // Hàm cung cấp Retrofit cho toàn bộ App
    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    // Lấy link Supabase từ file local.properties an toàn tuyệt đối
                    .baseUrl(BuildConfig.BASE_URL)
                    // Thêm bộ dịch thuật JSON -> Java Model (Gson)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
