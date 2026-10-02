package com.wedu.exam_creation.passwordResetToken.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetTokenEntity {
    private String id;
    private String tokenHash;
    private String userId;
    private Instant expiryDate;
    private boolean used;
    private Instant createdAt;
}
