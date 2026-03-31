package com.melodix.app.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String KEY_USER_ID = "session_user_id";
    private static final String KEY_ROLE = "session_role";
    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(Constants.PREFS_APP, Context.MODE_PRIVATE);
    }

    public void saveSession(String userId, String role) {
        preferences.edit().putString(KEY_USER_ID, userId).putString(KEY_ROLE, role).apply();
    }

    public void clear() {
        preferences.edit().remove(KEY_USER_ID).remove(KEY_ROLE).apply();
    }

    public String getUserId() {
        return preferences.getString(KEY_USER_ID, null);
    }

    public String getRole() {
        return preferences.getString(KEY_ROLE, null);
    }

    public boolean hasSession() {
        return getUserId() != null && getRole() != null;
    }
}
