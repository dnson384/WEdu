package com.fckedu.exam_creation.draft.controller;

import com.fckedu.exam_creation.common.dto.draft.response.DraftDTO;
import com.fckedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.fckedu.exam_creation.draft.dto.request.CreateDraftDTO;
import com.fckedu.exam_creation.draft.dto.request.UpdateChaptersDraftDTO;
import com.fckedu.exam_creation.draft.dto.request.UpdateLessonsDraftDTO;
import com.fckedu.exam_creation.draft.usecase.DraftUsecase;
import com.fckedu.exam_creation.user.usecase.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/draft")
public class DraftController {
    private final DraftUsecase draftUsecase;
    private final UserService userService;

    public DraftController(DraftUsecase draftUsecase, UserService userService) {
        this.draftUsecase = draftUsecase;
        this.userService = userService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createDraft(@RequestHeader("Authorization") String authorization, @RequestBody CreateDraftDTO payload) {
        String accessToken = authorization.substring(7);
        CommonUserResponseDTO user = userService.getMe(accessToken);

        return ResponseEntity.ok(draftUsecase.createDraft(payload, user.getId()));
    }

    @GetMapping("/{draftId}")
    public ResponseEntity<DraftDTO> getDraft(@RequestHeader("Authorization") String authorization, @PathVariable String draftId) {
        String accessToken = authorization.substring(7);
        CommonUserResponseDTO user = userService.getMe(accessToken);

        return ResponseEntity.ok(draftUsecase.getDraft(draftId, user.getId()));
    }

    @PutMapping("/chapter")
    public ResponseEntity<Boolean> updateChapters(@RequestHeader("Authorization") String authorization, @RequestBody UpdateChaptersDraftDTO payload) {
        String accessToken = authorization.substring(7);
        CommonUserResponseDTO user = userService.getMe(accessToken);

        return ResponseEntity.ok(draftUsecase.updateChapters(payload, user.getId()));
    }

    @PutMapping("/lesson")
    public ResponseEntity<Boolean> updateLessons(@RequestHeader("Authorization") String authorization, @RequestBody UpdateLessonsDraftDTO payload) {
        String accessToken = authorization.substring(7);
        CommonUserResponseDTO user = userService.getMe(accessToken);

        return ResponseEntity.ok(draftUsecase.updateLessons(payload, user.getId()));
    }

    @PutMapping("/generate-matrix")
    public ResponseEntity<Boolean> generateMatrix(@RequestHeader("Authorization") String authorization, @RequestParam String draftId) {
        String accessToken = authorization.substring(7);
        CommonUserResponseDTO user = userService.getMe(accessToken);

        return ResponseEntity.ok(draftUsecase.generateMatrix(draftId, user.getId()));
    }

    @PutMapping("/generate-matrix-details")
    public ResponseEntity<Boolean> generateMatrixDetails(@RequestHeader("Authorization") String authorization, @RequestParam String draftId) {
        String accessToken = authorization.substring(7);
        CommonUserResponseDTO user = userService.getMe(accessToken);

        return ResponseEntity.ok(draftUsecase.generateMatrixDetails(draftId, user.getId()));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<DraftDTO>> getRecentDraft(@RequestHeader("Authorization") String authorization) {
        String accessToken = authorization.substring(7);
        CommonUserResponseDTO user = userService.getMe(accessToken);

        return ResponseEntity.ok(draftUsecase.getRecentDraft(user.getId()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<DraftDTO>> getAllUserDrafts(@RequestHeader("Authorization") String authorization) {
        String accessToken = authorization.substring(7);
        CommonUserResponseDTO user = userService.getMe(accessToken);

        return ResponseEntity.ok(draftUsecase.getAllUserDrafts(user.getId()));
    }
}
