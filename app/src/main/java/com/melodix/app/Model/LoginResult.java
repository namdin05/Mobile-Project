package com.melodix.app.Model;

public class LoginResult {
    private boolean isSuccess;
    private String role;
    private String errorMessage;

    // 1. THÊM BIẾN LƯU ID
    private String userId;

    // 2. CẬP NHẬT CONSTRUCTOR THÀNH CÔNG (Thêm tham số userId)
    public LoginResult(boolean isSuccess, String role, String userId) {
        this.isSuccess = isSuccess;
        this.role = role;
        this.userId = userId;
    }

    // Trạng thái thất bại (Giữ nguyên)
    public LoginResult(boolean isSuccess, String errorMessage, boolean isError) {
        this.isSuccess = isSuccess;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() { return isSuccess; }
    public String getRole() { return role; }
    public String getErrorMessage() { return errorMessage; }

    // 3. THÊM HÀM GETTER ĐỂ VIEW LẤY DATA
    public String getUserId() { return userId; }
}