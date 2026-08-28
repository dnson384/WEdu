package com.wedu.exam_creation.admin.controller;

import com.wedu.exam_creation.admin.usecase.AdminUsecase;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminUsecase adminUsecase;

    public AdminController(AdminUsecase adminUsecase) {
        this.adminUsecase = adminUsecase;
    }

    @GetMapping("/all-user")
    public ResponseEntity<List<CommonUserResponseDTO>> getAllUsers(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        List<CommonUserResponseDTO> users = adminUsecase.getAllUsers(principal.getUser());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    public ResponseEntity<List<CommonUserResponseDTO>> findUsers(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(name = "keyword") String keyword
    ) {
        List<CommonUserResponseDTO> result = adminUsecase.findUsers(principal.getUser(), keyword);
        return ResponseEntity.ok(result);
    }
}
