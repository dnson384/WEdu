package com.wedu.exam_creation.auth.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.wedu.exam_creation.auth.domain.entity.AuthMigrationChangeEntity;
import com.wedu.exam_creation.auth.infrastructure.repository.AuthMigrationRepositoryImpl;
import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import java.util.Collection;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;

@SuppressWarnings("unchecked")
class AuthMigrationRepositoryTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final MongoCollection<Document> users = mock(MongoCollection.class);
    private final AuthMigrationRepositoryImpl repository = new AuthMigrationRepositoryImpl(mongo);

    @BeforeEach
    void setup() {
        when(mongo.collectionExists(UserDocument.class)).thenReturn(true);
        when(mongo.getCollectionName(UserDocument.class)).thenReturn("Users");
        when(mongo.getCollection("Users")).thenReturn(users);
    }

    @Test
    void readingUsersPreservesOpaqueIdAndHandlesMalformedLegacyEmail() {
        var id = new ObjectId();
        FindIterable<Document> found = mock(FindIterable.class);
        when(users.find()).thenReturn(found);
        when(found.projection(any())).thenReturn(found);
        doAnswer(
                        call -> {
                            Collection<Document> result = call.getArgument(0);
                            result.add(
                                    new Document("_id", id)
                                            .append("email", "A@example.com")
                                            .append("loginMethod", "LOCAL")
                                            .append("hashedPassword", "hash"));
                            result.add(new Document("_id", "legacy-id").append("email", 123));
                            return result;
                        })
                .when(found)
                .into(any());
        var result = repository.findUsers();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isSameAs(id);
        assertThat(result.get(0).getHashedPassword()).isEqualTo("hash");
        assertThat(result.get(1).getId()).isEqualTo("legacy-id");
        assertThat(result.get(1).getEmail()).isNull();
    }

    @Test
    void onlyDerivedFieldIndexesAreDropped() {
        ListIndexesIterable<Document> indexes = mock(ListIndexesIterable.class);
        when(users.listIndexes()).thenReturn(indexes);
        doAnswer(
                        call -> {
                            Collection<Document> result = call.getArgument(0);
                            result.add(
                                    new Document("name", "auth_email_key")
                                            .append("key", new Document("emailKey", 1)));
                            result.add(
                                    new Document("name", "email_1")
                                            .append("key", new Document("email", 1)));
                            result.add(
                                    new Document("name", "auth_google_subject")
                                            .append("key", new Document("googleSubject", 1)));
                            return result;
                        })
                .when(indexes)
                .into(any());
        repository.dropLegacyIndexes();
        verify(users).dropIndex("auth_email_key");
        verify(users, times(1)).dropIndex(anyString());
    }

    @Test
    void partialUpdatesPreservePasswordRoleAndPlusAndKeepOriginalIdType() {
        var id = new ObjectId();
        repository.apply(
                List.of(
                        new AuthMigrationChangeEntity(id, "a@example.com", "MIXED"),
                        new AuthMigrationChangeEntity("legacy-id", "b@example.com", null)));
        ArgumentCaptor<List<WriteModel<Document>>> writes = ArgumentCaptor.forClass(List.class);
        verify(users).bulkWrite(writes.capture());
        var first = (UpdateOneModel<Document>) writes.getValue().get(0);
        assertThat(((Document) first.getFilter()).get("_id")).isSameAs(id);
        var update = (Document) first.getUpdate();
        assertThat(update.get("$set", Document.class))
                .containsOnlyKeys("email", "loginMethod")
                .containsEntry("email", "a@example.com")
                .containsEntry("loginMethod", "MIXED");
        assertThat(update.get("$unset", Document.class)).containsOnlyKeys("emailKey");
        var second = (UpdateOneModel<Document>) writes.getValue().get(1);
        assertThat(((Document) second.getFilter()).get("_id")).isEqualTo("legacy-id");
        assertThat(((Document) second.getUpdate()).get("$set", Document.class))
                .containsOnlyKeys("email");
    }

    @Test
    void missingCollectionHasNoReadOrDdlSideEffects() {
        when(mongo.collectionExists(UserDocument.class)).thenReturn(false);
        assertThat(repository.findUsers()).isEmpty();
        repository.dropLegacyIndexes();
        verifyNoInteractions(users);
    }

    @Test
    void emptyChangeListDoesNotWrite() {
        repository.apply(List.of());
        verifyNoInteractions(users);
    }
}
