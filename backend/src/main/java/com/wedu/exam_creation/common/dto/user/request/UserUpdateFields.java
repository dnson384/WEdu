package com.wedu.exam_creation.common.dto.user.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateFields {
    private String s3Key;
    private String username;
    private Boolean isActive;
}
