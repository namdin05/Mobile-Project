package com.melodix.app.Utils;

import android.text.TextUtils;
import android.util.Patterns;

public final class InputValidator {

    private InputValidator() {
    }

    public static String validateLogin(String email, String password) {
        String safeEmail = email == null ? "" : email.trim();
        String safePassword = password == null ? "" : password.trim();

        if (TextUtils.isEmpty(safeEmail)) {
            return "Bạn chưa nhập email.";
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(safeEmail).matches()) {
            return "Email không đúng định dạng.";
        }

        if (TextUtils.isEmpty(safePassword)) {
            return "Bạn chưa nhập mật khẩu.";
        }

        if (safePassword.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự.";
        }

        return null;
    }

    public static String validateRegister(String fullName, String email, String password, String confirmPassword) {
        String safeName = fullName == null ? "" : fullName.trim();
        String safeEmail = email == null ? "" : email.trim();
        String safePassword = password == null ? "" : password.trim();
        String safeConfirmPassword = confirmPassword == null ? "" : confirmPassword.trim();

        if (TextUtils.isEmpty(safeName)) {
            return "Bạn chưa nhập họ và tên.";
        }

        if (safeName.length() < 2) {
            return "Họ và tên phải có ít nhất 2 ký tự.";
        }

        if (TextUtils.isEmpty(safeEmail)) {
            return "Bạn chưa nhập email.";
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(safeEmail).matches()) {
            return "Email không đúng định dạng.";
        }

        if (TextUtils.isEmpty(safePassword)) {
            return "Bạn chưa nhập mật khẩu.";
        }

        if (safePassword.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự.";
        }

        if (TextUtils.isEmpty(safeConfirmPassword)) {
            return "Bạn chưa nhập lại mật khẩu.";
        }

        if (!safePassword.equals(safeConfirmPassword)) {
            return "Mật khẩu nhập lại không khớp.";
        }

        return null;
    }
}