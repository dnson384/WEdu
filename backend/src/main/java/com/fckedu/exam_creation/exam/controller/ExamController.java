package com.fckedu.exam_creation.exam.controller;

import com.fckedu.exam_creation.common.dto.exam.response.ExamDetailDTO;
import com.fckedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.fckedu.exam_creation.exam.dto.request.GenerateExamPayload;
import com.fckedu.exam_creation.exam.dto.response.ExamDTO;
import com.fckedu.exam_creation.exam.dto.response.ExamGeneratedResponseDTO;
import com.fckedu.exam_creation.exam.usecase.ExamService;
import com.fckedu.exam_creation.exam.usecase.ExamUsecase;
import com.fckedu.exam_creation.user.usecase.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exam")
public class ExamController {
    private final ExamUsecase examUsecase;
    private final ExamService examService;
    private final UserService userService;

    public ExamController(ExamUsecase examUsecase, ExamService examService, UserService userService) {
        this.examUsecase = examUsecase;
        this.examService = examService;
        this.userService = userService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ExamGeneratedResponseDTO> generateExam(
            @RequestHeader("Authorization") String authorization,
            @CookieValue(value = "refreshToken") String refreshToken,
            @RequestBody GenerateExamPayload payload) {
        String accessToken = authorization.substring(7);
        CommonUserResponseDTO user = userService.getMe(accessToken, refreshToken);

        return ResponseEntity.ok(examUsecase.generateExam(payload.getDraftId(), user.getId()));
    }

    @GetMapping("/{examId}")
    public ResponseEntity<ExamDetailDTO> getExamById(@PathVariable String examId) {
        return ResponseEntity.ok(examService.getExamById(examId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ExamDTO>> getAllExams(
            @RequestHeader("Authorization") String authorization,
            @CookieValue(value = "refreshToken") String refreshToken
    ) {
        String accessToken = authorization.substring(7);
        CommonUserResponseDTO user = userService.getMe(accessToken, refreshToken);

        return ResponseEntity.ok(examUsecase.getAllUserExams(user.getId()));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<ExamDTO>> getRecentExam(
            @RequestHeader("Authorization") String authorization,
            @CookieValue(value = "refreshToken") String refreshToken
    ) {
        String accessToken = authorization.substring(7);
        CommonUserResponseDTO user = userService.getMe(accessToken, refreshToken);

        return ResponseEntity.ok(examUsecase.getRecentExams(user.getId()));
    }

    @DeleteMapping("/delete/{examId}")
    public ResponseEntity<Boolean> deleteExam(
            @RequestHeader("Authorization") String authorization,
            @CookieValue(value = "refreshToken") String refreshToken,
            @PathVariable String examId
    ) {
        String accessToken = authorization.substring(7);
        CommonUserResponseDTO user = userService.getMe(accessToken, refreshToken);

        return ResponseEntity.ok(examUsecase.deleteExam(user.getId(), examId));
    }
}
