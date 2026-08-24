package com.wedu.exam_creation.storage.controller;

import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.storage.service.S3Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authorization
    ) {
        String accessToken = checkAuth(authorization);

        if (accessToken.trim().isEmpty()) {
            throw new BadRequestException("AT rỗng");
        }

        try {
            String s3Key = s3Service.uploadFile(file, "avatars", accessToken);
            return ResponseEntity.ok(s3Key);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload avatar thất bại: " + e.getMessage());
        }
    }

    @PostMapping("/upload/document")
    public ResponseEntity<String> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authorization

    ) {
        String accessToken = checkAuth(authorization);

        try {
            String s3Key = s3Service.uploadFile(file, "documents", accessToken);
            return ResponseEntity.ok(s3Key);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload document thất bại: " + e.getMessage());
        }
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