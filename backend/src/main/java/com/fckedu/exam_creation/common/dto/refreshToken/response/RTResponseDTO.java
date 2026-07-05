package com.fckedu.exam_creation.common.dto.refreshToken.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RTResponseDTO {
    private String id;
    private String jti;
    private String userId;
    private LocalDateTime expiresAt;
    private LocalDateTime issuedAt;
}
