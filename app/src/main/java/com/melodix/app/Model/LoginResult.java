package com.melodix.app.Model;

public class LoginResult {
    private boolean isSuccess;
    private String role;
    private String errorMessage;

    private String userId;

    public LoginResult(boolean isSuccess, String role, String userId) {
        this.isSuccess = isSuccess;
        this.role = role;
        this.userId = userId;
    }
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