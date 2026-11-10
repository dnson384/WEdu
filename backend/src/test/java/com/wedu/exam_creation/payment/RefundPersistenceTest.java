package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.payment.domain.entity.*;
import com.wedu.exam_creation.payment.infrastructure.document.*;
import com.wedu.exam_creation.payment.infrastructure.mapper.RefundMapper;
import com.wedu.exam_creation.payment.infrastructure.repository.RefundRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefundPersistenceTest {
    RefundMapper mapper = Mappers.getMapper(RefundMapper.class);
    MongoTemplate mongo = mock(MongoTemplate.class);
    RefundRepositoryImpl repository = new RefundRepositoryImpl(mongo, mapper);
    @Test void snapshotMappingRetainsPolicyProviderAttemptChecksAndHistory() {
        var refund = new RefundEntity(); refund.setId("a".repeat(32)); refund.setOrderId("b".repeat(64));
        refund.setAmount(20000); refund.setRequestId("attempt"); refund.setPolicyVersion("plus-refund-v1");
        refund.setSubmittedAt(Instant.now()); refund.setProviderCode("00"); refund.setProviderStatus("05");
        refund.getChecks().add(new RefundEntity.Check("WINDOW", "PASSED", "48h"));
        var event = new RefundEntity.Event(); event.setActor("admin"); event.setStatus("PROCESSING"); event.setAt(Instant.now());
        refund.getHistory().add(event);
        assertThat(mapper.toEntity(mapper.toDocument(refund))).isEqualTo(refund);
        var account = new RefundAccountEntity(); account.setId("a".repeat(24)); account.getHolds().add(refund.getId());
        account.getActiveAi().add("attempt"); account.setTrackedSince(Instant.now()); account.setLastAiUsedAt(Instant.now());
        assertThat(mapper.toEntity(mapper.toDocument(account))).isEqualTo(account);
    }
    @Test void requestCasPreservesIdWithoutPuttingAnIdOnFindAndReplacePayload() {
        var refund = new RefundEntity(); refund.setId("a".repeat(32)); refund.setAmount(20000); refund.setRevision(7);
        when(mongo.findAndReplace(any(Query.class), any(RefundDocument.class))).thenAnswer(i -> {
            Query query = i.getArgument(0); RefundDocument replacement = i.getArgument(1);
            assertThat(query.getQueryObject()).containsEntry("_id", refund.getId()).containsEntry("revision", 7L);
            assertThat(replacement.getId()).isNull(); assertThat(replacement.getRevision()).isEqualTo(8);
            assertThat(replacement.getAmount()).isEqualTo(20000); return mapper.toDocument(refund);
        });
        assertThat(repository.update(refund, 7)).isTrue(); assertThat(refund.getId()).isEqualTo("a".repeat(32));
    }
    @Test void accountCasDoesNotRegenerateIdentityAndStaleRevisionFails() {
        var account = new RefundAccountEntity(); account.setId("a".repeat(24)); account.setRevision(4);
        when(mongo.findAndReplace(any(Query.class), any(RefundAccountDocument.class))).thenAnswer(i -> {
            Query query = i.getArgument(0); RefundAccountDocument replacement = i.getArgument(1);
            assertThat(query.getQueryObject()).containsEntry("_id", account.getId()).containsEntry("revision", 4L);
            assertThat(replacement.getId()).isNull(); return null;
        });
        assertThat(repository.saveAccount(account, 4)).isFalse(); assertThat(account.getId()).isEqualTo("a".repeat(24));
    }
}
