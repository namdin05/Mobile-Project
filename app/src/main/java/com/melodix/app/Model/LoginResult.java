package com.melodix.app.Model;

public class LoginResult {
    private boolean isSuccess;
    private String role;
    private String errorMessage;
    private String accessToken;
    private String userId;

    // Trạng thái thành công
    public LoginResult(boolean isSuccess, String role, String accessToken, String userId) {
        this.isSuccess = isSuccess;
        this.role = role;
        this.accessToken = accessToken;
        this.userId = userId;
    }

    // Trạng thái thất bại
    public LoginResult(boolean isSuccess, String errorMessage, boolean isError) {
        this.isSuccess = isSuccess;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() { return isSuccess; }
    public String getRole() { return role; }
    public String getErrorMessage() { return errorMessage; }
    public String getAccessToken() { return accessToken; }
    public String getUserId() { return userId; }
}
