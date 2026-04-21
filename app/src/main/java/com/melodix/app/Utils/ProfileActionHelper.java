package com.melodix.app.Utils;

import android.content.Context;
import android.widget.Toast;

import com.melodix.app.Model.Profile;
import com.melodix.app.Model.Song;

import java.util.ArrayList;
import java.util.List;

public class ProfileActionHelper {

    public static void handleMenuClick(Context context, Profile profile, String action) {
        switch (action) {
            case "view":
                Toast.makeText(context, "VIEW " + profile.getDisplayName(), Toast.LENGTH_SHORT).show();
                // TODO: Gọi API thả tim lên Supabase ở đây
                break;
            case "ban":
                Toast.makeText(context, "BAN " + profile.getDisplayName(), Toast.LENGTH_SHORT).show();
                // TODO: Gọi API thêm vào playlist
                break;
        }
    }
}
