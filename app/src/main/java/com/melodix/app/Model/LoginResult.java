package com.melodix.app.Model;

public class LoginResult {
    private boolean isSuccess;
    private String role;
    private String errorMessage;

    // Trạng thái thành công
    public LoginResult(boolean isSuccess, String role) {
        this.isSuccess = isSuccess;
        this.role = role;
    }

    // Trạng thái thất bại
    public LoginResult(boolean isSuccess, String errorMessage, boolean isError) {
        this.isSuccess = isSuccess;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() { return isSuccess; }
    public String getRole() { return role; }
    public String getErrorMessage() { return errorMessage; }
}
