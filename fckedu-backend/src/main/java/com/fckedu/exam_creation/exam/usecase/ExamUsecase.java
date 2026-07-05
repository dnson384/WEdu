package com.fckedu.exam_creation.exam.usecase;

import com.fckedu.exam_creation.common.dto.draft.response.ChapterDraftDTO;
import com.fckedu.exam_creation.common.dto.draft.response.DraftDTO;
import com.fckedu.exam_creation.common.dto.draft.response.LessonDraftDTO;
import com.fckedu.exam_creation.common.dto.draft.response.MatrixDetailItemDTO;
import com.fckedu.exam_creation.common.dto.exam.response.ExamQuestionGeneratedDTO;
import com.fckedu.exam_creation.draft.usecase.DraftService;
import com.fckedu.exam_creation.exam.domain.entity.ChapterExamEntity;
import com.fckedu.exam_creation.exam.domain.entity.ExamEntity;
import com.fckedu.exam_creation.exam.domain.entity.QuestionExamEntity;
import com.fckedu.exam_creation.exam.domain.repository.IExamRepository;
import com.fckedu.exam_creation.exam.dto.mapper.ExamDTOMapper;
import com.fckedu.exam_creation.exam.dto.response.ExamDTO;
import com.fckedu.exam_creation.question.dto.request.ExamMatrixDetailDTO;
import com.fckedu.exam_creation.question.usecase.QuestionService;
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

    public ExamUsecase(IExamRepository repo, QuestionService questionService, DraftService draftService, ExamDTOMapper mapper) {
        this.repo = repo;
        this.questionService = questionService;
        this.draftService = draftService;
        this.mapper = mapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public String generateExam(String draftId, String userId) {
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

        List<ExamQuestionGeneratedDTO> questions = questionService.generateExamQuestions(examMatrixDetailDTOS);

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

        return repo.saveExam(payload);
    }

    public List<ExamDTO> getAllUserExams(String userId) {
        List<ExamEntity> examEntities = repo.getAllUserExams(userId);

        return examEntities.stream()
                .map(mapper::convertToExamResponse)
                .toList();
    }

    public List<ExamDTO> getRecentExams(String userId) {
        List<ExamEntity> examEntities = repo.getRecentExams(userId);
        return examEntities.stream().map(mapper::convertToExamResponse).toList();
    }
}
