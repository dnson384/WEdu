package com.wedu.exam_creation.auth.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mongodb.client.result.UpdateResult;
import com.wedu.exam_creation.auth.domain.entity.VerificationEntity;
import com.wedu.exam_creation.auth.infrastructure.document.*;
import com.wedu.exam_creation.auth.infrastructure.mapper.VerificationMapper;
import com.wedu.exam_creation.auth.infrastructure.repository.VerificationRepositoryImpl;
import com.wedu.exam_creation.auth.usecase.VerificationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;

class VerificationRepositoryTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final VerificationRepositoryImpl repo =
            new VerificationRepositoryImpl(mongo, Mappers.getMapper(VerificationMapper.class));
    private final Instant now = Instant.parse("2026-11-13T01:00:00Z");

    private OtpRateLimitDocument bucket(List<Instant> sent) {
        var bucket = new OtpRateLimitDocument();
        bucket.setId("teacher@example.com");
        bucket.setRevision(7);
        bucket.setSentAt(sent);
        bucket.setResendAt(now.minusSeconds(1));
        return bucket;
    }

    @Test
    void rollingHourRejectsFiveRecentSendsEvenAcrossClockHour() {
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
    void reservationPrunesExpiredEntriesAndUsesRevisionCompareAndSet() {
        when(mongo.findById(anyString(), eq(OtpRateLimitDocument.class)))
                .thenReturn(bucket(List.of(now.minusSeconds(3600), now.minusSeconds(121))));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(OtpRateLimitDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        repo.reserveEmail("teacher@example.com", now);
        var query = ArgumentCaptor.forClass(Query.class);
        var update = ArgumentCaptor.forClass(Update.class);
        verify(mongo)
                .updateFirst(query.capture(), update.capture(), eq(OtpRateLimitDocument.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("revision", 7L);
        var sets = update.getValue().getUpdateObject().get("$set", org.bson.Document.class);
        assertThat(sets.get("sentAt")).isEqualTo(List.of(now.minusSeconds(121), now));
        assertThat(sets.get("resendAt")).isEqualTo(now.plusSeconds(120));
    }

    @Test
    void cooldownAppliesAcrossDifferentSignupMethods() {
        var bucket = bucket(List.of(now.minusSeconds(20)));
        bucket.setResendAt(now.plusSeconds(100));
        when(mongo.findById(anyString(), eq(OtpRateLimitDocument.class))).thenReturn(bucket);
        assertThatThrownBy(() -> repo.reserveEmail("teacher@example.com", now))
                .isInstanceOfSatisfying(
                        VerificationException.class,
                        error -> assertThat(error.getRetryAfter()).isEqualTo(100));
    }

    @Test
    void failedConcurrentReservationsCannotSendMail() {
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
    void otpConsumptionRequiresActiveReadyUnconsumedMatchingCodeAndRemainingAttempts() {
        repo.consumeOtp("id", "hash", now);
        var query = ArgumentCaptor.forClass(Query.class);
        verify(mongo)
                .findAndModify(query.capture(), any(Update.class), eq(VerificationDocument.class));
        var document = query.getValue().getQueryObject();
        assertThat(document)
                .containsEntry("consumed", false)
                .containsEntry("ready", true)
                .containsEntry("otpHash", "hash");
        assertThat(document.get("attempts", org.bson.Document.class)).containsEntry("$lt", 5);
        assertThat(document.get("expiresAt", org.bson.Document.class)).containsEntry("$gt", now);
        assertThat(document.get("otpExpiresAt", org.bson.Document.class)).containsEntry("$gt", now);
    }

    @Test
    void resendIsConditionalAndResetsAttemptCounter() {
        var entity = new VerificationEntity();
        entity.setId("id");
        entity.setRevision(2);
        entity.setOtpHash("new");
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(VerificationDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        assertThat(repo.replaceOtp(entity, now)).isTrue();
        var query = ArgumentCaptor.forClass(Query.class);
        var update = ArgumentCaptor.forClass(Update.class);
        verify(mongo)
                .updateFirst(query.capture(), update.capture(), eq(VerificationDocument.class));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("revision", 2L)
                .containsEntry("consumed", false);
        assertThat(update.getValue().getUpdateObject().get("$set", org.bson.Document.class))
                .containsEntry("attempts", 0)
                .containsEntry("ready", false);
    }
}
