package com.wedu.exam_creation.payment.infrastructure.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Data
@Document("RefundRequests")
public class RefundDocument {
    @Id private String id;
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
    private List<EventDocument> history;
    private List<CheckDocument> checks;

    @Data
    public static class EventDocument {
        private Instant at;
        private String status;
        private String actor;
        private String reason;
    }
    @Data
    public static class CheckDocument {
        private String key;
        private String result;
        private String reason;
    }
}
