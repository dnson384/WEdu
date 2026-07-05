package com.fckedu.exam_creation.importer.controller;

import com.fckedu.exam_creation.importer.service.ImporterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/importer")
public class ImporterController {
    private final ImporterService importerService;

    public ImporterController(ImporterService importerService) {
        this.importerService = importerService;
    }

    @PostMapping("/parse")
    public ResponseEntity<?> parseDoc(
            @RequestParam("file") MultipartFile file,
            @RequestParam("subject") String subject) {
        try {
            byte[] fileBytes = file.getBytes();

            boolean result = importerService.execute(fileBytes, subject);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Đã xảy ra lỗi trong quá trình xử lý file: " + e.getMessage());
        }
    }
}
