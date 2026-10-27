package com.wedu.exam_creation.question.infrastructure.repository;

import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.question.domain.entity.QuestionEntity;
import com.wedu.exam_creation.question.domain.repository.IQuestionRepository;
import com.wedu.exam_creation.question.infrastructure.document.QuestionDocument;
import com.wedu.exam_creation.question.infrastructure.mapper.QuestionMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort;

@Repository
public class QuestionRepositoryImpl implements IQuestionRepository {
    private final MongoTemplate mongoTemplate;
    private final QuestionMapper questionMapper;

    public QuestionRepositoryImpl(MongoTemplate mongoTemplate, QuestionMapper questionMapper) {
        this.mongoTemplate = mongoTemplate;
        this.questionMapper = questionMapper;
    }

    @Override
    public List<QuestionEntity> findPending(int page, String search, String questionType,
                                            List<String> chapterIds, List<String> lessonIds) {
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
