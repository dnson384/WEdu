package com.wedu.exam_creation.moderator.usecase;

import com.mongodb.MongoException;
import com.wedu.exam_creation.ai.service.AIQuestionGenerationService;
import com.wedu.exam_creation.chapter.usecase.ChapterService;
import com.wedu.exam_creation.common.dto.ai.request.GenerateQuestionRequestDTO;
import com.wedu.exam_creation.common.dto.ai.response.AIQuestionResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.LessonDataResponseDTO;
import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
import com.wedu.exam_creation.common.dto.question.response.PendingQuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.VariablesDTO;
import com.wedu.exam_creation.common.exception.AIGenerationException;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.moderator.dto.RejectPendingQuestionResponseDTO;
import com.wedu.exam_creation.moderator.dto.ReviewPendingQuestionDTO;
import com.wedu.exam_creation.question.usecase.QuestionService;
import com.wedu.exam_creation.questionReview.usecase.QuestionReviewService;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReviewPendingQuestionUsecase {
    private static final Pattern REFERENCE = Pattern.compile("<(math|img)_(\\d+)>");
    private final QuestionService questionService;
    private final QuestionReviewService questionReviewService;
    private final ChapterService chapterService;
    private final AIQuestionGenerationService aiQuestionGenerationService;
    private final TransactionTemplate transactionTemplate;

    public ReviewPendingQuestionUsecase(QuestionService questionService, QuestionReviewService questionReviewService,
                                        ChapterService chapterService, AIQuestionGenerationService aiQuestionGenerationService, PlatformTransactionManager transactionManager) {
        this.questionService = questionService;
        this.questionReviewService = questionReviewService;
        this.chapterService = chapterService;
        this.aiQuestionGenerationService = aiQuestionGenerationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    private static AIGenerationException invalidAI() {
        return new AIGenerationException("Dữ liệu AI không hợp lệ");
    }

    private static ConflictException conflict() {
        return new ConflictException("Câu hỏi đã thay đổi hoặc không còn chờ duyệt. Vui lòng tải lại danh sách");
    }

    public void approve(String id, ReviewPendingQuestionDTO input) {
        validate(id, input);

        inTransaction(() -> {
            PendingQuestionDTO current = requireCurrent(id, input);
            questionService.commitPendingReview(id, "APPROVED", current.getLatestRegeneratedQuestion(), input.getUpdatedAt());
            questionReviewService.delete(id, input.getReviewRevision());
        });
    }

    public RejectPendingQuestionResponseDTO reject(String id, ReviewPendingQuestionDTO input) {
        validate(id, input);

        PendingQuestionDTO current = requireCurrent(id, input);
        int nextCount = current.getRegenerationCount() + 1;
        if (nextCount >= 5) {
            inTransaction(() -> {
                requireCurrent(id, input);
                questionService.commitPendingReview(id, "NEEDS_CONSIDERATION", null, input.getUpdatedAt());
                questionReviewService.delete(id, input.getReviewRevision());
            });
            return new RejectPendingQuestionResponseDTO("NEEDS_CONSIDERATION", 5);
        }

        GenerateQuestionRequestDTO request = generationRequest(current);
        QuestionDTO candidate;
        try {
            List<AIQuestionResponseDTO> generated = aiQuestionGenerationService.generateQuestions(List.of(request));

            if (generated == null || generated.size() != 1) {
                throw invalidAI();
            }

            candidate = validateCandidate(current, request, generated.get(0));
        } catch (Exception failure) {
            throw new AIGenerationException("Không thể sinh lại câu hỏi hợp lệ. Chưa tính lượt; vui lòng thử lại");
        }

        inTransaction(() -> {
            requireCurrent(id, input);
            questionService.commitPendingReview(id, "PENDING_REVIEW", null, input.getUpdatedAt());
            questionReviewService.saveCandidate(id, candidate, input.getReviewRevision(), true);
        });

        return new RejectPendingQuestionResponseDTO("PENDING_REVIEW", nextCount);
    }

    private void validate(String id, ReviewPendingQuestionDTO input) {
        if (id == null || input == null || input.getReviewRevision() == null || input.getReviewRevision() < 0) {
            throw new BadRequestException("ID hoặc phiên bản kiểm duyệt không hợp lệ");
        }
    }

    private PendingQuestionDTO requireCurrent(String id, ReviewPendingQuestionDTO input) {
        PendingQuestionDTO current = questionService.getPendingForEdit(id);

        if (!Objects.equals(current.getUpdatedAt(), input.getUpdatedAt()) ||
                current.getReviewRevision() != input.getReviewRevision()) {
            throw conflict();
        }

        return current;
    }

    private GenerateQuestionRequestDTO generationRequest(PendingQuestionDTO question) {
        ChapterResponseDTO chapter = chapterService.getAll().stream()
                .filter(c -> Objects.equals(c.getId(), question.getChapterId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Không tìm thấy chương để sinh lại câu hỏi"));

        LessonDataResponseDTO lesson = chapter.getLessons() == null ? null : chapter.getLessons().stream()
                .filter(l -> Objects.equals(l.getId(), question.getLessonId()))
                .findFirst()
                .orElse(null);

        if (lesson == null ||
                question.getLearningOutcomes() == null ||
                question.getLearningOutcomes().isEmpty() ||
                question.getLearningOutcomes().stream()
                        .anyMatch(s -> s == null || s.isBlank())
        ) {
            throw new BadRequestException("Thiếu bài học hoặc yêu cầu cần đạt để sinh lại câu hỏi");
        }

        return new GenerateQuestionRequestDTO(
                1,
                chapter.getId(),
                chapter.getName(),
                lesson.getId(),
                lesson.getName(),
                question.getExerciseType(),
                question.getQuestionType(),
                question.getDifficultyLevel(),
                String.join("; ", question.getLearningOutcomes())
        );
    }

    private QuestionDTO validateCandidate(PendingQuestionDTO original, GenerateQuestionRequestDTO request, AIQuestionResponseDTO aiQuestion) {
        if (aiQuestion == null || !Objects.equals(original.getChapterId(), aiQuestion.getChapterId())
                || !Objects.equals(original.getLessonId(), aiQuestion.getLessonId())
                || !Objects.equals(original.getExerciseType(), aiQuestion.getExerciseType())
                || !Objects.equals(original.getDifficultyLevel(), aiQuestion.getDifficultyLevel())
                || !Objects.equals(original.getQuestionType(), aiQuestion.getQuestionType())
                || !Objects.equals(request.getLearningOutcome(), aiQuestion.getLearningOutcome())) {
            throw invalidAI();
        }

        List<ContentDTO> options = aiQuestion.getOptions();
        if (options == null || options.size() > 50) {
            throw invalidAI();
        }

        boolean validOptions = switch (original.getQuestionType()) {
            case "Nhiều lựa chọn" -> options.size() == 4;
            case "Đúng sai" -> options.size() >= 4;
            case "Trả lời ngắn" -> options.isEmpty();
            default -> false;
        };
        if (!validOptions) {
            throw invalidAI();
        }

        validateContent(aiQuestion.getQuestion());
        options.forEach(this::validateContent);

        return new QuestionDTO(
                original.getId(),
                original.getChapterId(),
                original.getLessonId(),
                original.getExerciseType(),
                original.getDifficultyLevel(),
                original.getLearningOutcomes(),
                original.getQuestionType(),
                aiQuestion.getQuestion(),
                options
        );
    }

    private void validateContent(ContentDTO content) {
        if (content == null || content.getTemplate() == null || content.getTemplate().isBlank()
                || content.getTemplate().length() > 50000) {
            throw invalidAI();
        }

        VariablesDTO variables = content.getVariables();

        Map<String, String> math = variables == null || variables.getMath() == null ? Map.of() : variables.getMath();
        Set<String> mathKeys = new HashSet<>();

        Map<String, String> images = variables == null || variables.getImage() == null ? Map.of() : variables.getImage();
        Set<String> imageKeys = new HashSet<>();

        Matcher matcher = REFERENCE.matcher(content.getTemplate());

        while (matcher.find()) {
            String key = matcher.group(1) + "_" + matcher.group(2);
            boolean isMath = matcher.group(1).equals("math");
            String value = (isMath ? math : images).get(key);

            if (value == null || value.isBlank() || value.length() > 10000 ||
                    (!isMath && (value.contains("://") ||
                            QuestionImageReference.isStoredKey(value)))) {
                throw invalidAI();
            }

            (isMath ? mathKeys : imageKeys).add(key);
        }

        if (!mathKeys.equals(math.keySet()) ||
                !imageKeys.equals(images.keySet()) ||
                REFERENCE.matcher(content.getTemplate()).replaceAll("").matches("(?s).*<(?:math|img)_.*")) {
            throw invalidAI();
        }
    }

    private void inTransaction(Runnable action) {
        try {
            transactionTemplate.executeWithoutResult(status -> action.run());
        } catch (ConcurrencyFailureException | DuplicateKeyException exception) {
            throw conflict();
        } catch (MongoException exception) {
            if (exception.hasErrorLabel("TransientTransactionError")) {
                throw conflict();
            }

            throw exception;
        }
    }

}
