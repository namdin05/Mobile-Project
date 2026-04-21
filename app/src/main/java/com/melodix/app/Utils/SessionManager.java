package com.melodix.app.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.melodix.app.Model.Profile;

public class SessionManager {

    // 1. CHUẨN HÓA TÊN KÉT SẮT VÀ CHÌA KHÓA (Phải khớp 100% với RetrofitClient)
    private static final String PREF_NAME = "MelodixPrefs";
    private static final String KEY_USER_ID = "USER_ID";
    private static final String KEY_ROLE = "USER_ROLE";
    private static final String KEY_ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String KEY_IS_LOGGED_IN = "IS_LOGGED_IN";

    private static SessionManager instance;
    private final SharedPreferences preferences;

    // Khởi tạo private để ép dùng Singleton
    private SessionManager(Context context) {
        // Dùng getApplicationContext() để chống Memory Leak
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 2. BỔ SUNG SINGLETON (Để Repo gọi được SessionManager.getInstance)
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    // 3. BỔ SUNG HÀM LƯU SESSION MÀ AUTH REPOSITORY ĐANG ĐÒI
    // ĐÃ SỬA: Nhận trực tiếp chuỗi userId
    public void saveLogInSession(String userId, String role, String token) {
        preferences.edit()
                .putString(KEY_USER_ID, userId) // Giờ thì chắc chắn 100% có ID thật
                .putString(KEY_ROLE, role)
                .putString(KEY_ACCESS_TOKEN, token)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    // Cập nhật lại Token (nếu Token cũ hết hạn)
    public void updateToken(String newToken) {
        preferences.edit().putString(KEY_ACCESS_TOKEN, newToken).apply();
    }

    // Dọn dẹp khi Đăng xuất
    public void clear() {
        preferences.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_ROLE)
                .remove(KEY_ACCESS_TOKEN)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
    }

    // Các hàm Getter cho toàn App xài
    public String getUserId() {
        return preferences.getString(KEY_USER_ID, null);
    }

    public String getRole() {
        return preferences.getString(KEY_ROLE, null);
    }

    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public boolean hasSession() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false)
                && getUserId() != null
                && getAccessToken() != null;
    }
}