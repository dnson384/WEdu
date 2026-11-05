package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.payment.infrastructure.repository.PaymentRepositoryImpl;
import com.wedu.exam_creation.payment.infrastructure.mapper.PaymentMapper;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentEmailDocument;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentOrderDocument;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.infrastructure.PaymentSchedulingConfig;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import com.wedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import com.wedu.exam_creation.user.infrastructure.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mapstruct.factory.Mappers;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentPersistenceTest {
    @Test void requestHashIsMappedAndQueriedSeparatelyFromProviderReference() {
        PaymentMapper mapper = Mappers.getMapper(PaymentMapper.class);
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId("a".repeat(64));
        order.setIdempotencyKeyHash("b".repeat(64));
        assertThat(mapper.toEntity(mapper.toDocument(order))).isEqualTo(order);
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.findOne(any(Query.class), eq(PaymentOrderDocument.class))).thenReturn(mapper.toDocument(order));
        PaymentRepositoryImpl repository = new PaymentRepositoryImpl(mongo, mapper);
        assertThat(repository.findByIdempotencyKeyHash(order.getIdempotencyKeyHash())).isEqualTo(order);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findOne(query.capture(), eq(PaymentOrderDocument.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("idempotencyKeyHash", order.getIdempotencyKeyHash());
    }

    @Test void uniqueSparseRequestHashIndexAllowsLegacyOrdersWithoutHash() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        IndexOperations orders = mock(IndexOperations.class), emails = mock(IndexOperations.class);
        when(mongo.indexOps(PaymentOrderDocument.class)).thenReturn(orders);
        when(mongo.indexOps(PaymentEmailDocument.class)).thenReturn(emails);
        new PaymentSchedulingConfig(mongo).indexes();
        ArgumentCaptor<IndexDefinition> index = ArgumentCaptor.forClass(IndexDefinition.class);
        verify(orders, times(2)).ensureIndex(index.capture());
        IndexDefinition requestIndex = index.getAllValues().stream()
                .filter(i -> i.getIndexKeys().containsKey("idempotencyKeyHash")).findFirst().orElseThrow();
        assertThat(requestIndex.getIndexOptions()).containsEntry("unique", true).containsEntry("sparse", true);
    }
    @Test void passwordAndRoleUpdatesCannotOverwritePaidEntitlement() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        UserRepositoryImpl users = new UserRepositoryImpl(mongo, Mappers.getMapper(UserMapper.class));
        users.updatePassword("teacher", "hash");
        users.updateRole("teacher", "ROLE_MODERATOR");
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo, times(2)).findAndModify(any(Query.class), update.capture(), any(FindAndModifyOptions.class), eq(UserDocument.class));
        assertThat(update.getAllValues().get(0).getUpdateObject().get("$set", org.bson.Document.class))
                .containsKeys("hashedPassword", "updatedAt").doesNotContainKeys("accountType", "plusExpiresAt", "role");
        assertThat(update.getAllValues().get(1).getUpdateObject().get("$set", org.bson.Document.class))
                .containsKeys("role", "updatedAt").doesNotContainKeys("accountType", "plusExpiresAt", "hashedPassword");
        verify(mongo, never()).save(any());
    }

    @Test void mailClaimHasBoundedRetriesAndLease() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentRepositoryImpl repository = new PaymentRepositoryImpl(mongo, Mappers.getMapper(PaymentMapper.class));
        Instant now = Instant.parse("2026-11-03T00:00:00Z");
        repository.claimEmail(now, "lease");
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).findAndModify(query.capture(), update.capture(), any(FindAndModifyOptions.class), eq(PaymentEmailDocument.class));
        assertThat(query.getValue().getQueryObject().get("attempts", org.bson.Document.class)).containsEntry("$lt", 5);
        assertThat(query.getValue().getQueryObject()).containsEntry("sentAt", null);
        assertThat(update.getValue().getUpdateObject().get("$set", org.bson.Document.class))
                .containsEntry("leaseToken", "lease").containsEntry("nextAttemptAt", now.plusSeconds(300));
    }

    @Test void mailCompletionRequiresCurrentLease() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentRepositoryImpl repository = new PaymentRepositoryImpl(mongo, Mappers.getMapper(PaymentMapper.class));
        repository.completeEmail("id", "lease", Instant.now(), true);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).updateFirst(query.capture(), any(Update.class), eq(PaymentEmailDocument.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("_id", "id").containsEntry("leaseToken", "lease");
    }
}
