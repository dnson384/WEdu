package com.wedu.exam_creation.payment.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mongodb.client.result.UpdateResult;
import com.wedu.exam_creation.payment.domain.entity.PaymentEmailEntity;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.domain.entity.VnpayTransactionEntity;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentEmailDocument;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentOrderDocument;
import com.wedu.exam_creation.payment.infrastructure.document.VnpayTransactionDocument;
import com.wedu.exam_creation.payment.infrastructure.mapper.PaymentMapper;
import java.time.Instant;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class PaymentRepositoryImplTest {
    @Test
    @DisplayName("trả về đơn đúng khi truy vấn bằng hash yêu cầu độc lập với mã giao dịch")
    void test_findByIdempotencyKeyHash_001() {
        PaymentMapper mapper = mock(PaymentMapper.class);
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId("a".repeat(64));
        order.setVnpayTransaction(
                new com.wedu.exam_creation.payment.domain.entity.VnpayTransactionEntity(
                        null, null, null, null));
        order.setIdempotencyKeyHash("b".repeat(64));
        PaymentOrderDocument stored = new PaymentOrderDocument();
        stored.setVnpayTransaction(new VnpayTransactionDocument());
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.findOne(any(Query.class), eq(PaymentOrderDocument.class))).thenReturn(stored);
        when(mapper.toEntity(stored)).thenReturn(order);
        PaymentRepositoryImpl repository = new PaymentRepositoryImpl(mongo, mapper);
        assertThat(repository.findByIdempotencyKeyHash(order.getIdempotencyKeyHash()))
                .isEqualTo(order);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findOne(query.capture(), eq(PaymentOrderDocument.class));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("idempotencyKeyHash", order.getIdempotencyKeyHash());
    }

    @Test
    @DisplayName("trả về không có ngoại lệ và áp dụng lease khi tìm email còn lượt gửi lại")
    void test_claimEmail_002() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentRepositoryImpl repository =
                new PaymentRepositoryImpl(mongo, mock(PaymentMapper.class));
        Instant now = Instant.parse("2026-11-03T00:00:00Z");
        repository.claimEmail(now, "lease");
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo)
                .findAndModify(
                        query.capture(),
                        update.capture(),
                        any(FindAndModifyOptions.class),
                        eq(PaymentEmailDocument.class));
        assertThat(query.getValue().getQueryObject().get("attempts", org.bson.Document.class))
                .containsEntry("$lt", 5);
        assertThat(query.getValue().getQueryObject())
                .containsEntry("sentAt", null)
                .containsEntry("refundId", null);
        assertThat(update.getValue().getUpdateObject().get("$set", org.bson.Document.class))
                .containsEntry("leaseToken", "lease")
                .containsEntry("nextAttemptAt", now.plusSeconds(300));
    }

    @Test
    @DisplayName("trả về không có ngoại lệ và lọc theo lease khi hoàn tất gửi email")
    void test_completeEmail_003() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentRepositoryImpl repository =
                new PaymentRepositoryImpl(mongo, mock(PaymentMapper.class));
        repository.completeEmail("id", "lease", Instant.parse("2026-11-21T00:00:00Z"), true);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo)
                .updateFirst(query.capture(), any(Update.class), eq(PaymentEmailDocument.class));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("_id", "id")
                .containsEntry("leaseToken", "lease");
    }

    @Test
    @DisplayName(
            "trả về IllegalArgumentException trước khi ghi khi đơn thiếu giao dịch VNPay nhúng")
    void test_insert_004() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentRepositoryImpl repository =
                new PaymentRepositoryImpl(mongo, mock(PaymentMapper.class));
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId("order");
        assertThatThrownBy(() -> repository.insert(order))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.update(order, 0))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(mongo);
    }

    @Test
    @DisplayName("trả về kết quả theo chủ đơn khi cập nhật riêng thời điểm xác minh nhúng")
    void test_claimVerification_005() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentRepositoryImpl repository =
                new PaymentRepositoryImpl(mongo, mock(PaymentMapper.class));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(PaymentOrderDocument.class)))
                .thenReturn(
                        UpdateResult.acknowledged(0, 0L, null),
                        UpdateResult.acknowledged(1, 1L, null));
        Instant now = Instant.parse("2026-11-21T00:00:00Z");
        assertThat(repository.claimVerification("order", "other", now)).isFalse();
        assertThat(repository.claimVerification("order", "owner", now)).isTrue();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo, times(2))
                .updateFirst(query.capture(), update.capture(), eq(PaymentOrderDocument.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("userId", "owner");
        assertThat(update.getValue().getUpdateObject().get("$set", Document.class))
                .containsOnlyKeys("vnpayTransaction.lastVerifiedAt")
                .containsEntry("vnpayTransaction.lastVerifiedAt", now);
    }

    @Test
    @DisplayName("trả về false với revision cũ và true khi cập nhật được mà giữ timestamp xác minh")
    void test_update_006() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentRepositoryImpl repository =
                new PaymentRepositoryImpl(mongo, mock(PaymentMapper.class));
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId("order");
        order.setVnpayTransaction(
                new com.wedu.exam_creation.payment.domain.entity.VnpayTransactionEntity(
                        "123", "00", "00", Instant.parse("2026-11-21T00:00:00Z")));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(PaymentOrderDocument.class)))
                .thenReturn(
                        UpdateResult.acknowledged(0, 0L, null),
                        UpdateResult.acknowledged(1, 1L, null));
        assertThat(repository.update(order, 1)).isFalse();
        assertThat(repository.update(order, 1)).isTrue();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo, times(2))
                .updateFirst(query.capture(), update.capture(), eq(PaymentOrderDocument.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("revision", 1L);
        assertThat(update.getValue().getUpdateObject().get("$set", Document.class))
                .containsEntry("vnpayTransaction.transactionNo", "123")
                .doesNotContainKeys("vnpayTransaction", "vnpayTransaction.lastVerifiedAt");
    }

    @Test
    @DisplayName("trả về trang và count theo cùng bộ lọc khi từ khóa có ký tự regex")
    void test_searchTransactions_007() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.find(any(Query.class), eq(PaymentOrderDocument.class))).thenReturn(List.of());
        PaymentRepositoryImpl repository =
                new PaymentRepositoryImpl(mongo, mock(PaymentMapper.class));
        Instant from = Instant.parse("2026-11-07T17:00:00Z"), to = from.plusSeconds(86400);
        repository.searchTransactions("a.*", "PENDING", from, to, 2);
        repository.countTransactions("a.*", "PENDING", from, to);
        ArgumentCaptor<Query> search = ArgumentCaptor.forClass(Query.class),
                count = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(search.capture(), eq(PaymentOrderDocument.class));
        verify(mongo).count(count.capture(), eq(PaymentOrderDocument.class));
        Query query = search.getValue();
        assertThat(query.getSkip()).isEqualTo(20);
        assertThat(query.getLimit()).isEqualTo(10);
        assertThat(query.getSortObject())
                .isEqualTo(new Document("createdAt", -1).append("_id", -1));
        assertThat(query.getQueryObject().toString())
                .isEqualTo(count.getValue().getQueryObject().toString());
        assertThat(query.getQueryObject()).containsEntry("status", "PENDING");
        assertThat(query.getQueryObject().get("createdAt", Document.class))
                .containsEntry("$gte", from)
                .containsEntry("$lt", to);
        List<Document> alternatives = query.getQueryObject().getList("$or", Document.class);
        assertThat(alternatives.get(0)).containsEntry("_id", "a.*");
        assertThat(alternatives.get(1)).containsEntry("vnpayTransaction.transactionNo", "a.*");
        java.util.regex.Pattern pattern =
                alternatives.get(2).get("email", java.util.regex.Pattern.class);
        assertThat(pattern.pattern()).isEqualTo("\\Qa.*\\E");
        assertThat(pattern.flags() & java.util.regex.Pattern.CASE_INSENSITIVE).isNotZero();
        assertThat(pattern.matcher("A.*@example.com").find()).isTrue();
        assertThat(pattern.matcher("alice@example.com").find()).isFalse();
        assertThat(count.getValue().getLimit()).isZero();
    }

    @Test
    @DisplayName("trả về giao dịch VNPay được ánh xạ khi tìm thấy đơn có dữ liệu nhúng")
    void test_find_008() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentMapper mapper = mock(PaymentMapper.class);
        PaymentOrderDocument stored = new PaymentOrderDocument();
        stored.setVnpayTransaction(new VnpayTransactionDocument());
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setVnpayTransaction(new VnpayTransactionEntity("123", "00", "00", null));
        when(mongo.findById("order", PaymentOrderDocument.class)).thenReturn(stored);
        when(mapper.toEntity(stored)).thenReturn(order);

        assertThat(new PaymentRepositoryImpl(mongo, mapper).find("order")).isSameAs(order);
    }

    @Test
    @DisplayName("trả về null khi không tìm thấy đơn theo mã tham chiếu")
    void test_find_009() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentMapper mapper = mock(PaymentMapper.class);

        assertThat(new PaymentRepositoryImpl(mongo, mapper).find("missing")).isNull();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi không có đơn chờ quá hạn và query giới hạn 100 đơn")
    void test_expiredPending_010() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentMapper mapper = mock(PaymentMapper.class);
        Instant now = Instant.parse("2026-11-21T00:00:00Z");
        when(mongo.find(any(Query.class), eq(PaymentOrderDocument.class))).thenReturn(List.of());

        assertThat(new PaymentRepositoryImpl(mongo, mapper).expiredPending(now)).isEmpty();

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(PaymentOrderDocument.class));
        assertThat(query.getValue().getLimit()).isEqualTo(100);
        assertThat(query.getValue().getQueryObject())
                .containsEntry("status", "PENDING")
                .containsEntry("expiresAt", new Document("$lte", now));
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi ghi email đã ánh xạ vào outbox")
    void test_enqueueEmail_011() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentMapper mapper = mock(PaymentMapper.class);
        PaymentEmailEntity email = new PaymentEmailEntity();
        PaymentEmailDocument document = new PaymentEmailDocument();
        when(mapper.toDocument(email)).thenReturn(document);

        new PaymentRepositoryImpl(mongo, mapper).enqueueEmail(email);

        verify(mongo).insert(document);
    }
}
