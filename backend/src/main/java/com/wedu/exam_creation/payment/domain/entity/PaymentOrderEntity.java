package com.wedu.exam_creation.payment.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
    private String transactionNo;
    private String responseCode;
    private String transactionStatus;
    private String clientIp;
    private boolean manualReview;
    private long revision;
    private Instant lastVerifiedAt;
}
