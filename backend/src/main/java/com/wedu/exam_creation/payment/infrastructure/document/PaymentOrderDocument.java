package com.wedu.exam_creation.payment.infrastructure.document;

import lombok.Data;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("PaymentOrders")
public class PaymentOrderDocument {
    @Id
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
