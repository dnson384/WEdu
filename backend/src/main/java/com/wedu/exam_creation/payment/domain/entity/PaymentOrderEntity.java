package com.wedu.exam_creation.payment.domain.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderEntity {
    private String id;
    private String idempotencyKeyHash;
    private String userId;
    private String email;
    private long amount;
    private int durationDays;
    private String status;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant paidAt;
    private Instant plusExpiresAt;
    private VnpayTransactionEntity vnpayTransaction;
    private long revision;

    public boolean isManualReview() {
        return "SUCCEEDED".equals(status) && plusExpiresAt == null;
    }
}
