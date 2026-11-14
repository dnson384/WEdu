package com.wedu.exam_creation.auth.infrastructure;

import com.wedu.exam_creation.auth.infrastructure.document.OtpRateLimitDocument;
import com.wedu.exam_creation.auth.infrastructure.document.VerificationDocument;
import com.wedu.exam_creation.common.util.EmailNormalizer;
import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuthIndexConfig implements ApplicationRunner {
    private final MongoTemplate mongo;

    public AuthIndexConfig(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        Query emails = new Query();
        emails.fields().include("email");
        try (var users = mongo.stream(emails, UserDocument.class)) {
            if (users.anyMatch(user -> !EmailNormalizer.isCanonical(user.getEmail()))) {
                throw new IllegalStateException(
                        "Start once with --auth.migration.enabled=true: Users.email normalization required");
            }
        }
        List<Document> indexes =
                mongo.collectionExists(UserDocument.class)
                        ? mongo.getCollection(mongo.getCollectionName(UserDocument.class))
                                .listIndexes()
                                .into(new ArrayList<>())
                        : List.of();
        if (indexes.stream()
                .anyMatch(index -> index.get("key", Document.class).containsKey("emailKey"))) {
            throw new IllegalStateException(
                    "Start once with --auth.migration.enabled=true: obsolete auth email index must be removed");
        }
        boolean emailIndexed =
                indexes.stream()
                        .anyMatch(
                                index -> {
                                    Document key = index.get("key", Document.class);
                                    Document collation = index.get("collation", Document.class);
                                    return key.size() == 1
                                            && Integer.valueOf(1).equals(key.get("email"))
                                            && index.getBoolean("unique", false)
                                            && !index.getBoolean("sparse", false)
                                            && !index.containsKey("partialFilterExpression")
                                            && (collation == null
                                                    || "simple"
                                                            .equals(collation.getString("locale")));
                                });
        if (!emailIndexed)
            mongo.indexOps(UserDocument.class)
                    .createIndex(
                            new Index()
                                    .on("email", Sort.Direction.ASC)
                                    .unique()
                                    .named("auth_email"));
        mongo.indexOps(UserDocument.class)
                .createIndex(
                        new Index()
                                .on("googleSubject", Sort.Direction.ASC)
                                .unique()
                                .partial(
                                        PartialIndexFilter.of(
                                                Criteria.where("googleSubject").type(2)))
                                .named("auth_google_subject"));
        mongo.indexOps(VerificationDocument.class)
                .createIndex(new Index().on("expiresAt", Sort.Direction.ASC).expire(Duration.ZERO));
        mongo.indexOps(OtpRateLimitDocument.class)
                .createIndex(new Index().on("expiresAt", Sort.Direction.ASC).expire(Duration.ZERO));
    }
}
