package com.wedu.exam_creation.question.infrastructure.repository;

import com.mongodb.client.result.UpdateResult;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.question.domain.entity.ContentEntity;
import com.wedu.exam_creation.question.domain.entity.QuestionEntity;
import com.wedu.exam_creation.question.domain.repository.IQuestionRepository;
import com.wedu.exam_creation.question.infrastructure.document.QuestionDocument;
import com.wedu.exam_creation.question.infrastructure.mapper.QuestionMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Repository
public class QuestionRepositoryImpl implements IQuestionRepository {
    private final MongoTemplate mongoTemplate;
    private final QuestionMapper questionMapper;

    public QuestionRepositoryImpl(MongoTemplate mongoTemplate, QuestionMapper questionMapper) {
        this.mongoTemplate = mongoTemplate;
        this.questionMapper = questionMapper;
    }

    @Override
    public QuestionEntity findById(String id) {
        QuestionDocument document = mongoTemplate.findById(id, QuestionDocument.class);
        if (document == null) throw new NotFoundException("Không tìm thấy câu hỏi");
        return questionMapper.toEntity(document);
    }

    @Override
    public boolean updatePendingContent(String id, ContentEntity question, List<ContentEntity> options, Instant expectedUpdatedAt) {
        return updatePendingState(id, "PENDING_REVIEW", question, options, expectedUpdatedAt);
    }

    @Override
    public boolean updatePendingState(String id, String status, ContentEntity question, List<ContentEntity> options, Instant expectedUpdatedAt) {
        Query query = new Query(Criteria.where("_id").is(id).and("status").is("PENDING_REVIEW")
                .and("updatedAt").is(expectedUpdatedAt));

        QuestionEntity entity = QuestionEntity.builder().question(question).options(options).build();
        QuestionDocument document = questionMapper.toDocument(entity);

        Instant nextUpdatedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        if (expectedUpdatedAt != null && !nextUpdatedAt.isAfter(expectedUpdatedAt)) {
            nextUpdatedAt = expectedUpdatedAt.plusMillis(1);
        }

        Update update = new Update().set("updatedAt", nextUpdatedAt);
        if (!"PENDING_REVIEW".equals(status)) {
            update.set("status", status);
        }
        if (question != null) {
            update.set("question", document.getQuestion()).set("options", document.getOptions());
        }

        UpdateResult result = mongoTemplate.updateFirst(query, update, QuestionDocument.class);
        return result.getMatchedCount() == 1;
    }

    @Override
    public List<QuestionEntity> findPending(int page, String search, String questionType, List<String> chapterIds, List<String> lessonIds) {
        Query query = pendingQuery(search, questionType, chapterIds, lessonIds)
                .with(Sort.by(Sort.Direction.DESC, "createdAt", "_id"))
                .skip((long) page * 10).limit(10);
        return mongoTemplate.find(query, QuestionDocument.class).stream()
                .map(questionMapper::toEntity).toList();
    }

    @Override
    public long countPending(String search, String questionType, List<String> chapterIds, List<String> lessonIds) {
        return mongoTemplate.count(pendingQuery(search, questionType, chapterIds, lessonIds), QuestionDocument.class);
    }

    private Query pendingQuery(String search, String questionType, List<String> chapterIds, List<String> lessonIds) {
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("status").is("PENDING_REVIEW"));

        if (!questionType.isEmpty()) {
            filters.add(Criteria.where("questionType").is(questionType));
        }

        if (!search.isEmpty()) {
            filters.add(new Criteria().orOperator(
                    Criteria.where("question.template").regex(Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)),
                    Criteria.where("chapterId").in(chapterIds),
                    Criteria.where("lessonId").in(lessonIds)));
        }

        return new Query(new Criteria().andOperator(filters.toArray(Criteria[]::new)));
    }

    @Override
    public List<String> saveQuestions(List<QuestionEntity> questions) {
        List<QuestionDocument> questionsDoc = questions.stream()
                .map(question -> {
                    QuestionDocument newDoc = questionMapper.toDocument(question);
                    newDoc.setCreatedAt(Instant.now());
                    newDoc.setUpdatedAt(Instant.now());
                    return newDoc;
                })
                .toList();

        List<QuestionDocument> inserted = mongoTemplate.insertAll(questionsDoc)
                .stream()
                .toList();

        return inserted.stream()
                .map(QuestionDocument::getId)
                .toList();
    }

    @Override
    public List<QuestionEntity> findByLessonIds(List<String> lessonIds) {
        Query query = new Query(Criteria.where("lessonId").in(lessonIds));
        List<QuestionDocument> questionDocuments = mongoTemplate.find(query, QuestionDocument.class);

        if (questionDocuments.isEmpty()) {
            throw new NotFoundException("Không tìm thấy câu hỏi nào");
        }

        return questionDocuments.stream().map(questionMapper::toEntity).toList();
    }

    @Override
    public List<QuestionEntity> findByIds(List<String> ids) {
        Query query = new Query(Criteria.where("_id").in(ids));
        List<QuestionDocument> questionDocuments = mongoTemplate.find(query, QuestionDocument.class);

        if (questionDocuments.isEmpty()) {
            throw new NotFoundException("Không tìm thấy câu hỏi nào");
        }

        return questionDocuments.stream().map(questionMapper::toEntity).toList();
    }
}
