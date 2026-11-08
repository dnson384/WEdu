package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.payment.infrastructure.repository.PaymentRepositoryImpl;
import com.wedu.exam_creation.payment.infrastructure.mapper.PaymentMapper;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentOrderDocument;
import com.wedu.exam_creation.payment.infrastructure.AdminTransactionIndexConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mapstruct.factory.Mappers;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.bson.Document;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminTransactionRepositoryTest {
    @Test void searchAndCountShareFiltersAndEscapeRegexBeforePagination() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.find(any(Query.class), eq(PaymentOrderDocument.class))).thenReturn(List.of());
        var repository = new PaymentRepositoryImpl(mongo, Mappers.getMapper(PaymentMapper.class));
        Instant from = Instant.parse("2026-11-07T17:00:00Z"), to = from.plusSeconds(86400);
        repository.searchTransactions("a.*", "PENDING", from, to, 2);
        repository.countTransactions("a.*", "PENDING", from, to);
        ArgumentCaptor<Query> search = ArgumentCaptor.forClass(Query.class), count = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(search.capture(), eq(PaymentOrderDocument.class));
        verify(mongo).count(count.capture(), eq(PaymentOrderDocument.class));
        Query query = search.getValue();
        assertThat(query.getSkip()).isEqualTo(20);
        assertThat(query.getLimit()).isEqualTo(10);
        assertThat(query.getSortObject()).isEqualTo(new Document("createdAt", -1).append("_id", -1));
        assertThat(query.getQueryObject().toString()).isEqualTo(count.getValue().getQueryObject().toString());
        assertThat(query.getQueryObject()).containsEntry("status", "PENDING");
        assertThat(query.getQueryObject().get("createdAt", Document.class)).containsEntry("$gte", from).containsEntry("$lt", to);
        List<Document> alternatives = query.getQueryObject().getList("$or", Document.class);
        assertThat(alternatives.get(0)).containsEntry("_id", "a.*");
        assertThat(alternatives.get(1)).containsEntry("transactionNo", "a.*");
        var pattern = alternatives.get(2).get("email", java.util.regex.Pattern.class);
        assertThat(pattern.pattern()).isEqualTo("\\Qa.*\\E");
        assertThat(pattern.flags() & java.util.regex.Pattern.CASE_INSENSITIVE).isNotZero();
        assertThat(pattern.matcher("A.*@example.com").find()).isTrue();
        assertThat(pattern.matcher("alice@example.com").find()).isFalse();
        assertThat(count.getValue().getLimit()).isZero();
    }

    @Test void indexesDoNotDependOnEnabledPayment() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        IndexOperations operations = mock(IndexOperations.class);
        when(mongo.indexOps(PaymentOrderDocument.class)).thenReturn(operations);
        new AdminTransactionIndexConfig(mongo).indexes();
        ArgumentCaptor<IndexDefinition> indexes = ArgumentCaptor.forClass(IndexDefinition.class);
        verify(operations, times(3)).ensureIndex(indexes.capture());
        assertThat(indexes.getAllValues().stream().map(IndexDefinition::getIndexKeys).toList()).contains(
                new Document("createdAt", -1).append("_id", -1),
                new Document("status", 1).append("createdAt", -1).append("_id", -1), new Document("transactionNo", 1));
    }
}
