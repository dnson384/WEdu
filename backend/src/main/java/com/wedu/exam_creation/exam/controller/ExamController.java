package com.wedu.exam_creation.exam.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.wedu.exam_creation.common.dto.exam.response.ExamDetailDTO;
import com.wedu.exam_creation.exam.dto.request.GenerateExamPayload;
import com.wedu.exam_creation.exam.dto.response.ExamDTO;
import com.wedu.exam_creation.exam.dto.response.ExamGeneratedResponseDTO;
import com.wedu.exam_creation.exam.usecase.ExamService;
import com.wedu.exam_creation.exam.usecase.ExamUsecase;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exam")
public class ExamController {
    private final ExamUsecase examUsecase;
    private final ExamService examService;

    public ExamController(ExamUsecase examUsecase, ExamService examService) {
        this.examUsecase = examUsecase;
        this.examService = examService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ExamGeneratedResponseDTO> generateExam(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody GenerateExamPayload payload
    ) throws JsonProcessingException {
        return ResponseEntity.ok(examUsecase.generateExam(
                payload.getDraftId(),
                principal.getUser().getId(),
                principal.getUser().getAccountType()
        ));
    }

    @GetMapping("/{examId}")
    public ResponseEntity<ExamDetailDTO> getExamById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String examId) {
        return ResponseEntity.ok(examService.getExamById(principal.getUser().getId(), examId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ExamDTO>> getAllExams(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(examUsecase.getAllUserExams(principal.getUser().getId()));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<ExamDTO>> getRecentExam(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(examUsecase.getRecentExams(principal.getUser().getId()));
    }

    @DeleteMapping("/delete/{examId}")
    public ResponseEntity<Boolean> deleteExam(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable String examId
    ) {
        return ResponseEntity.ok(examUsecase.deleteExam(principal.getUser().getId(), examId));
    }
}
