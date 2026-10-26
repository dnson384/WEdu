package com.wedu.exam_creation.questionReview.usecase;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionReviewService {
    private final QuestionReviewUsecase questionReviewUsecase;

    public QuestionReviewService(QuestionReviewUsecase questionReviewUsecase) {
        this.questionReviewUsecase = questionReviewUsecase;
    }

    public void createForQuestions(List<String> questionIds) {
        questionReviewUsecase.createForQuestions(questionIds);
    }
}
