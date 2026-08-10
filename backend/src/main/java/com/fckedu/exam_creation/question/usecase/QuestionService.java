package com.fckedu.exam_creation.question.usecase;

import com.fckedu.exam_creation.ai.service.AIQuestionGenerationService;
import com.fckedu.exam_creation.common.dto.category.response.CategoryResponseDTO;
import com.fckedu.exam_creation.common.dto.category.response.LessonDataResponseDTO;
import com.fckedu.exam_creation.common.dto.exam.response.ExamGeneratedDTO;
import com.fckedu.exam_creation.common.dto.exam.response.ExamQuestionGeneratedDTO;
import com.fckedu.exam_creation.common.dto.question.NewQuestionDTO;
import com.fckedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.fckedu.exam_creation.question.domain.entity.QuestionEntity;
import com.fckedu.exam_creation.question.domain.repository.IQuestionRepository;
import com.fckedu.exam_creation.question.dto.mapper.QuestionDTOMapper;
import com.fckedu.exam_creation.question.dto.request.ExamMatrixDetailDTO;
import com.fckedu.exam_creation.question.dto.request.GenerateQuestionRequestDTO;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuestionService {
    private final IQuestionRepository repo;
    private final QuestionDTOMapper mapper;
    private final AIQuestionGenerationService aiQuestionGenerationService;

    public QuestionService(IQuestionRepository repo, QuestionDTOMapper mapper, AIQuestionGenerationService aiQuestionGenerationService) {
        this.repo = repo;
        this.mapper = mapper;
        this.aiQuestionGenerationService = aiQuestionGenerationService;
    }

    public void insert(List<NewQuestionDTO> questions) {
        List<QuestionEntity> newQuestionsEntity = questions.stream().map(mapper::newQuestionDTOToEntity).toList();
        repo.saveQuestions(newQuestionsEntity);
    }

    public List<QuestionDTO> findByIds(List<String> ids) {
        List<QuestionEntity> questions = repo.findByIds(ids);
        return questions.stream()
                .map(mapper::entityToCommonDTO)
                .toList();
    }

    public ExamGeneratedDTO generateExamQuestions(String accountType, List<CategoryResponseDTO> categories, List<ExamMatrixDetailDTO> matrixDetails) {
        List<String> errors = new ArrayList<>();

        List<GenerateQuestionRequestDTO> aiRequests = new ArrayList<>();

        // Tạo mảng lessonId không trùng id
        List<String> uniqueLessonIds = matrixDetails.stream()
                .map(ExamMatrixDetailDTO::getLessonId)
                .distinct()
                .toList();

        if (uniqueLessonIds.isEmpty()) {
            return null;
        }

        // Lấy toàn bộ câu hỏi
        List<QuestionEntity> allQuestionsInLessons = repo.findByLessonIds(uniqueLessonIds);

        // Tạo pool chứa các câu hỏi có thể dùng
        List<QuestionEntity> availablePool = new ArrayList<>(allQuestionsInLessons);

        Map<String, ExamQuestionGeneratedDTO> groupedResult = new LinkedHashMap<>();

        Map<String, Integer> missingCountByPair = new LinkedHashMap<>();

        for (ExamMatrixDetailDTO detail : matrixDetails) {
            if (detail.getLimit() <= 0) {
                continue;
            }

            // Lọc câu hỏi khớp với điều kiện
            List<QuestionEntity> matchingQuestions = availablePool.stream()
                    .filter(q -> q.getLessonId().equals(detail.getLessonId()) &&
                            q.getExerciseType().equals(detail.getExerciseType()) &&
                            q.getDifficultyLevel().equals(detail.getDifficultyLevel()) &&
                            q.getQuestionType().equals(detail.getQuestionType()) &&
                            q.getLearningOutcomes().contains(detail.getLearningOutcome())
                    )
                    .toList();

            List<QuestionEntity> shuffledMatches = new ArrayList<>(matchingQuestions);
            Collections.shuffle(shuffledMatches);

            // Lấy lượng câu hỏi cần thiết (nếu thiếu thì lấy tối đa số đang có)
            int takeCount = Math.min(detail.getLimit(), shuffledMatches.size());
            List<QuestionEntity> selectedQuestions = shuffledMatches.subList(0, takeCount);

            // Tính toán và ghi nhận số lượng thiếu cho cặp này
            CategoryResponseDTO curChapter = categories.stream()
                    .filter(cate -> cate.getId().equals(detail.getChapterId()))
                    .findFirst()
                    .orElse(null);

            if (curChapter == null) {
                continue;
            }

            LessonDataResponseDTO curLesson = curChapter.getLessons().stream()
                    .filter(l -> l.getId().equals(detail.getLessonId()))
                    .findFirst()
                    .orElse(null);

            if (curLesson == null) {
                continue;
            }

            // Thiếu cầu hỏi
            int missingCount = detail.getLimit() - takeCount;
            if (missingCount > 0) {
                String errorMsg = String.format(
                        "Thiếu %d câu hỏi | Chương: %s | Bài: %s | Loại: %s | Mức độ: %s | YCCĐ: %s",
                        missingCount,
                        curChapter.getChapter(),
                        curLesson.getName(),
                        detail.getQuestionType(),
                        detail.getDifficultyLevel(),
                        detail.getLearningOutcome()
                );
                errors.add(errorMsg);

                GenerateQuestionRequestDTO requestDTO = new GenerateQuestionRequestDTO(
                        missingCount,
                        curChapter.getChapter(),
                        curLesson.getName(),
                        detail.getExerciseType(),
                        detail.getQuestionType(),
                        detail.getDifficultyLevel(),
                        detail.getLearningOutcome()
                );

                aiRequests.add(requestDTO);
            }


            if (!selectedQuestions.isEmpty()) {
                // QUAN TRỌNG: Xóa các câu đã chọn khỏi pool để không bị bốc trùng ở vòng lặp sau
                availablePool.removeAll(selectedQuestions);

                List<String> selectedIds = selectedQuestions.stream()
                        .map(QuestionEntity::getId)
                        .toList();

                String groupKey = detail.getQuestionType() + "_" + detail.getDifficultyLevel();

                // Gom nhóm
                ExamQuestionGeneratedDTO group = groupedResult.computeIfAbsent(groupKey, k -> new ExamQuestionGeneratedDTO(
                        detail.getQuestionType(),
                        detail.getDifficultyLevel(),
                        new ArrayList<>()
                ));

                group.getQuestionIds().addAll(selectedIds);
            }
        }

        // Gọi AI Generate câu hỏi nếu là tài khoản plus
        if (!aiRequests.isEmpty()) {
            aiQuestionGenerationService.generateQuestions(aiRequests);
        }

        return new ExamGeneratedDTO(new ArrayList<>(groupedResult.values()), errors);
    }


}
