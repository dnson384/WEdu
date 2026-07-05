package com.fckedu.exam_creation.refreshToken.controller;

import com.fckedu.exam_creation.refreshToken.dto.request.ReGenATDTO;
import com.fckedu.exam_creation.refreshToken.dto.response.NewAccessTokenResponseDTO;
import com.fckedu.exam_creation.refreshToken.usecase.RefreshTokenUsecase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<NewAccessTokenResponseDTO> generateAccessToken(@RequestBody ReGenATDTO payload) {
        String newAT = usecase.generateAccessToken(payload.getRefreshToken());
        return ResponseEntity.ok(new NewAccessTokenResponseDTO(newAT));
    }
}
