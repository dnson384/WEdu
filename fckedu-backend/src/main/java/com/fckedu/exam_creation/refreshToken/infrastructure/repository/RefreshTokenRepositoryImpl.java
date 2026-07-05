package com.fckedu.exam_creation.refreshToken.infrastructure.repository;

import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.refreshToken.domain.entity.RefreshTokenEntity;
import com.fckedu.exam_creation.refreshToken.domain.repository.IRefreshTokenRepository;
import com.fckedu.exam_creation.refreshToken.infrastructure.document.RefreshTokenDocument;
import com.fckedu.exam_creation.refreshToken.infrastructure.mapper.RefreshTokenMapper;
import com.mongodb.client.result.DeleteResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class RefreshTokenRepositoryImpl implements IRefreshTokenRepository {
    private final MongoTemplate mongoTemplate;
    private final RefreshTokenMapper mapper;

    public RefreshTokenRepositoryImpl(MongoTemplate mongoTemplate, RefreshTokenMapper mapper) {
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    public boolean save(RefreshTokenEntity newEntity) {
        RefreshTokenDocument document = mapper.toDocument(newEntity);
        RefreshTokenDocument savedRefreshToken = mongoTemplate.save(document);
        return savedRefreshToken.getId() != null;
    }

    public RefreshTokenEntity getRefreshTokenByJti(String jti, String userId) {
        Criteria criteria = new Criteria();
        criteria.andOperator(
                Criteria.where("jti").is(jti),
                Criteria.where("userId").is(userId)
        );

        Query query = new Query(criteria);

        RefreshTokenDocument doc = mongoTemplate.findOne(query, RefreshTokenDocument.class);

        if (doc == null) {
            throw new NotFoundException("Không tìm thấy RT");
        }
        return mapper.toEntity(doc);
    }

    public List<RefreshTokenEntity> getRefreshTokenByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));

        List<RefreshTokenDocument> refreshTokenDocuments = mongoTemplate.find(query, RefreshTokenDocument.class);

        return refreshTokenDocuments.stream().map(mapper::toEntity).toList();
    }

    @Transactional
    public boolean delete(String jti) {
        Query query = new Query(Criteria.where("jti").is(jti));

        DeleteResult result = mongoTemplate.remove(query, RefreshTokenDocument.class);

        if (result.getDeletedCount() != 1) {
            throw new RuntimeException("Xóa Refresh Token thất bại do không tìm thấy hoặc xóa dư. Đang thực hiện Rollback!");
        }
        return true;
    }

    public boolean deleteMany(List<String> jtis) {
        Query query = new Query(Criteria.where("jti").in(jtis));

        DeleteResult result = mongoTemplate.remove(query, RefreshTokenDocument.class);

        return result.getDeletedCount() != 0;
    }
}
