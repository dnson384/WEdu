package com.wedu.exam_creation.importer.controller;

import com.wedu.exam_creation.importer.service.ImporterService;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/moderator")
public class ModeratorController {
    private final ImporterService importerService;

    public ModeratorController(ImporterService importerService) {
        this.importerService = importerService;
    }

    @PostMapping("/parse")
    public ResponseEntity<List<String>> parseDoc(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("subject") String subject) throws Exception {
        return ResponseEntity.ok(importerService.execute(principal.getUser(), file, subject));
    }
}
