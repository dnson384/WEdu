package com.wedu.exam_creation.questionReview.usecase;

import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionReviewStateDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class QuestionReviewService {
    private final QuestionReviewUsecase questionReviewUsecase;

    public QuestionReviewService(QuestionReviewUsecase questionReviewUsecase) {
        this.questionReviewUsecase = questionReviewUsecase;
    }

    public void createForQuestions(List<String> questionIds) {
        questionReviewUsecase.createForQuestions(questionIds);
    }

    public Map<String, QuestionReviewStateDTO> states(List<String> ids) {
        return questionReviewUsecase.states(ids);
    }

    public QuestionReviewStateDTO state(String id) {
        return states(List.of(id)).getOrDefault(id,
                new QuestionReviewStateDTO(0, 0, null));
    }

    public void saveCandidate(String id, QuestionDTO candidate, long revision, boolean append) {
        questionReviewUsecase.saveCandidate(id, candidate, revision, append);
    }

    public void delete(String id, long revision) {
        questionReviewUsecase.delete(id, revision);
    }
}
