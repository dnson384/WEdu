package com.wedu.exam_creation.auth.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Legacy user fields needed for migration. The opaque ID preserves its original stored type. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class AuthMigrationUserEntity {
    private Object id;
    private String email;
    private String googleSubject;
    private String loginMethod;
    private String hashedPassword;
}
