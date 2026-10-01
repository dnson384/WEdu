package com.wedu.exam_creation.auth.controller;

import com.wedu.exam_creation.auth.dto.request.ChangePasswordRequestDTO;
import com.wedu.exam_creation.auth.dto.request.ResetPasswordRequestDTO;
import com.wedu.exam_creation.auth.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.auth.dto.response.NewAccessTokenResponseDTO;
import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.auth.usecase.AuthUsecase;
import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
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

    @PatchMapping("/change-password")
    public ResponseEntity<Boolean> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ChangePasswordRequestDTO reqPayload,
            @RequestHeader("Authorization") String authorization
    ) {
        String accessToken = authorization.substring(7).trim();
        return ResponseEntity.ok(authUsecase.changePassword(accessToken, principal.getUser().getId(), reqPayload));
    }

    @PostMapping("/regenerate-access-token")
    public ResponseEntity<NewAccessTokenResponseDTO> regenerateAccessToken(
            @CookieValue(value = "refreshToken") String refreshToken
    ) {
        String newAT = authUsecase.regenerateAccessToken(refreshToken);
        return ResponseEntity.ok(new NewAccessTokenResponseDTO(newAT));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @RequestParam("email") String email
    ) {
        authUsecase.forgotPassword(email);
        return ResponseEntity.ok("Đường dẫn đặt lại mật khẩu đã được gửi về email!");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        authUsecase.resetPassword(dto.getPlainToken(), dto.getNewPassword(), dto.getConfirmNewPassword());
        return ResponseEntity.ok("Đặt lại mật khẩu thành công");
    }
}
