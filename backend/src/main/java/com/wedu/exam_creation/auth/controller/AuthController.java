package com.wedu.exam_creation.auth.controller;

import com.wedu.exam_creation.auth.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.auth.usecase.AuthUsecase;
import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.refreshToken.dto.response.NewAccessTokenResponseDTO;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.user.dto.request.ChangePasswordRequestDTO;
import com.wedu.exam_creation.user.dto.request.LoginUserRequestDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthUsecase authUsecase;

    public AuthController(AuthUsecase authUsecase) {
        this.authUsecase = authUsecase;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(
            @Valid @RequestBody NewUserRequestDTO newUser,
            HttpServletResponse response) {
        AuthorizedResponseDTO dto = authUsecase.register(newUser);

        Cookie accessToken = new Cookie(
                "accessToken",
                dto.getAccessToken()
        );
        accessToken.setHttpOnly(true);
        accessToken.setMaxAge(15 * 60);
        accessToken.setPath("/");
        // accessToken.setSecure(true);

        response.addCookie(accessToken);

        Cookie refreshToken = new Cookie(
                "refreshToken",
                dto.getRefreshToken()
        );
        refreshToken.setHttpOnly(true);
        refreshToken.setMaxAge(7 * 24 * 60 * 60);
        refreshToken.setPath("/");
        // refreshToken.setSecure(true);

        response.addCookie(refreshToken);

        return ResponseEntity.ok(dto.getUser());
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDTO> login(
            @Valid @RequestBody LoginUserRequestDTO payload,
            HttpServletResponse response) {
        AuthorizedResponseDTO dto = authUsecase.login(payload);

        Cookie accessToken = new Cookie(
                "accessToken",
                dto.getAccessToken()
        );
        accessToken.setHttpOnly(true);
        accessToken.setMaxAge(15 * 60);
        accessToken.setPath("/");
        // accessToken.setSecure(true);

        response.addCookie(accessToken);

        Cookie refreshToken = new Cookie(
                "refreshToken",
                dto.getRefreshToken()
        );
        refreshToken.setHttpOnly(true);
        refreshToken.setMaxAge(7 * 24 * 60 * 60);
        refreshToken.setPath("/");
        // refreshToken.setSecure(true);

        response.addCookie(refreshToken);

        return ResponseEntity.ok(dto.getUser());
    }

    @PostMapping("/logout")
    public ResponseEntity<Boolean> logout(
            @RequestHeader("Authorization") String authorization
    ) {
        return ResponseEntity.ok(authUsecase.logout(authorization));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Boolean> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ChangePasswordRequestDTO reqPayload
    ) {
        return ResponseEntity.ok(authUsecase.changePassword(principal.getUser().getId(), reqPayload));
    }

    @PostMapping("/lock")
    public ResponseEntity<Boolean> lockAccount(
            @AuthenticationPrincipal CustomUserDetails principal,
            @CookieValue(value = "refreshToken") String refreshToken
    ) {
        return ResponseEntity.ok(authUsecase.lockAccount(principal.getUser().getId(), refreshToken));
    }

    @PostMapping("/regenerate-access-token")
    public ResponseEntity<NewAccessTokenResponseDTO> regenerateAccessToken(
            @CookieValue(value = "refreshToken") String refreshToken
    ) {
        String newAT = authUsecase.regenerateAccessToken(refreshToken);
        return ResponseEntity.ok(new NewAccessTokenResponseDTO(newAT));
    }
}
