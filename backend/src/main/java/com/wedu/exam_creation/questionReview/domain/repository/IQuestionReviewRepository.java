package com.wedu.exam_creation.questionReview.domain.repository;

import com.wedu.exam_creation.questionReview.domain.entity.QuestionReviewEntity;

import java.util.List;

public interface IQuestionReviewRepository {
    void saveReviews(List<QuestionReviewEntity> reviews);
    List<QuestionReviewEntity> findByIds(List<String> ids);
    void ensureExists(String id);
    boolean update(QuestionReviewEntity review, long expectedRevision);
    boolean delete(String id, long expectedRevision);
}
