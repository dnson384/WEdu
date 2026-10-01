package com.wedu.exam_creation.refreshToken.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity {
    private String id;
    private String jti;
    private String userId;
    private Instant expiresAt;
    private Instant issuedAt;
}
