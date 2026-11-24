package com.wedu.exam_creation.auth.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerificationResponseDTO {
    private String email;
    private Instant expiresAt;
    private Instant otpExpiresAt;
    private Instant resendAt;
    private int remainingAttempts;
    private Instant serverTime;
}
