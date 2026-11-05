package com.wedu.exam_creation.user.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
    private Instant createdAt;
    private Instant updatedAt;
    private Instant plusExpiresAt;

    public UserEntity(String id, String email, String hashedPassword, String username, String role, String loginMethod, String avatarUrl, Boolean isActive, String accountType, Instant createdAt, Instant updatedAt) {
        this(id, email, hashedPassword, username, role, loginMethod, avatarUrl, isActive, accountType, createdAt, updatedAt, null);
    }
}
