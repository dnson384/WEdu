package com.wedu.exam_creation.auth.controller;

import com.wedu.exam_creation.auth.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.auth.usecase.AuthUsecase;
import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.refreshToken.dto.response.NewAccessTokenResponseDTO;
import com.wedu.exam_creation.user.dto.request.ChangePasswordRequestDTO;
import com.wedu.exam_creation.user.dto.request.LoginUserRequestDTO;
import com.wedu.exam_creation.user.usecase.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    private final AuthUsecase authUsecase;

    public AuthController(UserService userService, AuthUsecase authUsecase) {
        this.userService = userService;
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
            @CookieValue(value = "accessToken") String accessToken,
            @CookieValue(value = "refreshToken") String refreshToken) {
        return ResponseEntity.ok(authUsecase.logout(accessToken, refreshToken));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Boolean> changePassword(
            @RequestHeader("Authorization") String authorization,
            @CookieValue(value = "refreshToken") String refreshToken,
            @RequestBody ChangePasswordRequestDTO reqPayload
    ) {
        String accessToken = checkAuth(authorization);
        CommonUserResponseDTO user = userService.getMe(accessToken, refreshToken);

        return ResponseEntity.ok(authUsecase.changePassword(user.getId(), reqPayload));
    }

    @PostMapping("/lock")
    public ResponseEntity<Boolean> lockAccount(
            @RequestHeader("Authorization") String authorization,
            @CookieValue(value = "refreshToken") String refreshToken
    ) {
        String accessToken = checkAuth(authorization);
        CommonUserResponseDTO user = userService.getMe(accessToken, refreshToken);

        return ResponseEntity.ok(authUsecase.lockAccount(user.getId(), refreshToken));
    }

    @PostMapping("/regenerate-access-token")
    public ResponseEntity<NewAccessTokenResponseDTO> regenerateAccessToken(@CookieValue(value = "refreshToken") String refreshToken) {
        String newAT = authUsecase.regenerateAccessToken(refreshToken);
        return ResponseEntity.ok(new NewAccessTokenResponseDTO(newAT));
    }

    private String checkAuth(String authorization) {
        if (authorization == null || authorization.trim().isEmpty()) {
            throw new BadRequestException("Authorization rỗng");
        }

        if (!authorization.startsWith("Bearer ")) {
            throw new BadRequestException("Authorization sai định dạng Bearer");
        }

        String accessToken = authorization.substring(7);

        if (accessToken.trim().isEmpty()) {
            throw new BadRequestException("AT rỗng");
        }

        return accessToken;
    }
}
