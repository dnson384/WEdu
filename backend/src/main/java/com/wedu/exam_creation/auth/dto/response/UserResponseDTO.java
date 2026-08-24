package com.wedu.exam_creation.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private String id;
    private String email;
    private String username;
    private String role;
    private String avatarUrl;
}
