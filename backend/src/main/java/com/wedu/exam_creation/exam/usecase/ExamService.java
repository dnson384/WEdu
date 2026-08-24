package com.wedu.exam_creation.exam.usecase;

import com.wedu.exam_creation.common.dto.exam.response.ExamDetailDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.exam.domain.entity.ExamEntity;
import com.wedu.exam_creation.exam.domain.repository.IExamRepository;
import com.wedu.exam_creation.exam.dto.mapper.ExamDTOMapper;
import com.wedu.exam_creation.exam.infrastructure.repository.ExamRepositoryImpl;
import com.wedu.exam_creation.question.usecase.QuestionService;
import com.wedu.exam_creation.storage.service.S3Service;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamService {
    private final IExamRepository repo;
    private final QuestionService questionService;
    private final ExamDTOMapper mapper;
    private final S3Service s3Service;


    public ExamService(ExamRepositoryImpl repo, QuestionService questionService, ExamDTOMapper mapper, S3Service s3Service) {
        this.repo = repo;
        this.questionService = questionService;
        this.mapper = mapper;
        this.s3Service = s3Service;
    }

    public ExamDetailDTO getExamById(String examId) {
        ExamEntity exam = repo.getExamById(examId);

        List<String> questionIds = exam.getQuestions().stream()
                .flatMap(questionExam -> questionExam.getQuestionIds().stream())
                .toList();

        List<QuestionDTO> questions = questionService.findByIds(questionIds);

        return mapper.convertToExamDetailResponse(exam, questions, s3Service);
    }
}
