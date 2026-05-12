package com.melodix.app.Utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class StringUtils {

    public static String generateSlug(String input) {
        if (input == null || input.isEmpty()) return "unknown_song";

        
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccents = pattern.matcher(normalized).replaceAll("");

        
        noAccents = noAccents.replace("đ", "d").replace("Đ", "D");

        
        return noAccents.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }
}