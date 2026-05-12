package com.melodix.app.Utils;

import android.util.Log;

public class DateHelper {
    public static String formatSupabaseDate(String supabaseDate) {
        if (supabaseDate == null || supabaseDate.isEmpty()) return "";

        try {
            String mainPart = supabaseDate;
            if (supabaseDate.length() >= 19) {
                mainPart = supabaseDate.substring(0, 19);
            }

            mainPart = mainPart.replace("T", " ");
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = inputFormat.parse(mainPart);
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            outputFormat.setTimeZone(java.util.TimeZone.getDefault());

            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            Log.e("DateUtils", "Lỗi parse ngày tháng: " + e.getMessage());
        }
        return supabaseDate;
    }
}
