package com.wedu.exam_creation.user.controller;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
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

    @PatchMapping("/update-avatar")
    public ResponseEntity<Boolean> updateAvatar(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("s3Key") String s3Key) {
        return ResponseEntity.ok(userUsecase.updateAvatar(principal.getUser().getId(), s3Key));
    }

    @PatchMapping("/update-username")
    public ResponseEntity<Boolean> updateUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("username") String username) {
        return ResponseEntity.ok(userUsecase.updateUsername(principal.getUser().getId(), username));
    }
}
