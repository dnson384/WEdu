package com.wedu.exam_creation.user.controller;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.user.dto.request.UpdateAvatarRequestDTO;
import com.wedu.exam_creation.user.dto.request.UpdateUserRequestDTO;
import com.wedu.exam_creation.user.usecase.UserService;
import com.wedu.exam_creation.user.usecase.UserUsecase;
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

    @PutMapping("/update-avatar")
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
