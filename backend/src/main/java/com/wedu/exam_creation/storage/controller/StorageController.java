package com.wedu.exam_creation.storage.controller;

import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.storage.service.S3Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/storage")
public class StorageController {
    private final S3Service s3Service;

    public StorageController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/avatar")
    public ResponseEntity<String> uploadAvatar(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            return ResponseEntity.ok(s3Service.uploadImageFile(file, "avatars", 5));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload avatar thất bại");
        }
    }

    @PostMapping("/question-file")
    public ResponseEntity<String> uploadDocumentToQuestionBank(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("subject") String subject
    ) {
        try {
            return ResponseEntity.ok(s3Service.updateDocumentFile(
                    file, "question-files/" + subject, 50, principal.getUser().getEmail()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload document thất bại");
        }
    }
}
