package com.wedu.exam_creation.payment.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mongodb.client.result.UpdateResult;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class PaymentStorageMigrationRepositoryImplTest {
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
    @DisplayName("trả về lỗi kiểm tra dữ liệu trước khi ghi khi userId không hợp lệ")
    void test_preflight_001() {
        Document order = order();
        order.put("userId", "invalid");
        data(order);
        assertThatThrownBy(repository::preflight).hasMessageContaining("valid ObjectId");
        verify(mongo, never()).insert(any(Document.class), anyString());
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), anyString());
    }

    @Test
    @DisplayName("trả về lỗi xung đột trước khi ghi khi dữ liệu giao dịch cũ khác nhau")
    void test_preflight_002() {
        data(order());
        when(mongo.findById(id, Document.class, "VnpayTransactions"))
                .thenReturn(new Document("_id", id).append("transactionNo", "different"));
        assertThatThrownBy(repository::preflight).hasMessageContaining("conflicts");
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), anyString());
    }

    @Test
    @DisplayName("trả về ID cần di trú khi đơn chứa cờ refund legacy")
    void test_preflight_003() {
        data(order());
        assertThat(repository.preflight()).containsExactly(id);
    }

    @Test
    @DisplayName("trả về lỗi kiểm tra dữ liệu khi cờ manualReview khác trạng thái suy ra")
    void test_preflight_004() {
        Document order = order();
        order.put("manualReview", true);
        data(order);
        assertThatThrownBy(repository::preflight).hasMessageContaining("manualReview conflicts");
    }

    @Test
    @DisplayName("trả về không có ngoại lệ và nhúng thông tin VNPay khi di trú đơn có trường phẳng")
    void test_migrateOrder_005() {
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
    @DisplayName("trả về không có ngoại lệ và xóa bản tách khi đã xác minh bản nhúng")
    void test_migrateOrder_006() {
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
    @DisplayName("trả về lỗi và giữ bản tách khi không xác minh được bản sao")
    void test_migrateOrder_007() {
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
    @DisplayName("trả về lỗi xung đột khi giao dịch nhúng khác thông tin legacy")
    void test_preflight_008() {
        Document source = order();
        source.put("vnpayTransaction", new Document("transactionNo", "other"));
        data(source);
        assertThatThrownBy(repository::preflight).hasMessageContaining("conflicts");
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi chạy lại sau khi đơn đã được di trú")
    void test_preflight_009() {
        Document source =
                new Document("_id", id)
                        .append("userId", new ObjectId(user))
                        .append("vnpayTransaction", new Document());
        data(source);
        assertThat(repository.preflight()).isEmpty();
    }

    @Test
    @DisplayName("trả về ID cần di trú khi đơn tách giao dịch thiếu cờ AI cũ")
    void test_preflight_010() {
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
    @DisplayName("trả về không có ngoại lệ khi mọi đơn dùng ObjectId và có dữ liệu VNPay nhúng")
    void test_assertCurrent_011() {
        when(mongo.stream(any(Query.class), eq(Document.class), eq("PaymentOrders")))
                .thenReturn(
                        Stream.of(
                                new Document("userId", new ObjectId(user))
                                        .append("vnpayTransaction", new Document())));

        assertThatCode(repository::assertCurrent).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("trả về lỗi yêu cầu di trú khi vẫn còn đơn dùng schema cũ")
    void test_assertCurrent_012() {
        when(mongo.stream(any(Query.class), eq(Document.class), eq("PaymentOrders")))
                .thenReturn(Stream.of(order()));

        assertThatThrownBy(repository::assertCurrent)
                .hasMessageContaining("--payment.storage-migration.enabled=true");
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), anyString());
    }
}
