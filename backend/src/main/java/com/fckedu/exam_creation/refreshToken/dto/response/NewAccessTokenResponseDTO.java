package com.fckedu.exam_creation.refreshToken.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewAccessTokenResponseDTO {
    private String accessToken;
}
