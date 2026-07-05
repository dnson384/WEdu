package com.fckedu.exam_creation.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CookieDataDTO {
    private String accessToken;
    private String refreshToken;
}
