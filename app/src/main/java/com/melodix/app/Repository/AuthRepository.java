package com.melodix.app.Repository;

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

import org.json.JSONObject;

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

                    SessionManager.getInstance(context).updateToken(token);
                    fetchUserRoleAndProfile(userId, token, refreshToken, context, result);

                } else {
                    String errorMsg = "Sai tài khoản hoặc mật khẩu";
                    if (response.code() == 403) {
                        errorMsg = "Tài khoản của bạn đã bị khóa bởi quản trị viên.";
                    } else {
                        try {
                            if (response.errorBody() != null) {
                                String errorStr = response.errorBody().string();
                                JSONObject json = new JSONObject(errorStr);
                                String msg = json.optString("error_description", json.optString("msg", ""));
                                if (msg.toLowerCase().contains("banned")) {
                                    errorMsg = "Tài khoản của bạn đã bị khóa bởi quản trị viên.";
                                }
                            }
                        } catch (Exception e) {
                            Log.e("AUTH_REPO", "Error parsing error", e);
                        }
                    }
                    result.setValue(new LoginResult(false, errorMsg, true));
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
        profileAPISerivce.getProfileById("eq." + userId).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Profile profile = response.body().get(0);
                    SessionManager.getInstance(context).saveLogInSession(userId, profile.getRole(), token, refreshToken, profile.getDisplayName(), profile.getAvatarUrl());
                    result.setValue(new LoginResult(true, profile.getRole(), userId));
                } else {
                    result.setValue(new LoginResult(false, "Chưa có Profile trong Database", true));
                }
            }
            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                result.setValue(new LoginResult(false, "Lỗi tải thông tin phân quyền", true));
            }
        });
    }

    public MutableLiveData<LoginResult> handleSocialLogin(String accessToken, String refreshToken, Context context) {
        MutableLiveData<LoginResult> result = new MutableLiveData<>();
        SessionManager.getInstance(context).updateToken(accessToken);
        apiService.getUserInfo().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonString);
                        String uid = jsonObject.getString("id");
                        fetchUserRoleAndProfile(uid, accessToken, refreshToken, context, result);
                    } catch (Exception e) {
                        result.setValue(new LoginResult(false, "Lỗi phân tích dữ liệu", true));
                    }
                } else {
                    String errorMsg = "Token MXH không hợp lệ";
                    // KIỂM TRA LỖI BAN KHI GỌI USER INFO
                    if (response.code() == 403) {
                        errorMsg = "Tài khoản của bạn đã bị khóa bởi quản trị viên.";
                    } else {
                        try {
                            if (response.errorBody() != null) {
                                String errorStr = response.errorBody().string();
                                if (errorStr.toLowerCase().contains("banned")) {
                                    errorMsg = "Tài khoản của bạn đã bị khóa bởi quản trị viên.";
                                }
                            }
                        } catch (Exception e) {}
                    }
                    result.setValue(new LoginResult(false, errorMsg, true));
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                result.setValue(new LoginResult(false, "Lỗi kết nối mạng", true));
            }
        });
        return result;
    }

    public MutableLiveData<String> signUp(String email, String password, String fullName) {
        MutableLiveData<String> result = new MutableLiveData<>();
        profileAPISerivce.getProfileByEmail("eq." + email).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    result.setValue("ERROR: Email này đã được đăng ký!");
                } else {
                    proceedToSignUp(email, password, fullName, result);
                }
            }
            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                result.setValue("ERROR: Lỗi kiểm tra tài khoản, vui lòng thử lại sau.");
            }
        });
        return result;
    }

    private void proceedToSignUp(String email, String password, String fullName, MutableLiveData<String> result) {
        SignUpRequest request = new SignUpRequest(email, password, fullName);
        apiService.signUpWithEmail(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful()) result.setValue("SUCCESS");
                else result.setValue("ERROR: Đăng ký thất bại");
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) { result.setValue("ERROR: Lỗi mạng"); }
        });
    }

    public void uploadPendingAvatarAndProfile(String userId, byte[] imageData, String fullName, MutableLiveData<String> result) {
        String fileName = userId + "_" + System.currentTimeMillis() + ".jpg";
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageData);
        storageAPIService.uploadFileToStorage("image/jpeg", "true", Constants.AVATAR_BUCKET.replace("/", ""), fileName, requestBody).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() || response.code() == 409) {
                    String avatarUrl = Constants.STORAGE_BASE_URL + Constants.AVATAR_BUCKET + fileName;
                    Map<String, Object> body = new HashMap<>();
                    body.put("display_name", fullName);
                    body.put("avatar_url", avatarUrl);
                    profileAPISerivce.updateProfile("eq." + userId, body).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) result.setValue("SUCCESS_UPLOAD");
                            else result.setValue("Lỗi cập nhật Profile: " + response.code());
                        }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) { result.setValue("Lỗi mạng khi cập nhật Profile"); }
                    });
                } else { result.setValue("Lỗi upload ảnh: " + response.code()); }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) { result.setValue("Lỗi mạng khi upload"); }
        });
    }

    public MutableLiveData<String> updateNewPassword(String accessToken, String newPassword) {
        MutableLiveData<String> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("password", newPassword);
        apiService.updateUserPassword("Bearer " + accessToken, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) result.setValue("SUCCESS");
                else result.setValue("Lỗi: " + response.code());
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) { result.setValue("Lỗi kết nối mạng"); }
        });
        return result;
    }

    public MutableLiveData<String> resetPassword(String email) {
        MutableLiveData<String> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        apiService.sendPasswordResetEmail(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) result.setValue("SUCCESS");
                else result.setValue("Lỗi: Email không tồn tại");
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) { result.setValue("Lỗi kết nối mạng"); }
        });
        return result;
    }

    public MutableLiveData<String> changePassword(String newPassword) {
        MutableLiveData<String> result = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        body.put("password", newPassword);
        apiService.changePassword(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) result.setValue("SUCCESS");
                else result.setValue(response.code() == 401 ? "Phiên đăng nhập hết hạn" : "Lỗi: " + response.code());
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) { result.setValue("Lỗi kết nối mạng"); }
        });
        return result;
    }

    public MutableLiveData<String> banUser(String targetUserId) {
        MutableLiveData<String> result = new MutableLiveData<>();
        Map<String, Object> body = new HashMap<>();
        body.put("ban_duration", "876000h");
        apiService.updateUserAdminStatus(targetUserId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) result.setValue("SUCCESS_BAN");
                else result.setValue("Lỗi: " + response.code());
            }
            @Override public void onFailure(Call<Void> call, Throwable t) { result.setValue("Lỗi mạng"); }
        });
        return result;
    }

    public MutableLiveData<String> unbanUser(String targetUserId) {
        MutableLiveData<String> result = new MutableLiveData<>();
        Map<String, Object> body = new HashMap<>();
        body.put("ban_duration", "none");
        apiService.updateUserAdminStatus(targetUserId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) result.setValue("SUCCESS_UNBAN");
                else result.setValue("Lỗi: " + response.code());
            }
            @Override public void onFailure(Call<Void> call, Throwable t) { result.setValue("Lỗi mạng"); }
        });
        return result;
    }

    public MutableLiveData<Boolean> checkUserBanStatus(String userId) {
        MutableLiveData<Boolean> isBanned = new MutableLiveData<>();
        apiService.getUserAdminDetails(userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject json = new JSONObject(response.body().string());
                        String bannedUntil = json.optString("banned_until", "");
                        isBanned.setValue(bannedUntil != null && !bannedUntil.isEmpty() && !bannedUntil.equalsIgnoreCase("none"));
                    } else isBanned.setValue(false);
                } catch (Exception e) { isBanned.setValue(false); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) { isBanned.setValue(false); }
        });
        return isBanned;
    }
}
