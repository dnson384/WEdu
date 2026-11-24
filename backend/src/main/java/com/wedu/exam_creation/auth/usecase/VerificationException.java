package com.wedu.exam_creation.auth.usecase;

import lombok.Getter;

@Getter
public class VerificationException extends RuntimeException {
    private final String code;
    private final int status;
    private final long retryAfter;

    public VerificationException(String code, String message, int status) {
        this(code, message, status, 0);
    }

    public VerificationException(String code, String message, int status, long retryAfter) {
        super(message);
        this.code = code;
        this.status = status;
        this.retryAfter = retryAfter;
    }
}
