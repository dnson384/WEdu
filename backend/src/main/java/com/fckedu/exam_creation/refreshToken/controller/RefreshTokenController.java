package com.fckedu.exam_creation.refreshToken.controller;

import com.fckedu.exam_creation.refreshToken.dto.response.NewAccessTokenResponseDTO;
import com.fckedu.exam_creation.refreshToken.usecase.RefreshTokenUsecase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/refresh-token")
public class RefreshTokenController {
    private final RefreshTokenUsecase usecase;

    public RefreshTokenController(RefreshTokenUsecase usecase) {
        this.usecase = usecase;
    }

    @PostMapping("generate-access-token")
    public ResponseEntity<NewAccessTokenResponseDTO> generateAccessToken(@CookieValue(value = "refreshToken") String refreshToken) {
        String newAT = usecase.generateAccessToken(refreshToken);
        return ResponseEntity.ok(new NewAccessTokenResponseDTO(newAT));
    }
}
