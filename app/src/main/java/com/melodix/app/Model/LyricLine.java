package com.melodix.app.Model;

import java.io.Serializable;
import java.util.Map;

public class LyricLine implements Serializable {

    private long timeMs;
    private String text;

    public LyricLine() {
        timeMs = -1L;
        text = "";
    }

    public LyricLine(long timeMs, String text) {
        this.timeMs = timeMs;
        this.text = text == null ? "" : text;
    }

    @SuppressWarnings("unchecked")
    public static LyricLine fromMap(Object rawObject) {
        LyricLine line = new LyricLine();

        try {
            if (!(rawObject instanceof Map)) {
                return line;
            }

            Map<String, Object> map = (Map<String, Object>) rawObject;

            long timeMs = getLongValue(map.get("timeMs"));
            if (timeMs <= 0L) {
                timeMs = getLongValue(map.get("timestampMs"));
            }
            if (timeMs <= 0L) {
                timeMs = getLongValue(map.get("startTimeMs"));
            }

            if (timeMs <= 0L) {
                long timeSeconds = getLongValue(map.get("timeSeconds"));
                if (timeSeconds > 0L) {
                    timeMs = timeSeconds * 1000L;
                }
            }

            String text = getStringValue(map.get("text"));
            if (text.isEmpty()) {
                text = getStringValue(map.get("line"));
            }
            if (text.isEmpty()) {
                text = getStringValue(map.get("content"));
            }

            line.setTimeMs(timeMs);
            line.setText(text);
        } catch (Exception ignored) {
        }

        return line;
    }

    private static long getLongValue(Object value) {
        try {
            if (value == null) {
                return 0L;
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String getStringValue(Object value) {
        try {
            if (value == null) {
                return "";
            }
            return String.valueOf(value).trim();
        } catch (Exception e) {
            return "";
        }
    }

    public String getDisplayText() {
        return text == null ? "" : text;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public void setTimeMs(long timeMs) {
        this.timeMs = timeMs;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
    }
}