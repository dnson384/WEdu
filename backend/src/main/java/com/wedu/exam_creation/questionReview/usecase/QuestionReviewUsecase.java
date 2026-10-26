package com.wedu.exam_creation.questionReview.usecase;

import com.wedu.exam_creation.questionReview.domain.entity.QuestionReviewEntity;
import com.wedu.exam_creation.questionReview.domain.repository.IQuestionReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionReviewUsecase {
    private final IQuestionReviewRepository repo;

    public QuestionReviewUsecase(IQuestionReviewRepository repo) {
        this.repo = repo;
    }

    public void createForQuestions(List<String> questionIds) {
        List<QuestionReviewEntity> reviews = questionIds.stream()
                .map(id -> QuestionReviewEntity.builder().id(id).build())
                .toList();
        repo.saveReviews(reviews);
    }
}
