package com.wedu.exam_creation.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequestDTO {
    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$")
    private String otp;
}
