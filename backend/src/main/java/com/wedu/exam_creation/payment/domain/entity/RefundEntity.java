package com.wedu.exam_creation.payment.domain.entity;

import lombok.Data;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class RefundEntity {
    private String id;
    private String orderId;
    private String userId;
    private String email;
    private String idempotencyKey;
    private long amount;
    private String status;
    private String reason;
    private String policyVersion;
    private String eligibility;
    private String decisionReason;
    private String adminId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant submittedAt;
    private Instant lastCheckedAt;
    private String requestId;
    private String providerCode;
    private String providerStatus;
    private String refundTransactionNo;
    private boolean pollingStopped;
    private long revision;
    private List<Event> history = new ArrayList<>();
    private List<Check> checks = new ArrayList<>();

    @Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class Check {
        private String key;
        private String result;
        private String reason;
    }

    @Data
    public static class Event {
        private Instant at;
        private String status;
        private String actor;
        private String reason;
    }
}
