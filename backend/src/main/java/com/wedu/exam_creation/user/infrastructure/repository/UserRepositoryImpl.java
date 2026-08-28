package com.wedu.exam_creation.user.infrastructure.repository;

import com.mongodb.client.result.DeleteResult;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.storage.service.S3Service;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.domain.repository.IUserRepository;
import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import com.wedu.exam_creation.user.infrastructure.mapper.UserMapper;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;


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

    @Override
    public List<UserEntity> all() {
        List<UserDocument> docs = mongoTemplate.findAll(UserDocument.class);
        return docs.stream().map(mapper::toEntity).toList();
    }

    @Override
    public List<UserEntity> findByKeyword(String keyword) {
        Query query = new Query();

        Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);

        List<Criteria> orConditions = new ArrayList<>();
        orConditions.add(Criteria.where("email").regex(pattern));
        orConditions.add(Criteria.where("username").regex(pattern));

        if (ObjectId.isValid(keyword.trim())) {
            orConditions.add(Criteria.where("_id").is(new ObjectId(keyword.trim())));
        }

        Criteria criteria = new Criteria().orOperator(orConditions.toArray(new Criteria[0]));
        query.addCriteria(criteria);

        List<UserDocument> users = mongoTemplate.find(query, UserDocument.class);

        return users.stream().map(mapper::toEntity).toList();
    }
}
