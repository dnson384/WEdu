package com.wedu.exam_creation.questionReview.infrastructure.repository;

import com.wedu.exam_creation.questionReview.domain.entity.QuestionReviewEntity;
import com.wedu.exam_creation.questionReview.domain.repository.IQuestionReviewRepository;
import com.wedu.exam_creation.questionReview.infrastructure.document.QuestionReviewDocument;
import com.wedu.exam_creation.questionReview.infrastructure.mapper.QuestionReviewMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class QuestionReviewRepositoryImpl implements IQuestionReviewRepository {
    private final MongoTemplate mongoTemplate;
    private final QuestionReviewMapper mapper;

    public QuestionReviewRepositoryImpl(MongoTemplate mongoTemplate, QuestionReviewMapper mapper) {
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public void saveReviews(List<QuestionReviewEntity> reviews) {
        if (reviews.isEmpty()) {
            return;
        }

        List<QuestionReviewDocument> documents = reviews.stream()
                .map(mapper::toDocument)
                .toList();
        mongoTemplate.insertAll(documents);
    }
}
