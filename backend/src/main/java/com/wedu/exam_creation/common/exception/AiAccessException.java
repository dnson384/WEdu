package com.wedu.exam_creation.common.exception;

import lombok.Getter;

@Getter
public class AiAccessException extends RuntimeException {
    private final String code;
    private final int status;

    public AiAccessException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
