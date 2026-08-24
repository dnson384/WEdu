package com.wedu.exam_creation.user.controller;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.user.dto.request.*;
import com.wedu.exam_creation.user.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.user.dto.response.UserResponseDTO;
import com.wedu.exam_creation.user.usecase.UserService;
import com.wedu.exam_creation.user.usecase.UserUsecase;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserUsecase userUsecase;
    private final UserService userService;

    public UserController(UserUsecase userUsecase, UserService userService) {
        this.userUsecase = userUsecase;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(
            @Valid @RequestBody NewUserRequestDTO newUser,
            HttpServletResponse response) {
        AuthorizedResponseDTO dto = userUsecase.register(newUser);

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
        AuthorizedResponseDTO dto = userUsecase.login(payload);

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
        return ResponseEntity.ok(userUsecase.logout(accessToken, refreshToken));
    }

    @GetMapping("/me")
    public ResponseEntity<CommonUserResponseDTO> getMe(
            @RequestHeader("Authorization") String authorization,
            @CookieValue(value = "refreshToken") String refreshToken
    ) {
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

        return ResponseEntity.ok(userService.getMe(accessToken, refreshToken));
    }

    @PutMapping("/avatar")
    public ResponseEntity<Boolean> updateAvatar(
            @RequestHeader("Authorization") String authorization,
            @CookieValue(value = "refreshToken") String refreshToken,
            @RequestBody UpdateAvatarRequestDTO payload) {
        String accessToken = checkAuth(authorization);
        CommonUserResponseDTO user = userService.getMe(accessToken, refreshToken);

        return ResponseEntity.ok(userUsecase.updateAvatar(user.getId(), payload.getS3Key()));
    }

    @PutMapping("/update-user")
    public ResponseEntity<Boolean> updateUser(
            @RequestHeader("Authorization") String authorization,
            @CookieValue(value = "refreshToken") String refreshToken,
            @RequestBody UpdateUserRequestDTO payload) {
        String accessToken = checkAuth(authorization);
        CommonUserResponseDTO user = userService.getMe(accessToken, refreshToken);

        return ResponseEntity.ok(userUsecase.updateUser(user.getId(), payload.getUsername()));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Boolean> changePassword(
            @RequestHeader("Authorization") String authorization,
            @CookieValue(value = "refreshToken") String refreshToken,
            @RequestBody ChangePasswordRequestDTO reqPayload
    ) {
        String accessToken = checkAuth(authorization);
        CommonUserResponseDTO user = userService.getMe(accessToken, refreshToken);

        return ResponseEntity.ok(userUsecase.changePassword(user.getId(), reqPayload));
    }

    @PostMapping("/lock")
    public ResponseEntity<Boolean> lockAccount(
            @RequestHeader("Authorization") String authorization,
            @CookieValue(value = "refreshToken") String refreshToken
    ) {
        String accessToken = checkAuth(authorization);
        CommonUserResponseDTO user = userService.getMe(accessToken, refreshToken);

        return ResponseEntity.ok(userUsecase.lockAccount(user.getId(), refreshToken));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Boolean> deleteAccount(
            @CookieValue(value = "accessToken") String accessToken,
            @CookieValue(value = "refreshToken") String refreshToken
    ) {
        CommonUserResponseDTO user = userService.getMe(accessToken, refreshToken);

        return ResponseEntity.ok(userUsecase.deleteAccount(user.getId()));
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
