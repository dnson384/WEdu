package com.wedu.exam_creation.exam.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.wedu.exam_creation.chapter.usecase.ChapterService;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import com.wedu.exam_creation.common.dto.draft.response.ChapterDraftDTO;
import com.wedu.exam_creation.common.dto.draft.response.DraftDTO;
import com.wedu.exam_creation.common.dto.draft.response.LessonDraftDTO;
import com.wedu.exam_creation.common.dto.draft.response.MatrixDetailItemDTO;
import com.wedu.exam_creation.common.dto.exam.response.ExamDetailDTO;
import com.wedu.exam_creation.common.dto.exam.response.ExamGeneratedDTO;
import com.wedu.exam_creation.common.dto.exam.response.ExamQuestionGeneratedDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.draft.usecase.DraftService;
import com.wedu.exam_creation.exam.domain.entity.ChapterExamEntity;
import com.wedu.exam_creation.exam.domain.entity.ExamEntity;
import com.wedu.exam_creation.exam.domain.entity.QuestionExamEntity;
import com.wedu.exam_creation.exam.domain.repository.IExamRepository;
import com.wedu.exam_creation.exam.dto.mapper.ExamDTOMapper;
import com.wedu.exam_creation.exam.dto.response.ExamDTO;
import com.wedu.exam_creation.exam.dto.response.ExamGeneratedResponseDTO;
import com.wedu.exam_creation.question.dto.request.ExamMatrixDetailDTO;
import com.wedu.exam_creation.question.usecase.QuestionService;
import com.wedu.exam_creation.storage.service.S3Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExamUsecase {
    private final IExamRepository repo;
    private final QuestionService questionService;
    private final DraftService draftService;
    private final ExamDTOMapper mapper;
    private final ChapterService chapterService;
    private final S3Service s3Service;

    public ExamUsecase(IExamRepository repo, QuestionService questionService, DraftService draftService, ExamDTOMapper mapper, ChapterService chapterService, S3Service s3Service) {
        this.repo = repo;
        this.questionService = questionService;
        this.draftService = draftService;
        this.mapper = mapper;
        this.chapterService = chapterService;
        this.s3Service = s3Service;
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamGeneratedResponseDTO generateExam(String draftId, String userId, String accountType) throws JsonProcessingException {
        DraftDTO draft = draftService.getDraft(draftId, userId);

        List<ChapterExamEntity> chaptersExam = new ArrayList<>();

        List<ExamMatrixDetailDTO> examMatrixDetailDTOS = new ArrayList<>();
        for (ChapterDraftDTO chapter : draft.getChapters()) {
            List<String> lessonIds = chapter.getLessons().stream().map(LessonDraftDTO::getId).toList();

            chaptersExam.add(new ChapterExamEntity(
                    lessonIds,
                    chapter.getId()
            ));

            for (LessonDraftDTO lesson : chapter.getLessons()) {
                for (MatrixDetailItemDTO matrixDetail : lesson.getMatrixDetails()) {
                    if (matrixDetail.getSelectedCount() > 0) {
                        examMatrixDetailDTOS.add(new ExamMatrixDetailDTO(
                                chapter.getId(),
                                lesson.getId(),
                                matrixDetail.getExerciseType(),
                                matrixDetail.getDifficultyLevel(),
                                matrixDetail.getLearningOutcome(),
                                matrixDetail.getQuestionType(),
                                matrixDetail.getSelectedCount()
                        ));
                    }
                }
            }
        }

        List<String> chapterIds = draft.getChapters().stream().map(ChapterDraftDTO::getId).toList();
        List<ChapterResponseDTO> categories = chapterService.getByIds(chapterIds);

        ExamGeneratedDTO examGeneratedResult = questionService.generateExamQuestions(accountType, categories, examMatrixDetailDTOS);

        if (examGeneratedResult == null) {
            throw new InternalServerException("Lỗi trong quá trình sinh đề");
        }

        List<ExamQuestionGeneratedDTO> questions = examGeneratedResult.getQuestions();

        ExamEntity payload = new ExamEntity(
                null,
                draft.getUserId(),
                draftId,
                draft.getExamName(),
                chaptersExam,
                questions.stream()
                        .map(q -> new QuestionExamEntity(
                                q.getQuestionType(),
                                q.getDifficultyLevel(),
                                q.getQuestionIds()
                        ))
                        .toList(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        String examId = repo.saveExam(payload);
        List<String> errors = examGeneratedResult.getErrors();

        return new ExamGeneratedResponseDTO(examId, errors);
    }

    public List<ExamDTO> getAllUserExams(String userId) {
        List<ExamEntity> examEntities = repo.getAllUserExams(userId);

        return examEntities.stream()
                .map(mapper::convertToExamResponse)
                .toList();
    }

    public ExamDetailDTO getExamById(String userId, String examId) {
        ExamEntity exam = repo.getExamById(userId, examId);

        List<String> questionIds = exam.getQuestions().stream()
                .flatMap(questionExam -> questionExam.getQuestionIds().stream())
                .toList();

        List<QuestionDTO> questions = questionService.findByIds(questionIds);

        return mapper.convertToExamDetailResponse(exam, questions, s3Service);
    }

    public List<ExamDTO> getRecentExams(String userId) {
        List<ExamEntity> examEntities = repo.getRecentExams(userId);
        return examEntities.stream().map(mapper::convertToExamResponse).toList();
    }

    public boolean deleteExam(String userId, String examId) {
        boolean deleteResult = repo.deleteExam(userId, examId);

        if (!deleteResult) {
            throw new NotFoundException("Không tìm thấy đề kiểm tra để xóa");
        }
        return true;
    }
}
