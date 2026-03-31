package com.melodix.app.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtils {
    public static String formatDuration(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", min, sec);
    }

    public static String formatMillis(long millis) {
        int total = (int) (millis / 1000L);
        return formatDuration(total);
    }

    public static String relative(long timeMs) {
        long diff = System.currentTimeMillis() - timeMs;
        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "just now";
        }
        if (diff < TimeUnit.HOURS.toMillis(1)) {
            return (diff / TimeUnit.MINUTES.toMillis(1)) + "m ago";
        }
        if (diff < TimeUnit.DAYS.toMillis(1)) {
            return (diff / TimeUnit.HOURS.toMillis(1)) + "h ago";
        }
        if (diff < TimeUnit.DAYS.toMillis(7)) {
            return (diff / TimeUnit.DAYS.toMillis(1)) + "d ago";
        }
        return new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date(timeMs));
    }
}
