package com.melodix.app.Utils;

import androidx.media3.common.C;

import java.util.Locale;

public final class PlaybackTimeUtils {

    private PlaybackTimeUtils() {
    }

    public static String formatDuration(long millis) {
        try {
            if (millis == C.TIME_UNSET || millis < 0) {
                return "0:00";
            }

            long totalSeconds = millis / 1000L;
            long hours = totalSeconds / 3600L;
            long minutes = (totalSeconds % 3600L) / 60L;
            long seconds = totalSeconds % 60L;

            if (hours > 0L) {
                return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
            }

            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        } catch (Exception e) {
            return "0:00";
        }
    }
}