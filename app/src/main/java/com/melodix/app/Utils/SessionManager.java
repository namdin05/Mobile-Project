package com.melodix.app.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "MelodixPrefs";
    private static final String KEY_USER_ID = "USER_ID";
    private static final String KEY_ROLE = "USER_ROLE";
    private static final String KEY_ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String KEY_IS_LOGGED_IN = "IS_LOGGED_IN";

    // 👇 THÊM 2 KEY MỚI 👇
    private static final String KEY_USER_NAME = "USER_NAME";
    private static final String KEY_USER_AVATAR = "USER_AVATAR";

    private static SessionManager instance;
    private final SharedPreferences preferences;

    private SessionManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    // 👇 SỬA HÀM NÀY: Nhận thêm userName và userAvatar 👇
    public void saveLogInSession(String userId, String role, String token, String userName, String userAvatar) {
        preferences.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_ROLE, role)
                .putString(KEY_ACCESS_TOKEN, token)
                .putString(KEY_USER_NAME, userName)       // Lưu tên
                .putString(KEY_USER_AVATAR, userAvatar)   // Lưu avatar
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    // Để dành cho lúc cập nhật Role ngầm (như đoạn code ở AccountFragment)
    // Mình tạo thêm 1 hàm nạp chồng (overload) giữ nguyên token/name/avatar cũ
    public void updateRole(String newRole) {
        preferences.edit().putString(KEY_ROLE, newRole).apply();
    }

    public void updateToken(String newToken) {
        preferences.edit().putString(KEY_ACCESS_TOKEN, newToken).apply();
    }

    public void clear() {
        preferences.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_ROLE)
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_USER_NAME)      // 👇 Xóa khi đăng xuất
                .remove(KEY_USER_AVATAR)    // 👇 Xóa khi đăng xuất
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
    }

    public String getUserId() { return preferences.getString(KEY_USER_ID, null); }
    public String getRole() { return preferences.getString(KEY_ROLE, null); }
    public String getAccessToken() { return preferences.getString(KEY_ACCESS_TOKEN, null); }

    // 👇 THÊM 2 HÀM GETTER NÀY 👇
    public String getUserName() { return preferences.getString(KEY_USER_NAME, "Người dùng"); }
    public String getUserAvatar() { return preferences.getString(KEY_USER_AVATAR, ""); }

    public boolean hasSession() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false)
                && getUserId() != null
                && getAccessToken() != null;
    }
}