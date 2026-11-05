package com.wedu.exam_creation.user.domain.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class PlusEntitlement {
    private PlusEntitlement() { }

    public static String effectiveType(String accountType, Instant expiresAt, Instant now) {
        return "PLUS".equals(accountType) && expiresAt != null && !expiresAt.isAfter(now)
                ? "FREE" : accountType;
    }

    public static Instant extend(Instant expiresAt, Instant now, int days) {
        return (expiresAt != null && expiresAt.isAfter(now) ? expiresAt : now).plus(days, ChronoUnit.DAYS);
    }
}
