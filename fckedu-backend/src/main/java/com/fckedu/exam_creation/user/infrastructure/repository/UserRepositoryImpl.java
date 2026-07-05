package com.fckedu.exam_creation.user.infrastructure.repository;

import com.fckedu.exam_creation.common.exception.InternalServerException;
import com.fckedu.exam_creation.storage.service.S3Service;
import com.fckedu.exam_creation.user.domain.entity.UserEntity;
import com.fckedu.exam_creation.user.domain.repository.IUserRepository;
import com.fckedu.exam_creation.user.infrastructure.document.UserDocument;
import com.fckedu.exam_creation.user.infrastructure.mapper.UserMapper;
import com.mongodb.client.result.DeleteResult;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Repository
public class UserRepositoryImpl implements IUserRepository {
    private final MongoTemplate mongoTemplate;
    private final UserMapper mapper;

    public UserRepositoryImpl(MongoTemplate mongoTemplate, UserMapper mapper, S3Service s3Service) {
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        Query query = new Query(Criteria.where("email").is(email));

        UserDocument user = mongoTemplate.findOne(query, UserDocument.class);
        return Optional.ofNullable(user).map(mapper::toEntity);
    }

    @Override
    public UserEntity save(UserEntity newUser) {
        UserDocument documentToSave = mapper.toDocument(newUser);
        UserDocument savedDocument = mongoTemplate.save(documentToSave);
        return mapper.toEntity(savedDocument);
    }

    @Override
    public UserEntity findById(String userId) {
        UserDocument user = mongoTemplate.findById(userId, UserDocument.class);
        return mapper.toEntity(user);
    }

    @Override
    @Transactional
    public boolean delete(String userId) {
        Query query = new Query(Criteria.where("_id").is(new ObjectId(userId)));

        DeleteResult result = mongoTemplate.remove(query, UserDocument.class);

        if (result.getDeletedCount() > 1) {
            throw new InternalServerException("Xóa quá giới hạn. Yêu cầu Rollback!");
        }

        return result.getDeletedCount() == 1;
    }
}
