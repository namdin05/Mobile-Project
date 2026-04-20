package com.melodix.app.Utils;

import android.util.Log;

public class DateHelper {
    public static String formatSupabaseDate(String supabaseDate) {
        if (supabaseDate == null || supabaseDate.isEmpty()) return "";

        try {
            // Cắt lấy phần cốt lõi 19 ký tự (nếu chuỗi dài hơn 19)
            String mainPart = supabaseDate;
            if (supabaseDate.length() >= 19) {
                mainPart = supabaseDate.substring(0, 19);
            }

            // ==========================================
            // DÒNG CODE "PHÉP THUẬT": CHUẨN HÓA CHUỖI
            // Nếu có chữ 'T', biến nó thành dấu cách. Nếu không có, giữ nguyên.
            // ==========================================
            mainPart = mainPart.replace("T", " ");

            // 1. Bây giờ chuỗi chắc chắn luôn có dạng "yyyy-MM-dd HH:mm:ss"
            // Nên ta chỉ cần dùng một format duy nhất này thôi:
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = inputFormat.parse(mainPart);

            // 2. Format lại thành kiểu Việt Nam và theo giờ trên điện thoại
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            outputFormat.setTimeZone(java.util.TimeZone.getDefault());

            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            Log.e("DateUtils", "Lỗi parse ngày tháng: " + e.getMessage());
        }

        // Nếu vẫn lỗi, in ra chuỗi gốc (tránh crash)
        return supabaseDate;
    }
}
