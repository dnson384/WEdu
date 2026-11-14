package com.wedu.exam_creation.auth.infrastructure.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.wedu.exam_creation.auth.domain.entity.AuthMigrationChangeEntity;
import com.wedu.exam_creation.auth.domain.entity.AuthMigrationUserEntity;
import com.wedu.exam_creation.auth.domain.repository.IAuthMigrationRepository;
import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthMigrationRepositoryImpl implements IAuthMigrationRepository {
    private final MongoTemplate mongo;

    public AuthMigrationRepositoryImpl(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    private MongoCollection<Document> users() {
        return mongo.getCollection(mongo.getCollectionName(UserDocument.class));
    }

    @Override
    public List<AuthMigrationUserEntity> findUsers() {
        if (!mongo.collectionExists(UserDocument.class)) return List.of();
        Document projection =
                new Document("email", 1)
                        .append("googleSubject", 1)
                        .append("loginMethod", 1)
                        .append("hashedPassword", 1);
        return users().find().projection(projection).into(new ArrayList<>()).stream()
                .map(
                        user ->
                                new AuthMigrationUserEntity(
                                        user.get("_id"),
                                        string(user, "email"),
                                        string(user, "googleSubject"),
                                        string(user, "loginMethod"),
                                        string(user, "hashedPassword")))
                .toList();
    }

    private String string(Document user, String field) {
        return user.get(field) instanceof String value ? value : null;
    }

    @Override
    public void dropLegacyIndexes() {
        if (!mongo.collectionExists(UserDocument.class)) return;
        MongoCollection<Document> users = users();
        for (Document index : users.listIndexes().into(new ArrayList<>())) {
            if (index.get("key", Document.class).containsKey("emailKey")) {
                users.dropIndex(index.getString("name"));
            }
        }
    }

    @Override
    public void apply(List<AuthMigrationChangeEntity> changes) {
        if (changes.isEmpty()) return;
        List<WriteModel<Document>> writes = new ArrayList<>();
        for (AuthMigrationChangeEntity change : changes) {
            Document fields = new Document("email", change.getEmail());
            if (change.getLoginMethod() != null)
                fields.append("loginMethod", change.getLoginMethod());
            Document update =
                    new Document("$set", fields).append("$unset", new Document("emailKey", ""));
            writes.add(new UpdateOneModel<>(new Document("_id", change.getId()), update));
        }
        users().bulkWrite(writes);
    }
}
