package com.wedu.exam_creation.payment.domain.entity;

import java.time.Instant;
import lombok.Data;

@Data
public class PaymentEmailEntity {
    private String id;
    private String email;
    private String orderId;
    private String status;
    private Instant plusExpiresAt;
    private boolean manualReview;
    private int attempts;
    private Instant nextAttemptAt;
    private Instant sentAt;
    private String leaseToken;
}
