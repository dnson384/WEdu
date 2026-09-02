package com.wedu.exam_creation.exam.usecase;

import com.wedu.exam_creation.common.dto.exam.response.ExamDetailDTO;
import org.springframework.stereotype.Service;

@Service
public class ExamService {
    private final ExamUsecase examUsecase;

    public ExamService(ExamUsecase examUsecase) {
        this.examUsecase = examUsecase;
    }

    public ExamDetailDTO getExamById(String userId, String examId) {
        return examUsecase.getExamById(userId, examId);
    }
}
