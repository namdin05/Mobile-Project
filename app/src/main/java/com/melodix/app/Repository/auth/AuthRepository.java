package com.melodix.app.Repository.auth;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Constants;
import com.melodix.app.Model.AuthResponse;
import com.melodix.app.Model.LoginResult;
import com.melodix.app.Model.Profile;
import com.melodix.app.Service.StorageAPIService;
import com.melodix.app.Utils.SessionManager;
import com.melodix.app.Model.SignInRequest;
import com.melodix.app.Model.SignUpRequest;
import com.melodix.app.Service.AuthAPIService;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private AuthAPIService apiService;
    private ProfileAPIService profileAPISerivce;
    private StorageAPIService storageAPIService;

    public AuthRepository(Context context) {
        // Lưu ý: Dùng đúng tên hàm của RetrofitClient bạn đang có
        apiService = RetrofitClient.getAuth(context).create(AuthAPIService.class);
        profileAPISerivce = RetrofitClient.getClient(context).create(ProfileAPIService.class);
        storageAPIService = RetrofitClient.getStorage(context).create(StorageAPIService.class);
    }

    public MutableLiveData<LoginResult> signIn(String email, String password, Context context) {
        MutableLiveData<LoginResult> result = new MutableLiveData<>();
        SignInRequest request = new SignInRequest(email, password);

        apiService.signInWithEmail(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getAccessToken();
                    String refreshToken = response.body().getRefreshToken();
                    String userId = response.body().getUser().getId();

                    // Dùng SessionManager để lưu tạm Token cho Interceptor gọi hàm Profile
                    SessionManager.getInstance(context).updateToken(token);

                    fetchUserRoleAndProfile(userId, token, refreshToken, context, result);

                } else {
                    result.setValue(new LoginResult(false, "Sai tài khoản hoặc mật khẩu", true));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.setValue(new LoginResult(false, "Lỗi kết nối mạng", true));
            }
        });

        return result;
    }

    private void fetchUserRoleAndProfile(String userId, String token, String refreshToken, Context context, MutableLiveData<LoginResult> result) {
        String idFilter = "eq." + userId;

        profileAPISerivce.getProfileById(idFilter).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Log.e("SOCIAL_LOGIN", "4. Lấy Profile THÀNH CÔNG!");

                    Profile profile = response.body().get(0);
                    String role = profile.getRole();

                    // ĐÃ FIX: Lấy trực tiếp tên và avatar
                    String name = profile.getDisplayName(); // Nhớ check file Profile.java xem hàm này tên gì
                    String avatar = profile.getAvatarUrl(); // Nhớ check file Profile.java xem hàm này tên gì

                    // ĐÃ FIX: Dùng thẳng biến userId truyền vào từ tham số hàm, bỏ cái profile.getId() đi
                    SessionManager.getInstance(context).saveLogInSession(userId, role, token, refreshToken, name, avatar);

                    // Trả kết quả về với userId chuẩn
                    result.setValue(new LoginResult(true, role, userId));
                } else {
                    Log.e("SOCIAL_LOGIN", "4. LỖI PROFILE: Không tìm thấy dòng nào trong bảng profiles có id=" + userId);
                    result.setValue(new LoginResult(false, "Chưa có Profile trong Database", true));
                }
            }

            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                Log.e("SOCIAL_LOGIN", "LỖI MẠNG KHI TÌM PROFILE: " + t.getMessage());
                result.setValue(new LoginResult(false, "Lỗi tải thông tin phân quyền", true));
            }
        });
    }

    public MutableLiveData<String> signUp(String email, String password, String fullName) {
        MutableLiveData<String> result = new MutableLiveData<>();
        SignUpRequest request = new SignUpRequest(email, password, fullName);

        apiService.signUpWithEmail(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful()) {
                    result.setValue("Đăng ký thành công! Vui lòng kiểm tra email để xác thực.");
                } else {
                    result.setValue("Đăng ký thất bại: Tài khoản đã tồn tại hoặc email không hợp lệ");
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.setValue("Lỗi kết nối mạng");
            }
        });

        return result;
    }

    public MutableLiveData<LoginResult> handleSocialLogin(String accessToken, String refreshToken, Context context) {
        MutableLiveData<LoginResult> result = new MutableLiveData<>();

        // Dùng SessionManager để lưu tạm Token cho Interceptor gọi hàm UserInfo
        SessionManager.getInstance(context).updateToken(accessToken);

        apiService.getUserInfo().enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        org.json.JSONObject jsonObject = new org.json.JSONObject(jsonString);
                        String uid = jsonObject.getString("id");

                        Log.e("SOCIAL_LOGIN", "2. Lấy UID thành công: " + uid);

                        fetchUserRoleAndProfile(uid, accessToken, refreshToken, context, result);

                    } catch (Exception e) {
                        Log.e("SOCIAL_LOGIN", "LỖI PARSE JSON: " + e.getMessage());
                        result.setValue(new LoginResult(false, "Lỗi phân tích dữ liệu", true));
                    }
                } else {
                    Log.e("SOCIAL_LOGIN", "LỖI API LẤY USER: " + response.code());
                    result.setValue(new LoginResult(false, "Token MXH không hợp lệ", true));
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                Log.e("SOCIAL_LOGIN", "LỖI MẠNG: " + t.getMessage());
                result.setValue(new LoginResult(false, "Lỗi kết nối mạng", true));
            }
        });
        return result;
    }

    public void uploadPendingAvatarAndProfile(String userId, byte[] imageData, String fullName, MutableLiveData<String> result) {
        String fileName = userId + "_" + System.currentTimeMillis() + ".jpg";

        // Tạo RequestBody với kiểu dữ liệu là hình ảnh
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageData);

        Log.e("UPLOAD_AVATAR", "Bắt đầu upload ảnh lên Storage...");

        storageAPIService.uploadFileToStorage( "image/jpeg",
                "true",
                Constants.AVATAR_BUCKET.replace("/", ""),
                fileName,
                requestBody).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                // HTTP 200 (Thành công) hoặc 409 (Ảnh đã tồn tại do upload lại)
                if (response.isSuccessful() || response.code() == 409) {
                    Log.e("UPLOAD_AVATAR", "Upload ảnh thành công, tiến hành ghi vào profiles");

                    // Lấy Public URL của Supabase
                    String avatarUrl = com.melodix.app.BuildConfig.BASE_URL + "storage/v1/object/public/avatars/" + fileName;

                    // Gọi API update bảng profiles
                    Map<String, Object> body = new HashMap<>();
                    body.put("display_name", fullName);
                    body.put("avatar_url", avatarUrl);

                    profileAPISerivce.updateProfile("eq." + userId, body).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                result.setValue("SUCCESS_UPLOAD");
                            } else {
                                result.setValue("Lỗi cập nhật Profile: " + response.code());
                            }
                        }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            result.setValue("Lỗi mạng khi cập nhật Profile");
                        }
                    });
                } else {
                    result.setValue("Lỗi upload ảnh lên Storage: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                result.setValue("Lỗi mạng khi gọi Storage API: " + t.getMessage());
            }
        });
    }

    // Cập nhật mật khẩu mới bằng Token khôi phục
    public MutableLiveData<String> updateNewPassword(String accessToken, String newPassword) {
        MutableLiveData<String> result = new MutableLiveData<>();

        Map<String, String> body = new HashMap<>();
        body.put("password", newPassword);

        String authHeader = "Bearer " + accessToken;

        apiService.updateUserPassword(authHeader, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    result.setValue("SUCCESS");
                } else {
                    result.setValue("Lỗi cập nhật mật khẩu: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.setValue("Lỗi kết nối mạng");
            }
        });
        return result;
    }

    // Gọi Supabase gửi email reset mật khẩu
    public MutableLiveData<String> resetPassword(String email) {
        MutableLiveData<String> result = new MutableLiveData<>();

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        apiService.sendPasswordResetEmail(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    result.setValue("SUCCESS");
                } else {
                    result.setValue("Lỗi: Email không tồn tại hoặc có lỗi xảy ra");
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.setValue("Lỗi kết nối mạng");
            }
        });
        return result;
    }

    public MutableLiveData<String> changePassword(String newPassword) {
        MutableLiveData<String> result = new MutableLiveData<>();

        Map<String, String> body = new HashMap<>();
        body.put("password", newPassword);

        // Gọi hàm mới không cần truyền Token
        apiService.changePassword(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    result.setValue("SUCCESS");
                } else {
                    // Nếu trả về 401, có thể Token của user đã hết hạn (sống được 1 tiếng)
                    if (response.code() == 401) {
                        result.setValue("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại!");
                    } else {
                        result.setValue("Lỗi đổi mật khẩu: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.setValue("Lỗi kết nối mạng");
            }
        });
        return result;
    }

}