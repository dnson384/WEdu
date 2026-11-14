package com.wedu.exam_creation.user.infrastructure.repository;

import com.mongodb.client.result.DeleteResult;
import com.wedu.exam_creation.common.dto.user.request.UserUpdateFields;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.util.EmailNormalizer;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.domain.repository.IUserRepository;
import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import com.wedu.exam_creation.user.infrastructure.mapper.UserMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UserRepositoryImpl implements IUserRepository {
    private final MongoTemplate mongoTemplate;
    private final UserMapper mapper;

    public UserRepositoryImpl(MongoTemplate mongoTemplate, UserMapper mapper) {
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        Query query = new Query(Criteria.where("email").is(EmailNormalizer.normalize(email)));

        UserDocument user = mongoTemplate.findOne(query, UserDocument.class);
        return Optional.ofNullable(user).map(mapper::toEntity);
    }

    @Override
    public UserEntity save(UserEntity user) {
        UserDocument documentToSave = mapper.toDocument(user);
        documentToSave.setEmail(EmailNormalizer.normalize(user.getEmail()));
        UserDocument savedDocument = mongoTemplate.save(documentToSave);
        return mapper.toEntity(savedDocument);
    }

    public UserEntity linkGoogle(String id, String subject, String loginMethod) {
        Query query =
                Query.query(
                        Criteria.where("_id")
                                .is(id)
                                .and("role")
                                .is("ROLE_TEACHER")
                                .and("isActive")
                                .is(true)
                                .and("loginMethod")
                                .in(
                                        "MIXED".equals(loginMethod)
                                                ? List.of("LOCAL", "MIXED")
                                                : List.of("GOOGLE"))
                                .orOperator(
                                        Criteria.where("googleSubject").is(null),
                                        Criteria.where("googleSubject").is(subject)));
        UserDocument updated =
                mongoTemplate.findAndModify(
                        query,
                        new Update()
                                .set("googleSubject", subject)
                                .set("loginMethod", loginMethod)
                                .set("updatedAt", Instant.now()),
                        FindAndModifyOptions.options().returnNew(true),
                        UserDocument.class);
        if (updated == null)
            throw new com.wedu.exam_creation.common.exception.ConflictException(
                    "Không thể liên kết tài khoản Google");
        return mapper.toEntity(updated);
    }

    public UserEntity updateRole(String id, String role) {
        return updateOnly(id, new Update().set("role", role));
    }

    public UserEntity updatePassword(String id, String password) {
        return updateOnly(id, new Update().set("hashedPassword", password));
    }

    public UserEntity grantPlus(String id, Instant expiresAt) {
        return updateOnly(
                id, new Update().set("accountType", "PLUS").set("plusExpiresAt", expiresAt));
    }

    public UserEntity revokePaidTime(String id, Instant expiresAt) {
        return updateOnly(
                id,
                new Update()
                        .set("accountType", expiresAt.isAfter(Instant.now()) ? "PLUS" : "FREE")
                        .set("plusExpiresAt", expiresAt));
    }

    private UserEntity updateOnly(String id, Update update) {
        return mapper.toEntity(
                mongoTemplate.findAndModify(
                        Query.query(Criteria.where("_id").is(id)),
                        update.set("updatedAt", Instant.now()),
                        FindAndModifyOptions.options().returnNew(true),
                        UserDocument.class));
    }

    @Override
    public UserEntity updateField(String userId, UserUpdateFields updateFields) {
        Query query = new Query(Criteria.where("_id").is(userId));

        Update update = new Update();
        // Avatar
        if (updateFields.getS3Key() != null) {
            update.set("avatarUrl", updateFields.getS3Key());
        }

        // Username
        if (updateFields.getUsername() != null) {
            update.set("username", updateFields.getUsername());
        }

        // Account Status
        if (updateFields.getIsActive() != null) {
            update.set("isActive", updateFields.getIsActive());
        }
        update.set("updatedAt", Instant.now());

        UserDocument updatedDoc =
                mongoTemplate.findAndModify(
                        query,
                        update,
                        FindAndModifyOptions.options().returnNew(true),
                        UserDocument.class);

        if (updatedDoc == null) {
            return null;
        }

        return mapper.toEntity(updatedDoc);
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
