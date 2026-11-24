package com.wedu.exam_creation.auth.infrastructure.repository;

import com.wedu.exam_creation.auth.domain.entity.VerificationEntity;
import com.wedu.exam_creation.auth.domain.repository.IVerificationRepository;
import com.wedu.exam_creation.auth.infrastructure.document.OtpRateLimitDocument;
import com.wedu.exam_creation.auth.infrastructure.document.VerificationDocument;
import com.wedu.exam_creation.auth.infrastructure.mapper.VerificationMapper;
import com.wedu.exam_creation.auth.usecase.VerificationException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class VerificationRepositoryImpl implements IVerificationRepository {
    private final MongoTemplate mongo;
    private final VerificationMapper mapper;

    public VerificationRepositoryImpl(MongoTemplate mongo, VerificationMapper mapper) {
        this.mongo = mongo;
        this.mapper = mapper;
    }

    public void insert(VerificationEntity entity) {
        mongo.insert(mapper.toDocument(entity));
    }

    public VerificationEntity find(String id) {
        return mapper.toEntity(mongo.findById(id, VerificationDocument.class));
    }

    public void reserveEmail(String email, Instant now) {
        OtpRateLimitDocument fresh = new OtpRateLimitDocument();
        fresh.setId(email);
        fresh.setResendAt(now);
        fresh.setExpiresAt(now.plusSeconds(7200));
        try {
            mongo.insert(fresh);
        } catch (DuplicateKeyException ignored) {
            /* Existing bucket. */
        }
        for (int retryCount = 0; retryCount < 3; retryCount++) {
            OtpRateLimitDocument bucket = mongo.findById(email, OtpRateLimitDocument.class);
            if (bucket == null) continue;
            var recent =
                    new java.util.ArrayList<>(
                            bucket.getSentAt().stream()
                                    .filter(sent -> sent.isAfter(now.minusSeconds(3600)))
                                    .toList());
            Instant retry =
                    recent.size() >= 5 ? recent.get(0).plusSeconds(3600) : bucket.getResendAt();
            if (retry.isAfter(now)) {
                throw new VerificationException(
                        "OTP_RATE_LIMIT",
                        "Vui lòng chờ trước khi gửi lại OTP",
                        429,
                        Math.max(1, Duration.between(now, retry).getSeconds()));
            }
            recent.add(now);
            if (mongo.updateFirst(
                                    Query.query(
                                            Criteria.where("_id")
                                                    .is(email)
                                                    .and("revision")
                                                    .is(bucket.getRevision())),
                                    new Update()
                                            .set("sentAt", recent)
                                            .set("resendAt", now.plusSeconds(120))
                                            .set("expiresAt", now.plusSeconds(7200))
                                            .inc("revision", 1),
                                    OtpRateLimitDocument.class)
                            .getModifiedCount()
                    == 1) return;
        }
        throw new VerificationException(
                "OTP_RATE_LIMIT", "Yêu cầu gửi OTP đang được xử lý. Vui lòng thử lại", 429, 1);
    }

    private Criteria active(String id, Instant now) {
        return Criteria.where("_id").is(id).and("consumed").is(false).and("expiresAt").gt(now);
    }

    public boolean replaceOtp(VerificationEntity entity, Instant now) {
        return mongo.updateFirst(
                                Query.query(
                                        active(entity.getId(), now)
                                                .and("revision")
                                                .is(entity.getRevision())),
                                new Update()
                                        .set("otpHash", entity.getOtpHash())
                                        .set("otpExpiresAt", entity.getOtpExpiresAt())
                                        .set("resendAt", entity.getResendAt())
                                        .set("attempts", 0)
                                        .set("ready", false)
                                        .inc("revision", 1),
                                VerificationDocument.class)
                        .getModifiedCount()
                == 1;
    }

    public void markReady(String id, String otpHash, boolean ready) {
        mongo.updateFirst(
                Query.query(Criteria.where("_id").is(id).and("otpHash").is(otpHash)),
                new Update().set("ready", ready),
                VerificationDocument.class);
    }

    public void failAttempt(String id, String otpHash, Instant now) {
        mongo.updateFirst(
                Query.query(
                        active(id, now)
                                .and("otpHash")
                                .is(otpHash)
                                .and("ready")
                                .is(true)
                                .and("otpExpiresAt")
                                .gt(now)
                                .and("attempts")
                                .lt(5)),
                new Update().inc("attempts", 1),
                VerificationDocument.class);
    }

    public VerificationEntity consumeOtp(String id, String otpHash, Instant now) {
        return mapper.toEntity(
                mongo.findAndModify(
                        Query.query(
                                active(id, now)
                                        .and("purpose")
                                        .ne("HANDOFF")
                                        .and("otpHash")
                                        .is(otpHash)
                                        .and("ready")
                                        .is(true)
                                        .and("otpExpiresAt")
                                        .gt(now)
                                        .and("attempts")
                                        .lt(5)),
                        new Update().set("consumed", true),
                        VerificationDocument.class));
    }

    public VerificationEntity consumeHandoff(String id, String bindingHash, Instant now) {
        return mapper.toEntity(
                mongo.findAndModify(
                        Query.query(
                                active(id, now)
                                        .and("purpose")
                                        .is("HANDOFF")
                                        .and("bindingHash")
                                        .is(bindingHash)),
                        new Update().set("consumed", true),
                        VerificationDocument.class));
    }
}
