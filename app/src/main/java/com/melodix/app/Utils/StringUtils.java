package com.melodix.app.Utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class StringUtils {

    // BẮT BUỘC PHẢI CÓ PUBLIC STATIC Ở ĐÂY 👇
    public static String generateSlug(String input) {
        if (input == null || input.isEmpty()) return "unknown_song";

        // 1. Tách dấu ra khỏi chữ cái
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccents = pattern.matcher(normalized).replaceAll("");

        // 2. Xử lý riêng chữ Đ (vì Normalizer không xử lý được chữ này)
        noAccents = noAccents.replace("đ", "d").replace("Đ", "D");

        // 3. Xóa BỎ TOÀN BỘ khoảng trắng và ký tự đặc biệt, sau đó chuyển thành chữ thường
        return noAccents.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }
}