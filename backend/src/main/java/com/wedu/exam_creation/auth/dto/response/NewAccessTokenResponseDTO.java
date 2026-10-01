package com.wedu.exam_creation.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewAccessTokenResponseDTO {
    private String accessToken;
}
