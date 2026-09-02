package com.wedu.exam_creation.draft.controller;

import com.wedu.exam_creation.common.dto.draft.response.DraftDTO;
import com.wedu.exam_creation.draft.dto.request.CreateDraftDTO;
import com.wedu.exam_creation.draft.dto.request.UpdateChaptersDraftDTO;
import com.wedu.exam_creation.draft.dto.request.UpdateLessonsDraftDTO;
import com.wedu.exam_creation.draft.usecase.DraftUsecase;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/draft")
public class DraftController {
    private final DraftUsecase draftUsecase;

    public DraftController(DraftUsecase draftUsecase) {
        this.draftUsecase = draftUsecase;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createDraft(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody CreateDraftDTO payload
    ) {
        return ResponseEntity.ok(draftUsecase.createDraft(payload, principal.getUser().getId()));
    }

    @GetMapping("/{draftId}")
    public ResponseEntity<DraftDTO> getDraft(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String draftId
    ) {
        return ResponseEntity.ok(draftUsecase.getDraft(draftId, principal.getUser().getId()));
    }

    @PutMapping("/chapter")
    public ResponseEntity<Boolean> updateChapters(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody UpdateChaptersDraftDTO payload
    ) {
        return ResponseEntity.ok(draftUsecase.updateChapters(payload, principal.getUser().getId()));
    }

    @PutMapping("/lesson")
    public ResponseEntity<Boolean> updateLessons(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody UpdateLessonsDraftDTO payload
    ) {
        return ResponseEntity.ok(draftUsecase.updateLessons(payload, principal.getUser().getId()));
    }

    @PutMapping("/generate-matrix")
    public ResponseEntity<Boolean> generateMatrix(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam String draftId) {
        return ResponseEntity.ok(draftUsecase.generateMatrix(draftId, principal.getUser().getId()));
    }

    @PutMapping("/generate-matrix-details")
    public ResponseEntity<Boolean> generateMatrixDetails(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam String draftId) {
        return ResponseEntity.ok(draftUsecase.generateMatrixDetails(
                draftId, principal.getUser().getId()
        ));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<DraftDTO>> getRecentDraft(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(draftUsecase.getRecentDraft(principal.getUser().getId()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<DraftDTO>> getAllUserDrafts(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(draftUsecase.getAllUserDrafts(principal.getUser().getId()));
    }

    @DeleteMapping("/delete/{draftId}")
    public ResponseEntity<Boolean> deleteExam(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String draftId
    ) {
        return ResponseEntity.ok(draftUsecase.deleteDraft(principal.getUser().getId(), draftId));
    }
}
