package com.wedu.exam_creation.moderator.controller;

import com.wedu.exam_creation.moderator.usecase.ModeratorUsecase;
import com.wedu.exam_creation.moderator.dto.ApproveQuestionFileResponseDTO;
import com.wedu.exam_creation.moderator.dto.RejectQuestionFileDTO;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.storage.dto.response.S3ObjectInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/question-files")
    public ResponseEntity<List<S3ObjectInfo>> listQuestionFiles() {
        return ResponseEntity.ok(moderatorUsecase.listQuestionFiles());
    }

    @DeleteMapping("/question-files")
    public ResponseEntity<Void> deleteQuestionFile(@RequestParam("key") String key) {
        moderatorUsecase.deleteQuestionFile(key);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/question-files/content")
    public ResponseEntity<byte[]> getQuestionFileContent(@RequestParam("key") String key) {
        return ResponseEntity.ok()
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(moderatorUsecase.getQuestionFileContent(key));
    }

    @PostMapping("/parse")
    public ResponseEntity<List<String>> parseDocument(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("subject") String subject) throws Exception {
        return ResponseEntity.ok(moderatorUsecase.parseDocument(principal.getUser(), file, subject));
    }

    @PostMapping(value = "/question-files/approve", consumes = "multipart/form-data")
    public ResponseEntity<ApproveQuestionFileResponseDTO> approveQuestionFile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("key") String key,
            @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {
        return ResponseEntity.ok().header("Cache-Control", "private, no-store")
                .body(moderatorUsecase.approveQuestionFile(principal.getUser(), key, file));
    }

    @PostMapping("/question-files/reject")
    public ResponseEntity<Void> rejectQuestionFile(@RequestBody RejectQuestionFileDTO request) {
        moderatorUsecase.rejectQuestionFile(request);
        return ResponseEntity.noContent().header("Cache-Control", "private, no-store").build();
    }
}
