package com.wedu.exam_creation.passwordResetToken.infrastructure.repository;

import com.wedu.exam_creation.passwordResetToken.domain.entity.PasswordResetTokenEntity;
import com.wedu.exam_creation.passwordResetToken.domain.repository.IPasswordResetTokenRepository;
import com.wedu.exam_creation.passwordResetToken.infrastructure.document.PasswordResetTokenDocument;
import com.wedu.exam_creation.passwordResetToken.infrastructure.mapper.PasswordResetTokenMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetTokenRepositoryImpl implements IPasswordResetTokenRepository {
    private final MongoTemplate mongoTemplate;
    private final PasswordResetTokenMapper mapper;

    public PasswordResetTokenRepositoryImpl(MongoTemplate mongoTemplate, PasswordResetTokenMapper mapper) {
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    public void deleteByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        mongoTemplate.remove(query, PasswordResetTokenDocument.class);
    }

    public void save(PasswordResetTokenEntity entity) {
        PasswordResetTokenDocument doc = mapper.toDocument(entity);
        mongoTemplate.save(doc);
    }

    public PasswordResetTokenEntity findByTokenHashAndUsedFalse(String tokenHash) {
        Criteria criteria = new Criteria();
        criteria.andOperator(
                Criteria.where("tokenHash").is(tokenHash),
                Criteria.where("used").is(false)
        );

        Query query = new Query(criteria);

        return mapper.toEntity(mongoTemplate.findOne(query, PasswordResetTokenDocument.class));
    }
}
