package com.fckedu.exam_creation.question.infrastructure.repository;

import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.question.domain.entity.QuestionEntity;
import com.fckedu.exam_creation.question.domain.repository.IQuestionRepository;
import com.fckedu.exam_creation.question.infrastructure.document.QuestionDocument;
import com.fckedu.exam_creation.question.infrastructure.mapper.QuestionMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class QuestionRepositoryImpl implements IQuestionRepository {
    private final MongoTemplate mongoTemplate;
    private final QuestionMapper questionMapper;

    public QuestionRepositoryImpl(MongoTemplate mongoTemplate, QuestionMapper questionMapper) {
        this.mongoTemplate = mongoTemplate;
        this.questionMapper = questionMapper;
    }

    @Override
    public void saveQuestions(List<QuestionEntity> questions) {
        List<QuestionDocument> questionsDoc = questions.stream()
                .map(question -> {
                    QuestionDocument newDoc = questionMapper.toDocument(question);
                    newDoc.setCreatedAt(LocalDateTime.now());
                    newDoc.setUpdatedAt(LocalDateTime.now());
                    return newDoc;
                })
                .toList();

        mongoTemplate.insertAll(questionsDoc);
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
