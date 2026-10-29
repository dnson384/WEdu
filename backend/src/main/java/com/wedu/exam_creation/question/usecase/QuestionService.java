package com.wedu.exam_creation.question.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.wedu.exam_creation.ai.service.AIQuestionGenerationService;
import com.wedu.exam_creation.common.dto.ai.request.GenerateQuestionRequestDTO;
import com.wedu.exam_creation.common.dto.ai.response.AIQuestionResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.LessonDataResponseDTO;
import com.wedu.exam_creation.common.dto.exam.response.ExamGeneratedDTO;
import com.wedu.exam_creation.common.dto.exam.response.ExamQuestionGeneratedDTO;
import com.wedu.exam_creation.common.dto.question.NewQuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.PendingQuestionPageDTO;
import com.wedu.exam_creation.common.dto.question.response.PendingQuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
import com.wedu.exam_creation.common.exception.ConflictException;
import java.time.Instant;
import com.wedu.exam_creation.notification.telegram.TelegramNotificationService;
import com.wedu.exam_creation.question.domain.entity.QuestionEntity;
import com.wedu.exam_creation.question.domain.repository.IQuestionRepository;
import com.wedu.exam_creation.question.dto.mapper.QuestionDTOMapper;
import com.wedu.exam_creation.question.dto.request.ExamMatrixDetailDTO;
import com.wedu.exam_creation.questionReview.usecase.QuestionReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class QuestionService {
    private final IQuestionRepository repo;
    private final QuestionDTOMapper mapper;
    private final AIQuestionGenerationService aiQuestionGenerationService;
    private final TelegramNotificationService telegramNotificationService;
    private final QuestionReviewService questionReviewService;

    public QuestionService(IQuestionRepository repo, QuestionDTOMapper mapper, AIQuestionGenerationService aiQuestionGenerationService, TelegramNotificationService telegramNotificationService, QuestionReviewService questionReviewService) {
        this.repo = repo;
        this.mapper = mapper;
        this.aiQuestionGenerationService = aiQuestionGenerationService;
        this.telegramNotificationService = telegramNotificationService;
        this.questionReviewService = questionReviewService;
    }

    public List<String> insertFromDocx(List<NewQuestionDTO> questions) {
        return insert(questions, "APPROVED");
    }

    public PendingQuestionPageDTO findPending(
            int page, String search, String questionType, List<String> chapterIds, List<String> lessonIds) {
        long total = repo.countPending(search, questionType, chapterIds, lessonIds);
        List<QuestionEntity> questions = repo.findPending(page, search, questionType, chapterIds, lessonIds);
        return new PendingQuestionPageDTO(
                questions.stream().map(mapper::entityToPendingDTO).toList(), page, 10,
                total, (total + 9) / 10);
    }

    public PendingQuestionDTO getPendingForEdit(String id) {
        QuestionEntity entity = repo.findById(id);
        if (!"PENDING_REVIEW".equals(entity.getStatus())) {
            throw new ConflictException("Câu hỏi không còn chờ duyệt. Vui lòng tải lại danh sách");
        }
        return mapper.entityToPendingDTO(entity);
    }

    public void updatePendingContent(String id, ContentDTO question, List<ContentDTO> options, Instant expectedUpdatedAt) {
        if (!repo.updatePendingContent(id, mapper.contentDTOToEntity(question),
                options.stream().map(mapper::contentDTOToEntity).toList(), expectedUpdatedAt)) {
            throw new ConflictException("Câu hỏi đã thay đổi. Vui lòng tải lại danh sách trước khi chỉnh sửa");
        }
    }

    private List<String> insert(List<NewQuestionDTO> questions, String status) {
        List<QuestionEntity> newQuestionsEntity = questions.stream()
                .map(question -> {
                    QuestionEntity entity = mapper.newQuestionDTOToEntity(question);
                    entity.setStatus(status);
                    return entity;
                })
                .toList();
        return repo.saveQuestions(newQuestionsEntity);
    }

    public List<QuestionDTO> findByIds(List<String> ids) {
        List<QuestionEntity> questions = repo.findByIds(ids);
        return questions.stream()
                .map(mapper::entityToCommonDTO)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamGeneratedDTO generateExamQuestions(String accountType, List<ChapterResponseDTO> categories, List<ExamMatrixDetailDTO> matrixDetails) throws JsonProcessingException {
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
            ChapterResponseDTO curChapter = categories.stream()
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
                        curChapter.getName(),
                        curLesson.getName(),
                        detail.getQuestionType(),
                        detail.getDifficultyLevel(),
                        detail.getLearningOutcome()
                );
                errors.add(errorMsg);

                GenerateQuestionRequestDTO requestDTO = new GenerateQuestionRequestDTO(
                        missingCount,
                        curChapter.getId(),
                        curChapter.getName(),
                        curLesson.getId(),
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
        if (accountType.equals("PLUS") && !aiRequests.isEmpty()) {
            List<AIQuestionResponseDTO> generatedQuestions = aiQuestionGenerationService.generateQuestions(aiRequests);

            List<NewQuestionDTO> aiQuestions = toNewQuestionDTO(generatedQuestions);
            List<String> newQuestionIds = this.insert(aiQuestions, "PENDING_REVIEW");
            questionReviewService.createForQuestions(newQuestionIds);

            telegramNotificationService.notifyPendingReview(newQuestionIds);

            int cursor = 0;
            for (GenerateQuestionRequestDTO request : aiRequests) {
                int count = request.getNumberOfQuestions();
                List<String> idsForThisRequest = newQuestionIds.subList(cursor, cursor + count);
                cursor += count;

                String groupKey = request.getQuestionType() + "_" + request.getDifficultyLevel();
                ExamQuestionGeneratedDTO group = groupedResult.computeIfAbsent(groupKey, k -> new ExamQuestionGeneratedDTO(
                        request.getQuestionType(),
                        request.getDifficultyLevel(),
                        new ArrayList<>()
                ));
                group.getQuestionIds().addAll(idsForThisRequest);
            }
        }

        return new ExamGeneratedDTO(new ArrayList<>(groupedResult.values()), errors);
    }

    private List<NewQuestionDTO> toNewQuestionDTO(List<AIQuestionResponseDTO> aiQuestions) {
        List<NewQuestionDTO> newQuestionDTOS = new ArrayList<>();

        for (AIQuestionResponseDTO ai : aiQuestions) {
            NewQuestionDTO dto = new NewQuestionDTO(
                    null,
                    ai.getChapterId(),
                    ai.getLessonId(),
                    ai.getExerciseType(),
                    ai.getDifficultyLevel(),
                    List.of(ai.getLearningOutcome()),
                    ai.getQuestionType(),
                    ai.getQuestion(),
                    ai.getOptions()
            );

            newQuestionDTOS.add(dto);
        }

        return newQuestionDTOS;
    }
}
