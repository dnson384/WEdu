package com.fckedu.exam_creation.exam.infrastructure.repository;

import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.exam.domain.entity.ExamEntity;
import com.fckedu.exam_creation.exam.domain.repository.IExamRepository;
import com.fckedu.exam_creation.exam.infrastructure.document.ExamDocument;
import com.fckedu.exam_creation.exam.infrastructure.mapper.ExamMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ExamRepositoryImpl implements IExamRepository {
    private final MongoTemplate mongoTemplate;
    private final ExamMapper mapper;

    public ExamRepositoryImpl(MongoTemplate mongoTemplate, ExamMapper mapper) {
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public String saveExam(ExamEntity entity) {
        ExamDocument doc = mapper.entityToDocument(entity);
        ExamDocument result = mongoTemplate.insert(doc);
        return result.getId();
    }

    @Override
    public ExamEntity getExamById(String examId) {
        ExamDocument exam = mongoTemplate.findById(examId, ExamDocument.class);
        if (exam == null) {
            throw new NotFoundException("Không tồn tại bài kiểm tra");
        }
        return mapper.docToEntity(exam);
    }

    @Override
    public List<ExamEntity> getAllUserExams(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        List<ExamDocument> exams = mongoTemplate.find(query, ExamDocument.class);

        if (exams.isEmpty()) {
            throw new NotFoundException("Danh sách bài kiểm tra trống");
        }

        return exams.stream().map(mapper::docToEntity).toList();
    }

    @Override
    public List<ExamEntity> getRecentExams(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "updatedAt"))
                .limit(5);

        List<ExamDocument> examDocuments = mongoTemplate.find(query, ExamDocument.class);

        return examDocuments.stream().map(mapper::docToEntity).toList();

    }

}
