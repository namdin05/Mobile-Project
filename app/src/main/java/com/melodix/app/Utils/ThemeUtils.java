package com.melodix.app.Utils;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtils {
    public static void applyNightMode(boolean dark) {
        AppCompatDelegate.setDefaultNightMode(dark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
