package com.sep.treksphere.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PhoneNumberUtil {

    private PhoneNumberUtil() {
    }

    /**
     * Normalizes a phone number to standard 10-digit Vietnamese format starting with 0 (e.g. 0837319199).
     * Removes spaces, dots, dashes, parentheses, and converts +84 or leading 84 prefix to 0.
     */
    public static String normalize(String phone) {
        if (phone == null) {
            return null;
        }
        String cleaned = phone.trim().replaceAll("[\\s.\\-\\(\\)]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3);
        } else if (cleaned.startsWith("84") && cleaned.length() == 11) {
            cleaned = "0" + cleaned.substring(2);
        } else if (cleaned.startsWith("0084")) {
            cleaned = "0" + cleaned.substring(4);
        }
        return cleaned;
    }

    /**
     * Returns all potential representation variants of a phone number (e.g., 0837319199, +84837319199, 84837319199)
     * to safely detect duplicates against existing records in the database.
     */
    public static List<String> getPhoneVariants(String phone) {
        String normalized = normalize(phone);
        if (normalized == null || normalized.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> variants = new ArrayList<>();
        variants.add(normalized);
        if (normalized.startsWith("0") && normalized.length() == 10) {
            String suffix = normalized.substring(1);
            variants.add("+84" + suffix);
            variants.add("84" + suffix);
        }
        return variants;
    }
}
