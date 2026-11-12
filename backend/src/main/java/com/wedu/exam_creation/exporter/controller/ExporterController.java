package com.wedu.exam_creation.exporter.controller;

import com.wedu.exam_creation.exporter.dto.request.ExportRequestDTO;
import com.wedu.exam_creation.exporter.dto.request.ExportMatrixRequestDTO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import com.wedu.exam_creation.exporter.usecase.ExporterUsecase;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/exporter")
public class ExporterController {
    private final ExporterUsecase exporterUsecase;

    public ExporterController(ExporterUsecase exporterUsecase) {
        this.exporterUsecase = exporterUsecase;
    }

    @PostMapping("/exam")
    public ResponseEntity<Resource> exportExam(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ExportRequestDTO payload
    ) {
        byte[] buffer = exporterUsecase.exportExam(principal.getUser().getId(), payload.getExamId());

        ByteArrayResource resource = new ByteArrayResource(buffer);

        String fileName = URLEncoder.encode(payload.getExamName(), StandardCharsets.UTF_8) + ".docx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(buffer.length)
                .body(resource);
    }

    @PostMapping("/matrix")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public ResponseEntity<Resource> exportMatrix(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ExportMatrixRequestDTO payload) {
        byte[] buffer = exporterUsecase.exportMatrix(principal.getUser().getId(), payload.getExamId());
        String fileName = URLEncoder.encode(payload.getExamName() + "_ma trận đề.docx", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(buffer.length)
                .body(new ByteArrayResource(buffer));
    }
}
