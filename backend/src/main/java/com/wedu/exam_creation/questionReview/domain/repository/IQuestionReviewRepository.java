package com.wedu.exam_creation.questionReview.domain.repository;

import com.wedu.exam_creation.questionReview.domain.entity.QuestionReviewEntity;

import java.util.List;

public interface IQuestionReviewRepository {
    void saveReviews(List<QuestionReviewEntity> reviews);
}
