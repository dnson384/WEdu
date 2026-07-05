package com.fckedu.exam_creation.common.dto.user.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
