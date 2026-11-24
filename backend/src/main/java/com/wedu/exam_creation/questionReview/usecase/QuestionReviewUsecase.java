package com.wedu.exam_creation.questionReview.usecase;

import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionReviewStateDTO;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.questionReview.domain.entity.QuestionReviewEntity;
import com.wedu.exam_creation.questionReview.domain.entity.RegeneratedQuestionEntity;
import com.wedu.exam_creation.questionReview.domain.repository.IQuestionReviewRepository;
import com.wedu.exam_creation.questionReview.infrastructure.mapper.QuestionReviewMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuestionReviewUsecase {
    private final IQuestionReviewRepository repo;
    private final QuestionReviewMapper mapper;

    public QuestionReviewUsecase(IQuestionReviewRepository repo, QuestionReviewMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    private static ConflictException conflict() {
        return new ConflictException("Câu hỏi đã thay đổi. Vui lòng tải lại danh sách");
    }

    public void createForQuestions(List<String> questionIds) {
        List<QuestionReviewEntity> reviews = questionIds.stream()
                .map(id -> QuestionReviewEntity.builder().id(id).build())
                .toList();
        repo.saveReviews(reviews);
    }

    public Map<String, QuestionReviewStateDTO> states(List<String> ids) {
        Map<String, QuestionReviewStateDTO> result = new LinkedHashMap<>();
        for (QuestionReviewEntity review : repo.findByIds(ids)) {
            List<RegeneratedQuestionEntity> snapshots = review.getRegeneratedQuestions();
            QuestionDTO latest = snapshots == null || snapshots.isEmpty()
                    ? null : mapper.toDTO(snapshots.get(snapshots.size() - 1));
            result.put(review.getId(), new QuestionReviewStateDTO(review.getRegenerationCount(), review.getRevision(), latest));
        }

        return result;
    }

    public void saveCandidate(String id, QuestionDTO candidate, long revision, boolean append) {
        repo.ensureExists(id);

        QuestionReviewEntity review = repo.findByIds(List.of(id)).stream()
                .findFirst()
                .orElseThrow(QuestionReviewUsecase::conflict);
        if (review.getRevision() != revision) {
            throw conflict();
        }
        if (review.getRegeneratedQuestions() == null) {
            review.setRegeneratedQuestions(new ArrayList<>());
        }

        RegeneratedQuestionEntity snapshot = mapper.toSnapshot(candidate);
        snapshot.setId(id);
        snapshot.setStatus("PENDING_REVIEW");
        snapshot.setUpdatedAt(Instant.now());
        if (append) {
            if (review.getRegenerationCount() >= 4) {
                throw conflict();
            }
            snapshot.setCreatedAt(snapshot.getUpdatedAt());
            review.getRegeneratedQuestions().add(snapshot);
            review.setRegenerationCount(review.getRegenerationCount() + 1);
        } else {
            int last = review.getRegeneratedQuestions().size() - 1;
            if (last < 0) {
                throw conflict();
            }
            snapshot.setCreatedAt(review.getRegeneratedQuestions().get(last).getCreatedAt());
            review.getRegeneratedQuestions().set(last, snapshot);
        }
        if (!repo.update(review, revision)) {
            throw conflict();
        }
    }

    public void delete(String id, long revision) {
        repo.ensureExists(id);
        if (!repo.delete(id, revision)) {
            throw conflict();
        }
    }
}
