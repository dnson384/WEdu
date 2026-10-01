package com.wedu.exam_creation.admin.controller;

import com.wedu.exam_creation.admin.usecase.AdminUsecase;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminUsecase adminUsecase;

    public AdminController(AdminUsecase adminUsecase) {
        this.adminUsecase = adminUsecase;
    }

    @GetMapping("/users")
    public ResponseEntity<List<CommonUserResponseDTO>> getAllUsers(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(adminUsecase.getAllUsers(principal.getUser()));
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<CommonUserResponseDTO>> findUsers(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(name = "keyword") String keyword
    ) {
        return ResponseEntity.ok(adminUsecase.findUsers(principal.getUser(), keyword));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<CommonUserResponseDTO> updateUserRole(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("userId") String userId,
            @RequestParam("role") String role
    ) {
        return ResponseEntity.ok(adminUsecase.updateUserRole(principal.getUser(), userId, role));
    }

    @PutMapping("/{userId}/lock")
    public ResponseEntity<CommonUserResponseDTO> lockUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("userId") String userId
    ) {
        return ResponseEntity.ok(adminUsecase.lockUser(principal.getUser(), userId));
    }

    @PutMapping("/{userId}/unlock")
    public ResponseEntity<CommonUserResponseDTO> unLockUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("userId") String userId
    ) {
        return ResponseEntity.ok(adminUsecase.unlockUser(principal.getUser(), userId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Boolean> deleteUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String userId
    ) {
        return ResponseEntity.ok(adminUsecase.deleteUser(principal.getUser(), userId));
    }
}
