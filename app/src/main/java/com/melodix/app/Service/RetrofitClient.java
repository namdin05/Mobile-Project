package com.melodix.app.Service;

import android.content.Context;

import com.melodix.app.BuildConfig;
import com.melodix.app.Model.AuthResponse; // Đảm bảo import đúng đường dẫn
import com.melodix.app.Utils.SessionManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static OkHttpClient sharedHttpClient = null;

    private static Retrofit databaseRetrofit = null;
    private static Retrofit storageRetrofit = null;
    private static Retrofit authRetrofit = null;

    // =========================================================================
    // HÀM CHUNG: SETUP OKHTTP CLIENT (Gắn Chìa Khóa + Token + Tự động Refresh)
    // =========================================================================
    private static OkHttpClient getSharedHttpClient(Context context) {
        if (sharedHttpClient == null) {
            final Context safeContext = context.getApplicationContext();

            sharedHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)

                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            SessionManager sessionManager = SessionManager.getInstance(safeContext);

                            String role = sessionManager.getRole();
                            if (role == null) role = "user";

                            // LẤY ĐƯỜNG DẪN URL ĐỂ PHÂN LUỒNG
                            Request original = chain.request();
                            String path = original.url().encodedPath();

                            String apikey;
                            String authBearer;
                            String userToken = sessionManager.getAccessToken();

                            if ("admin".equals(role)) {
                                apikey = BuildConfig.SERVICE_KEY;

                                // PHÂN LUỒNG THÔNG MINH CHO ADMIN
                                if (path.startsWith("/auth/v1/admin/")) {
                                    // 1. Nhánh gọi API quản trị User (như Ban/Unban) -> Bắt buộc dùng Service Key
                                    authBearer = BuildConfig.SERVICE_KEY;
                                } else if (path.startsWith("/auth/")) {
                                    // 2. Gọi API Đăng nhập/Đổi mật khẩu cá nhân -> Dùng Token cá nhân
                                    authBearer = (userToken != null && !userToken.isEmpty()) ? userToken : BuildConfig.SERVICE_KEY;
                                } else {
                                    // 3. Gọi Database (rest) hoặc Storage -> Dùng Service Key để phá vỡ RLS
                                    authBearer = BuildConfig.SERVICE_KEY;
                                }
                            } else {
                                // USER BÌNH THƯỜNG THÌ DÙNG API_KEY VÀ TOKEN CÁ NHÂN CHO MỌI TRƯỜNG HỢP
                                apikey = BuildConfig.API_KEY;
                                authBearer = (userToken != null && !userToken.isEmpty()) ? userToken : BuildConfig.API_KEY;
                            }

                            Request.Builder requestBuilder = original.newBuilder();

                            if (original.header("apikey") == null) {
                                requestBuilder.addHeader("apikey", apikey);
                            }

                            if (original.header("Authorization") == null) {
                                requestBuilder.addHeader("Authorization", "Bearer " + authBearer);
                            }

                            return chain.proceed(requestBuilder.build());
                        }
                    })

                    // 2. AUTHENTICATOR: Bắt lỗi 401 và tự động xin cấp lại Token mới
                    .authenticator(new Authenticator() {
                        @Override
                        public Request authenticate(Route route, Response response) throws IOException {
                            // Chống lặp vô tận: Nếu đã thử đổi token rồi mà vẫn bị 401 thì từ bỏ
                            if (response.priorResponse() != null) {
                                return null;
                            }

                            SessionManager sessionManager = SessionManager.getInstance(safeContext);
                            String refreshToken = sessionManager.getRefreshToken(); // Cần viết thêm hàm này trong SessionManager

                            if (refreshToken == null || refreshToken.isEmpty()) {
                                return null;
                            }

                            // Gọi API làm mới token một cách đồng bộ (chờ kết quả ngay)
                            AuthAPIService authService = getAuth(safeContext).create(AuthAPIService.class);

                            Map<String, String> body = new HashMap<>();
                            body.put("refresh_token", refreshToken);

                            // Dùng execute() thay vì enqueue() để block luồng mạng hiện tại chờ token mới
                            retrofit2.Response<AuthResponse> refreshCall = authService.refreshToken(body).execute();

                            if (refreshCall.isSuccessful() && refreshCall.body() != null) {
                                // Cập nhật Token mới vào két sắt
                                String newAccessToken = refreshCall.body().getAccessToken();
                                String newRefreshToken = refreshCall.body().getRefreshToken();

                                sessionManager.updateTokens(newAccessToken, newRefreshToken); // Cần viết thêm hàm này

                                // Sửa lại cái Request cũ vừa bị lỗi, gắn thẻ mới vào và gửi đi tiếp
                                return response.request().newBuilder()
                                        .header("Authorization", "Bearer " + newAccessToken)
                                        .build();
                            } else {
                                // Nếu Refresh Token cũng hết hạn (thường là sau 6 tháng hoặc bị thu hồi)
                                // Xóa phiên làm việc để bắt người dùng đăng nhập lại
                                sessionManager.clear();
                                return null;
                            }
                        }
                    })
                    .build();
        }
        return sharedHttpClient;
    }

    // =========================================================================
    // NHÁNH 1: DATABASE (rest/v1/)
    // =========================================================================
    public static Retrofit getClient(Context context) {
        if (databaseRetrofit == null) {
            databaseRetrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL + "rest/v1/")
                    .client(getSharedHttpClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return databaseRetrofit;
    }

    // =========================================================================
    // NHÁNH 2: STORAGE (storage/v1/)
    // =========================================================================
    public static Retrofit getStorage(Context context) {
        if (storageRetrofit == null) {
            storageRetrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL + "storage/v1/object/")
                    .client(getSharedHttpClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return storageRetrofit;
    }

    // =========================================================================
    // NHÁNH 3: AUTH (auth/v1/)
    // =========================================================================
    public static Retrofit getAuth(Context context) {
        if (authRetrofit == null) {
            authRetrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL + "auth/v1/")
                    .client(getSharedHttpClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return authRetrofit;
    }
}