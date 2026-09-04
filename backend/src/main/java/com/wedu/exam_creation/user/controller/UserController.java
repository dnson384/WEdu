package com.wedu.exam_creation.user.controller;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.user.dto.request.UpdateAvatarRequestDTO;
import com.wedu.exam_creation.user.dto.request.UpdateUserRequestDTO;
import com.wedu.exam_creation.user.usecase.UserUsecase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserUsecase userUsecase;

    public UserController(UserUsecase userUsecase) {
        this.userUsecase = userUsecase;
    }

    @GetMapping("/me")
    public ResponseEntity<CommonUserResponseDTO> getMe(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(userUsecase.getMe(principal.getUser()));
    }

    @PutMapping("/update-avatar")
    public ResponseEntity<Boolean> updateAvatar(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody UpdateAvatarRequestDTO payload) {
        return ResponseEntity.ok(userUsecase.updateAvatar(principal.getUser().getId(), payload.getS3Key()));
    }

    @PatchMapping("/update-username")
    public ResponseEntity<CommonUserResponseDTO> updateUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody UpdateUserRequestDTO payload) {
        return ResponseEntity.ok(userUsecase.updateUsername(principal.getUser().getId(), payload.getUsername()));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Boolean> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(userUsecase.deleteAccount(principal.getUser().getId()));
    }
}
