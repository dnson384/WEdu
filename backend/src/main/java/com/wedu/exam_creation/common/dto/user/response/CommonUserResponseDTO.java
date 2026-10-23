package com.wedu.exam_creation.common.dto.user.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonUserResponseDTO {
    private String id;
    private String email;
    private String username;
    private String role;
    private String avatarUrl;
    private Boolean isActive;
    private String accountType;
    private Instant createdAt;
    private Instant updatedAt;
}
