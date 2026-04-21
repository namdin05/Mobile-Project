package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class ArtistRequest {
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("created_at")
    private String createdAt;

    private String status;

    // Giả sử bạn có join với bảng profiles để lấy tên người dùng hiển thị cho Admin xem
    @SerializedName("profiles")
    private Profile userProfile;

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public Profile getUserProfile() { return userProfile; }
}
