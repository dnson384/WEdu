package com.wedu.exam_creation.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mongodb.client.result.UpdateResult;
import com.wedu.exam_creation.payment.domain.repository.IPaymentStorageMigrationRepository;
import com.wedu.exam_creation.payment.infrastructure.repository.PaymentStorageMigrationRepositoryImpl;
import com.wedu.exam_creation.payment.usecase.PaymentStorageMigrationUsecase;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class PaymentStorageMigrationTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final PaymentStorageMigrationRepositoryImpl repository =
            new PaymentStorageMigrationRepositoryImpl(mongo);
    private final String id = "a".repeat(64);
    private final String user = "507f1f77bcf86cd799439011";
    private final Date paidAt = Date.from(Instant.parse("2026-11-01T00:00:00Z"));

    private Document order() {
        return new Document("_id", id)
                .append("userId", user)
                .append("status", "SUCCEEDED")
                .append("paidAt", paidAt)
                .append("plusExpiresAt", new Date(paidAt.getTime() + 2592000000L))
                .append("transactionNo", "123")
                .append("responseCode", "00")
                .append("transactionStatus", "00")
                .append("lastVerifiedAt", paidAt)
                .append("manualReview", false)
                .append("refundTracked", true)
                .append("clientIp", "127.0.0.1")
                .append("plusLegacyProtected", true)
                .append("revision", 2L);
    }

    private void data(Document order) {
        when(mongo.find(any(Query.class), eq(Document.class), eq("PaymentOrders")))
                .thenReturn(List.of(order));
    }

    @Test
    void invalidUserIdStopsBeforeAnyWrites() {
        Document order = order();
        order.put("userId", "invalid");
        data(order);
        assertThatThrownBy(repository::preflight).hasMessageContaining("valid ObjectId");
        verify(mongo, never()).insert(any(Document.class), anyString());
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), anyString());
    }

    @Test
    void conflictingProviderStopsBeforeAnyWrites() {
        data(order());
        when(mongo.findById(id, Document.class, "VnpayTransactions"))
                .thenReturn(new Document("_id", id).append("transactionNo", "different"));
        assertThatThrownBy(repository::preflight).hasMessageContaining("conflicts");
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), anyString());
    }

    @Test
    void legacyRefundFlagDoesNotRequireAnAiAccount() {
        data(order());
        assertThat(repository.preflight()).containsExactly(id);
    }

    @Test
    void manualReviewMustAgreeWithDerivedValue() {
        Document order = order();
        order.put("manualReview", true);
        data(order);
        assertThatThrownBy(repository::preflight).hasMessageContaining("manualReview conflicts");
    }

    @Test
    void embedsFlatDataWithoutRefundDependencies() {
        Document source = order();
        Document embedded =
                new Document("transactionNo", "123")
                        .append("responseCode", "00")
                        .append("transactionStatus", "00")
                        .append("lastVerifiedAt", paidAt);
        when(mongo.findById(id, Document.class, "PaymentOrders"))
                .thenReturn(source, new Document("_id", id).append("vnpayTransaction", embedded));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq("PaymentOrders")))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        repository.migrateOrder(id);
        ArgumentCaptor<Update> cleanup = ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateFirst(any(Query.class), cleanup.capture(), eq("PaymentOrders"));
        assertThat(cleanup.getValue().getUpdateObject().get("$set", Document.class))
                .containsEntry("userId", new ObjectId(user))
                .containsEntry("vnpayTransaction", embedded);
        assertThat(cleanup.getValue().getUpdateObject().get("$unset", Document.class))
                .containsKeys(
                        "transactionNo",
                        "responseCode",
                        "transactionStatus",
                        "lastVerifiedAt",
                        "clientIp",
                        "manualReview",
                        "refundTracked")
                .doesNotContainKey("plusLegacyProtected");
        verify(mongo, never()).remove(any(Query.class), anyString());
    }

    @Test
    void splitSourceIsRemovedOnlyAfterEmbeddedCopyVerification() {
        Document source = new Document("_id", id).append("userId", new ObjectId(user));
        Document provider = new Document("_id", id).append("transactionNo", "123");
        Document embedded = new Document("transactionNo", "123");
        when(mongo.findById(id, Document.class, "PaymentOrders"))
                .thenReturn(source, new Document("_id", id).append("vnpayTransaction", embedded));
        when(mongo.findById(id, Document.class, "VnpayTransactions")).thenReturn(provider);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq("PaymentOrders")))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        when(mongo.remove(any(Query.class), eq("VnpayTransactions")))
                .thenReturn(com.mongodb.client.result.DeleteResult.acknowledged(1));
        repository.migrateOrder(id);
        org.mockito.InOrder sequence = inOrder(mongo);
        sequence.verify(mongo).findById(id, Document.class, "PaymentOrders");
        sequence.verify(mongo).findById(id, Document.class, "VnpayTransactions");
        sequence.verify(mongo)
                .updateFirst(any(Query.class), any(Update.class), eq("PaymentOrders"));
        sequence.verify(mongo).findById(id, Document.class, "PaymentOrders");
        sequence.verify(mongo).remove(any(Query.class), eq("VnpayTransactions"));
    }

    @Test
    void failedCopyNeverDeletesSplitSource() {
        Document source = new Document("_id", id).append("userId", new ObjectId(user));
        when(mongo.findById(id, Document.class, "PaymentOrders")).thenReturn(source);
        when(mongo.findById(id, Document.class, "VnpayTransactions"))
                .thenReturn(new Document("_id", id).append("transactionNo", "123"));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq("PaymentOrders")))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        assertThatThrownBy(() -> repository.migrateOrder(id))
                .hasMessageContaining("copy verification failed");
        verify(mongo, never()).remove(any(Query.class), anyString());
    }

    @Test
    void embeddedConflictBlocksPreflight() {
        Document source = order();
        source.put("vnpayTransaction", new Document("transactionNo", "other"));
        data(source);
        assertThatThrownBy(repository::preflight).hasMessageContaining("conflicts");
    }

    @Test
    void convertedOrderIsSkippedOnRerun() {
        Document source =
                new Document("_id", id)
                        .append("userId", new ObjectId(user))
                        .append("vnpayTransaction", new Document());
        data(source);
        assertThat(repository.preflight()).isEmpty();
    }

    @Test
    void alreadySplitOrderDoesNotRequireOldAiFlag() {
        Document source =
                new Document("_id", id)
                        .append("userId", new ObjectId(user))
                        .append("paidAt", paidAt);
        data(source);
        when(mongo.findById(id, Document.class, "VnpayTransactions"))
                .thenReturn(new Document("_id", id));
        assertThat(repository.preflight()).containsExactly(id);
    }

    @Test
    void usecaseRollsBackFailedOrderAndDoesNotFinishMigration() {
        IPaymentStorageMigrationRepository storage = mock(IPaymentStorageMigrationRepository.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        SimpleTransactionStatus status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        when(storage.preflight()).thenReturn(List.of(id));
        doThrow(new IllegalStateException("copy failed")).when(storage).migrateOrder(id);
        assertThatThrownBy(() -> new PaymentStorageMigrationUsecase(storage, manager).migrate())
                .hasMessage("copy failed");
        org.mockito.InOrder sequence = inOrder(storage, manager);
        sequence.verify(storage).preflight();
        sequence.verify(storage).prepare();
        sequence.verify(manager).getTransaction(any());
        sequence.verify(storage).migrateOrder(id);
        sequence.verify(manager).rollback(status);
        verify(storage, never()).finish();
        verify(manager, never()).commit(any());
    }
}
