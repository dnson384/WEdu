package com.wedu.exam_creation.exporter.controller;

import com.wedu.exam_creation.exporter.dto.request.ExportRequestDTO;
import com.wedu.exam_creation.exporter.usecase.ExporterUsecase;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Resource> exportWord(@RequestBody ExportRequestDTO payload) {
        byte[] buffer = exporterUsecase.exportAsWord(payload.getExamId());

        ByteArrayResource resource = new ByteArrayResource(buffer);

        String fileName = URLEncoder.encode(payload.getExamName(), StandardCharsets.UTF_8) + ".docx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(buffer.length)
                .body(resource);
    }
}
