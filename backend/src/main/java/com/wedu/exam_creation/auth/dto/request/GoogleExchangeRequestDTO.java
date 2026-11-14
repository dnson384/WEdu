package com.wedu.exam_creation.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GoogleExchangeRequestDTO {
    @NotBlank
    @Pattern(regexp = "^[a-f0-9]{64}$")
    private String code;

    @NotBlank
    @Pattern(regexp = "^[a-f0-9]{64}$")
    private String verifier;
}
