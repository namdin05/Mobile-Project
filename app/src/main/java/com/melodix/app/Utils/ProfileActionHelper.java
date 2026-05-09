package com.melodix.app.Utils;

import android.content.Context;
import android.widget.Toast;

import com.melodix.app.Model.Profile;
import com.melodix.app.Repository.AuthRepository;

public class ProfileActionHelper {

    public static void handleMenuClick(Context context, Profile profile, String action) {
        AuthRepository authRepository = new AuthRepository(context);
        
        switch (action) {
            case "view":
                Toast.makeText(context, "VIEW " + profile.getDisplayName(), Toast.LENGTH_SHORT).show();
                break;
            case "ban":
                authRepository.banUser(profile.getId()).observeForever(result -> {
                    if ("SUCCESS_BAN".equals(result)) {
                        Toast.makeText(context, "Đã khóa tài khoản " + profile.getDisplayName(), Toast.LENGTH_SHORT).show();
                        // Vì cột nằm ở bảng Auth, ta cập nhật tạm Local để UI đổi nút Unban
                        profile.setBannedUntil("2099-12-31T23:59:59Z");
                    } else {
                        Toast.makeText(context, "Lỗi: " + result, Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case "unban":
                authRepository.unbanUser(profile.getId()).observeForever(result -> {
                    if ("SUCCESS_UNBAN".equals(result)) {
                        Toast.makeText(context, "Đã mở khóa tài khoản " + profile.getDisplayName(), Toast.LENGTH_SHORT).show();
                        profile.setBannedUntil("none");
                    } else {
                        Toast.makeText(context, "Lỗi: " + result, Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }
}
