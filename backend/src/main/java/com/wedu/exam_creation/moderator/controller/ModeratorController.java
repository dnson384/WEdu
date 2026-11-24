package com.wedu.exam_creation.moderator.controller;

import com.wedu.exam_creation.common.dto.question.response.PendingQuestionPageDTO;
import com.wedu.exam_creation.moderator.dto.ApproveQuestionFileResponseDTO;
import com.wedu.exam_creation.moderator.dto.EditPendingQuestionDTO;
import com.wedu.exam_creation.moderator.dto.RejectPendingQuestionResponseDTO;
import com.wedu.exam_creation.moderator.dto.RejectQuestionFileDTO;
import com.wedu.exam_creation.moderator.dto.ReviewPendingQuestionDTO;
import com.wedu.exam_creation.moderator.usecase.EditPendingQuestionUsecase;
import com.wedu.exam_creation.moderator.usecase.ModeratorUsecase;
import com.wedu.exam_creation.moderator.usecase.ReviewPendingQuestionUsecase;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.storage.dto.response.S3ObjectInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/moderator")
@PreAuthorize("hasRole('MODERATOR')")
public class ModeratorController {
    private final ModeratorUsecase moderatorUsecase;
    private final EditPendingQuestionUsecase editPendingQuestionUsecase;
    private final ReviewPendingQuestionUsecase reviewPendingQuestionUsecase;

    public ModeratorController(ModeratorUsecase moderatorUsecase, EditPendingQuestionUsecase editPendingQuestionUsecase,
            ReviewPendingQuestionUsecase reviewPendingQuestionUsecase) {
        this.moderatorUsecase = moderatorUsecase;
        this.editPendingQuestionUsecase = editPendingQuestionUsecase;
        this.reviewPendingQuestionUsecase = reviewPendingQuestionUsecase;
    }

    @PostMapping("/questions/{id}/approve")
    public ResponseEntity<Void> approvePendingQuestion(@PathVariable("id") String id,
            @RequestBody ReviewPendingQuestionDTO input) {
        reviewPendingQuestionUsecase.approve(id, input);
        return ResponseEntity.noContent().header("Cache-Control", "private, no-store").build();
    }

    @PostMapping("/questions/{id}/reject")
    public ResponseEntity<RejectPendingQuestionResponseDTO> rejectPendingQuestion(
            @PathVariable("id") String id, @RequestBody ReviewPendingQuestionDTO input) {
        return ResponseEntity.ok().header("Cache-Control", "private, no-store")
                .body(reviewPendingQuestionUsecase.reject(id, input));
    }

    @PatchMapping(value = "/questions/{id}", consumes = "multipart/form-data")
    public ResponseEntity<Void> editPendingQuestion(@PathVariable("id") String id,
            @RequestPart("payload") EditPendingQuestionDTO payload,
            @RequestParam MultiValueMap<String, MultipartFile> files) {
        editPendingQuestionUsecase.execute(id, payload, files);
        return ResponseEntity.noContent().header("Cache-Control", "private, no-store").build();
    }

    @GetMapping("/question-files")
    public ResponseEntity<List<S3ObjectInfo>> listQuestionFiles() {
        return ResponseEntity.ok(moderatorUsecase.listQuestionFiles());
    }

    @GetMapping("/questions/pending")
    public ResponseEntity<PendingQuestionPageDTO> listPendingQuestions(
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "search", defaultValue = "") String search,
            @RequestParam(name = "questionType", defaultValue = "") String questionType) {
        return ResponseEntity.ok().header("Cache-Control", "private, no-store")
                .body(moderatorUsecase.listPendingQuestions(page == null ? "0" : page, search, questionType));
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
