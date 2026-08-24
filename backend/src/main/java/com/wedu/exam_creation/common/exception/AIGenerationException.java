package com.wedu.exam_creation.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class AIGenerationException extends RuntimeException {
    public AIGenerationException(String message) {
        super(message);
    }
}
