package com.wedu.exam_creation.common.util;

import java.util.Locale;

public final class EmailNormalizer {
    private EmailNormalizer() {}

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isCanonical(String email) {
        return email != null && !email.isBlank() && email.equals(normalize(email));
    }
}
