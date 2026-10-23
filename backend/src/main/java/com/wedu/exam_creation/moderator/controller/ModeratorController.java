package com.wedu.exam_creation.moderator.controller;

import com.wedu.exam_creation.moderator.usecase.ModeratorUsecase;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/moderator")
@PreAuthorize("hasRole('MODERATOR')")
public class ModeratorController {
    private final ModeratorUsecase moderatorUsecase;

    public ModeratorController(ModeratorUsecase moderatorUsecase) {
        this.moderatorUsecase = moderatorUsecase;
    }

    @PostMapping("/parse")
    public ResponseEntity<List<String>> parseDocument(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("subject") String subject) throws Exception {
        return ResponseEntity.ok(moderatorUsecase.parseDocument(principal.getUser(), file, subject));
    }
}
