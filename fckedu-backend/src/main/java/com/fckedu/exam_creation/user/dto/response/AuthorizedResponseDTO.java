package com.fckedu.exam_creation.user.dto.response;

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
