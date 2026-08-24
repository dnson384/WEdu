package com.wedu.exam_creation.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizedResponseDTO {
    private UserResponseDTO user;
    private String accessToken;
    private String refreshToken;
}
