package com.wedu.exam_creation.auth.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mongodb.client.result.UpdateResult;
import com.wedu.exam_creation.auth.domain.entity.VerificationEntity;
import com.wedu.exam_creation.auth.infrastructure.document.*;
import com.wedu.exam_creation.auth.infrastructure.mapper.VerificationMapper;
import com.wedu.exam_creation.auth.usecase.VerificationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;

class VerificationRepositoryImplTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final VerificationMapper mapper = mock(VerificationMapper.class);
    private final VerificationRepositoryImpl repo = new VerificationRepositoryImpl(mongo, mapper);
    private final Instant now = Instant.parse("2026-11-13T01:00:00Z");

    private OtpRateLimitDocument bucket(List<Instant> sent) {
        OtpRateLimitDocument bucket = new OtpRateLimitDocument();
        bucket.setId("teacher@example.com");
        bucket.setRevision(7);
        bucket.setSentAt(sent);
        bucket.setResendAt(now.minusSeconds(1));
        return bucket;
    }

    @Test
    @DisplayName("trả về lỗi 429 khi đã gửi năm email trong sáu mươi phút gần nhất")
    void test_reserveEmail_001() {
        when(mongo.findById(anyString(), eq(OtpRateLimitDocument.class)))
                .thenReturn(
                        bucket(
                                List.of(
                                        now.minusSeconds(3500),
                                        now.minusSeconds(480),
                                        now.minusSeconds(360),
                                        now.minusSeconds(240),
                                        now.minusSeconds(120))));
        assertThatThrownBy(() -> repo.reserveEmail("teacher@example.com", now))
                .isInstanceOfSatisfying(
                        VerificationException.class,
                        error -> {
                            assertThat(error.getStatus()).isEqualTo(429);
                            assertThat(error.getRetryAfter()).isEqualTo(100);
                        });
        verify(mongo, never())
                .updateFirst(any(Query.class), any(Update.class), eq(OtpRateLimitDocument.class));
    }

    @Test
    @DisplayName("trả về quota bỏ lượt hết hạn và giữ revision guard khi đặt chỗ gửi email")
    void test_reserveEmail_002() {
        when(mongo.findById(anyString(), eq(OtpRateLimitDocument.class)))
                .thenReturn(bucket(List.of(now.minusSeconds(3600), now.minusSeconds(121))));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(OtpRateLimitDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        repo.reserveEmail("teacher@example.com", now);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo)
                .updateFirst(query.capture(), update.capture(), eq(OtpRateLimitDocument.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("revision", 7L);
        org.bson.Document sets =
                update.getValue().getUpdateObject().get("$set", org.bson.Document.class);
        assertThat(sets.get("sentAt")).isEqualTo(List.of(now.minusSeconds(121), now));
        assertThat(sets.get("resendAt")).isEqualTo(now.plusSeconds(120));
    }

    @Test
    @DisplayName("trả về thời gian chờ còn lại khi đổi cách đăng ký trong cooldown")
    void test_reserveEmail_003() {
        OtpRateLimitDocument bucket = bucket(List.of(now.minusSeconds(20)));
        bucket.setResendAt(now.plusSeconds(100));
        when(mongo.findById(anyString(), eq(OtpRateLimitDocument.class))).thenReturn(bucket);
        assertThatThrownBy(() -> repo.reserveEmail("teacher@example.com", now))
                .isInstanceOfSatisfying(
                        VerificationException.class,
                        error -> assertThat(error.getRetryAfter()).isEqualTo(100));
    }

    @Test
    @DisplayName("trả về lỗi xác thực khi các lần đặt chỗ cạnh tranh đều thất bại")
    void test_reserveEmail_004() {
        when(mongo.findById(anyString(), eq(OtpRateLimitDocument.class)))
                .thenReturn(bucket(List.of()));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(OtpRateLimitDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 0L, null));
        assertThatThrownBy(() -> repo.reserveEmail("teacher@example.com", now))
                .isInstanceOf(VerificationException.class);
        verify(mongo, times(3))
                .updateFirst(any(Query.class), any(Update.class), eq(OtpRateLimitDocument.class));
    }

    @Test
    @DisplayName(
            "trả về query chỉ nhận OTP hợp lệ khi phiên còn hạn sẵn sàng chưa dùng và còn lượt")
    void test_consumeOtp_001() {
        repo.consumeOtp("id", "hash", now);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo)
                .findAndModify(query.capture(), any(Update.class), eq(VerificationDocument.class));
        org.bson.Document document = query.getValue().getQueryObject();
        assertThat(document)
                .containsEntry("consumed", false)
                .containsEntry("ready", true)
                .containsEntry("otpHash", "hash");
        assertThat(document.get("attempts", org.bson.Document.class)).containsEntry("$lt", 5);
        assertThat(document.get("expiresAt", org.bson.Document.class)).containsEntry("$gt", now);
        assertThat(document.get("otpExpiresAt", org.bson.Document.class)).containsEntry("$gt", now);
    }

    @Test
    @DisplayName("trả về true và reset attempts khi revision và trạng thái phiên phù hợp")
    void test_replaceOtp_001() {
        VerificationEntity entity = new VerificationEntity();
        entity.setId("id");
        entity.setRevision(2);
        entity.setOtpHash("new");
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(VerificationDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        assertThat(repo.replaceOtp(entity, now)).isTrue();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo)
                .updateFirst(query.capture(), update.capture(), eq(VerificationDocument.class));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("revision", 2L)
                .containsEntry("consumed", false);
        assertThat(update.getValue().getUpdateObject().get("$set", org.bson.Document.class))
                .containsEntry("attempts", 0)
                .containsEntry("ready", false);
    }

    @Test
    @DisplayName("trả về false khi thay OTP không cập nhật được revision của phiên")
    void test_replaceOtp_002() {
        VerificationEntity entity = new VerificationEntity();
        entity.setId("id");
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(VerificationDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 0L, null));
        assertThat(repo.replaceOtp(entity, now)).isFalse();
    }

    @Test
    @DisplayName("trả về null khi không có OTP phù hợp để consume")
    void test_consumeOtp_002() {
        assertThat(repo.consumeOtp("missing", "hash", now)).isNull();
    }

    @Test
    @DisplayName("trả về query ràng buộc binding và hạn phiên khi consume handoff Google")
    void test_consumeHandoff_001() {
        assertThat(repo.consumeHandoff("id", "binding", now)).isNull();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo)
                .findAndModify(query.capture(), update.capture(), eq(VerificationDocument.class));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("_id", "id")
                .containsEntry("purpose", "HANDOFF")
                .containsEntry("consumed", false)
                .containsEntry("bindingHash", "binding");
        assertThat(query.getValue().getQueryObject().get("expiresAt", org.bson.Document.class))
                .containsEntry("$gt", now);
        assertThat(update.getValue().getUpdateObject().get("$set", org.bson.Document.class))
                .containsEntry("consumed", true);
    }

    @Test
    @DisplayName("trả về null khi không tìm thấy phiên xác thực")
    void test_find_001() {
        assertThat(repo.find("missing")).isNull();
        verify(mongo).findById("missing", VerificationDocument.class);
    }
}
