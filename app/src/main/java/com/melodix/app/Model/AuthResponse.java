package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    @SerializedName("user")
    private Profile user;

    // 2. THÊM HÀM GETTER NÀY LÀ HẾT LỖI ĐỎ Ở REPOSITORY
    public Profile getUser() {
        return user;
    }

    public void setUser(Profile user) {
        this.user = user;
    }
}