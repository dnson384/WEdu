package com.fckedu.exam_creation.user.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    private String id;
    private String email;
    private String hashedPassword;
    private String username;
    private String role;
    private String loginMethod;
    private String avatarUrl;
    private Boolean isActive;
    private String accountType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
