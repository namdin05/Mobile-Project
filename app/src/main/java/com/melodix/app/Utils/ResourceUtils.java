package com.melodix.app.Utils;

import android.content.Context;

public class ResourceUtils {

    public static int drawable(Context context, String name) {
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }

    public static int drawableNoDpi(Context context, String name) {
        return context.getResources().getIdentifier(name, "drawable-nodpi", context.getPackageName());
    }

    public static int anyDrawable(Context context, String name) {
        if (name == null || name.isEmpty()) return 0;
        int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        if (id == 0) {
            id = context.getResources().getIdentifier(name, "drawable-nodpi", context.getPackageName());
        }
        return id;
    }

    public static int raw(Context context, String name) {
        return context.getResources().getIdentifier(name, "raw", context.getPackageName());
    }
}