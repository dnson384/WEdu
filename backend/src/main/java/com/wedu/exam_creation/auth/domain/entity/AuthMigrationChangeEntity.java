package com.wedu.exam_creation.auth.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class AuthMigrationChangeEntity {
    private Object id;
    private String email;
    // Null preserves the existing method. Only verified LOCAL links are upgraded to MIXED.
    private String loginMethod;
}
