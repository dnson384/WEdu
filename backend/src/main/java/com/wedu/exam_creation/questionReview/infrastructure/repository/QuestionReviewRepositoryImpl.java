package com.wedu.exam_creation.questionReview.infrastructure.repository;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.wedu.exam_creation.questionReview.domain.entity.QuestionReviewEntity;
import com.wedu.exam_creation.questionReview.domain.repository.IQuestionReviewRepository;
import com.wedu.exam_creation.questionReview.infrastructure.document.QuestionReviewDocument;
import com.wedu.exam_creation.questionReview.infrastructure.mapper.QuestionReviewMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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

    @Override
    public List<QuestionReviewEntity> findByIds(List<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        Query query = new Query(Criteria.where("_id").in(ids));

        return mongoTemplate.find(query, QuestionReviewDocument.class).stream()
                .map(mapper::toEntity)
                .toList();
    }

    @Override
    public void ensureExists(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update()
                .setOnInsert("regenerationCount", 0)
                .setOnInsert("revision", 0L)
                .setOnInsert("regeneratedQuestions", List.of());

        mongoTemplate.upsert(query, update, QuestionReviewDocument.class);
    }

    private Query versionQuery(String id, long revision) {
        Criteria criteria = Criteria.where("_id").is(id);

        if (revision == 0) {
            criteria.andOperator(new Criteria().orOperator(
                    Criteria.where("revision").is(0L),
                    Criteria.where("revision").is(null)));
        } else {
            criteria.and("revision").is(revision);
        }

        return new Query(criteria);
    }

    @Override
    public boolean update(QuestionReviewEntity review, long expectedRevision) {
        QuestionReviewDocument document = mapper.toDocument(review);
        Update update = new Update()
                .set("regeneratedQuestions", document.getRegeneratedQuestions())
                .set("regenerationCount", review.getRegenerationCount())
                .set("revision", expectedRevision + 1);

        UpdateResult result = mongoTemplate.updateFirst(
                versionQuery(review.getId(), expectedRevision),
                update,
                QuestionReviewDocument.class);
        return result.getMatchedCount() == 1;
    }

    @Override
    public boolean delete(String id, long expectedRevision) {
        DeleteResult result = mongoTemplate.remove(versionQuery(id, expectedRevision), QuestionReviewDocument.class);
        return result.getDeletedCount() == 1;
    }
}
