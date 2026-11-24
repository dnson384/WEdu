package com.wedu.exam_creation.auth.domain.repository;

import com.wedu.exam_creation.auth.domain.entity.VerificationEntity;
import java.time.Instant;

public interface IVerificationRepository {
    void insert(VerificationEntity entity);

    VerificationEntity find(String id);

    void reserveEmail(String email, Instant now);

    boolean replaceOtp(VerificationEntity entity, Instant now);

    void markReady(String id, String otpHash, boolean ready);

    void failAttempt(String id, String otpHash, Instant now);

    VerificationEntity consumeOtp(String id, String otpHash, Instant now);

    VerificationEntity consumeHandoff(String id, String bindingHash, Instant now);
}
