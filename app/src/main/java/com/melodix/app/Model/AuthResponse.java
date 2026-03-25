package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("access_token")
    private String accessToken;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    @SerializedName("user")
    private User user;

    // 2. THÊM HÀM GETTER NÀY LÀ HẾT LỖI ĐỎ Ở REPOSITORY
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}