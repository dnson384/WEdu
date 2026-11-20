package com.wedu.exam_creation.auth.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Query;

class AuthIndexConfigTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final IndexOperations indexes = mock(IndexOperations.class);
    private final AuthIndexConfig config = new AuthIndexConfig(mongo);

    private void canonicalUser() {
        UserDocument user = new UserDocument();
        user.setEmail("teacher@example.com");
        when(mongo.stream(any(Query.class), eq(UserDocument.class))).thenReturn(Stream.of(user));
        when(mongo.indexOps(any(Class.class))).thenReturn(indexes);
    }

    @SuppressWarnings("unchecked")
    private void databaseIndexes(Document... documents) {
        MongoCollection<Document> collection = mock(MongoCollection.class);
        ListIndexesIterable<Document> cursor = mock(ListIndexesIterable.class);
        when(mongo.collectionExists(UserDocument.class)).thenReturn(true);
        when(mongo.getCollectionName(UserDocument.class)).thenReturn("Users");
        when(mongo.getCollection("Users")).thenReturn(collection);
        when(collection.listIndexes()).thenReturn(cursor);
        doAnswer(
                        call -> {
                            Collection<Document> result = call.getArgument(0);
                            result.addAll(List.of(documents));
                            return result;
                        })
                .when(cursor)
                .into(any());
    }

    @Test
    @DisplayName("trả về unique index email khi user có email chuẩn hóa")
    void test_run_001() {
        canonicalUser();
        config.run(new DefaultApplicationArguments());
        verify(indexes)
                .createIndex(
                        argThat(
                                (IndexDefinition definition) ->
                                        definition.getIndexKeys().containsKey("email")
                                                && Boolean.TRUE.equals(
                                                        definition
                                                                .getIndexOptions()
                                                                .get("unique"))));
        verify(mongo, never()).exists(any(Query.class), eq(UserDocument.class));
    }

    @Test
    @DisplayName("trả về lỗi yêu cầu migration khi email chưa chuẩn hóa")
    void test_run_002() {
        UserDocument user = new UserDocument();
        user.setEmail(" Teacher@Example.com ");
        when(mongo.stream(any(Query.class), eq(UserDocument.class))).thenReturn(Stream.of(user));
        assertThatThrownBy(() -> config.run(new DefaultApplicationArguments()))
                .hasMessageContaining("Users.email normalization required")
                .hasMessageContaining("--auth.migration.enabled=true");
        verifyNoInteractions(indexes);
    }

    @Test
    @DisplayName("trả về lỗi yêu cầu dọn index khi còn index emailKey cũ")
    void test_run_003() {
        canonicalUser();
        databaseIndexes(
                new Document("name", "auth_email_key")
                        .append("key", new Document("emailKey", 1))
                        .append("unique", true));
        assertThatThrownBy(() -> config.run(new DefaultApplicationArguments()))
                .hasMessageContaining("obsolete auth email index")
                .hasMessageContaining("--auth.migration.enabled=true");
        verifyNoInteractions(indexes);
    }

    @Test
    @DisplayName("trả về index email được tái sử dụng khi đã có unique index đầy đủ")
    void test_run_004() {
        canonicalUser();
        databaseIndexes(
                new Document("name", "email_1")
                        .append("key", new Document("email", 1))
                        .append("unique", true));
        config.run(new DefaultApplicationArguments());
        verify(indexes, never())
                .createIndex(
                        argThat(
                                (IndexDefinition definition) ->
                                        definition.getIndexKeys().containsKey("email")));
        verify(indexes, times(3)).createIndex(any());
    }
}
