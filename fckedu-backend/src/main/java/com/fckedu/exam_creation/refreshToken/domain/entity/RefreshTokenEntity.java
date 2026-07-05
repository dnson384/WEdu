package com.fckedu.exam_creation.refreshToken.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity {
    private String id;
    private String jti;
    private String userId;
    private LocalDateTime expiresAt;
    private LocalDateTime issuedAt;
}
