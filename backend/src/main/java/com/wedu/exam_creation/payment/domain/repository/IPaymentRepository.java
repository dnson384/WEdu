package com.wedu.exam_creation.payment.domain.repository;

import com.wedu.exam_creation.payment.domain.entity.PaymentEmailEntity;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;

import java.time.Instant;
import java.util.List;

public interface IPaymentRepository {
    PaymentOrderEntity find(String id);

    PaymentOrderEntity findByIdempotencyKeyHash(String hash);

    PaymentOrderEntity insert(PaymentOrderEntity order);

    boolean update(PaymentOrderEntity order, long expectedRevision);

    boolean claimVerification(String id, String userId, Instant now);

    List<PaymentOrderEntity> expiredPending(Instant now);

    void enqueueEmail(PaymentEmailEntity email);

    PaymentEmailEntity claimEmail(Instant now, String leaseToken);

    void completeEmail(String id, String leaseToken, Instant now, boolean sent);
}
