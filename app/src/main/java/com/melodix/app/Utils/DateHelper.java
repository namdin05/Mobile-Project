package com.melodix.app.Utils;

import android.util.Log;

public class DateHelper {

    public static String formatSupabaseDate(String supabaseDate) {
        try {
            // Supabase trả về dạng: "2026-04-13 17:45:00.166126+00"
            // Mình chỉ cắt lấy phần cốt lõi "2026-04-13 17:45:00" để dễ xử lý
            String mainPart = supabaseDate;
            if (supabaseDate.length() >= 19) {
                mainPart = supabaseDate.substring(0, 19);
            }

            // 1. Đọc chuỗi thời gian (Biết rằng nó đang ở múi giờ UTC)
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = inputFormat.parse(mainPart);

            // 2. Format lại thành kiểu Việt Nam và theo giờ trên điện thoại người dùng
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            outputFormat.setTimeZone(java.util.TimeZone.getDefault());

            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            Log.e("AdminStat", "Lỗi parse ngày tháng: " + e.getMessage());
        }
        // Nếu lỗi không parse được thì in ra chuỗi gốc cho an toàn
        return supabaseDate;
    }
}
