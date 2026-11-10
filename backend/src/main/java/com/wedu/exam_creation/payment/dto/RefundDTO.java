package com.wedu.exam_creation.payment.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

public class RefundDTO {
    @Data @AllArgsConstructor public static class Event {
        private Instant at;
        private String status;
        private String actor;
        private String reason;
    }
    @Data @AllArgsConstructor public static class Check {
        private String key;
        private String result;
        private String reason;
    }
    @Data public static class Create { private String reason; private String policyVersion; }
    @Data public static class Decision {
        private Long revision;
        private String reason;
        private boolean legacyException;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Item {
        private String id;
        private String orderId;
        private String email;
        private long amount;
        private String status;
        private String reason;
        private String policyVersion;
        private String eligibility;
        private String decisionReason;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant paidAt;
        private Instant lastAiUsedAt;
        private boolean activeAi;
        private Instant projectedPlusExpiresAt;
        private boolean entitlementVerifiable;
        private String providerCode;
        private String providerStatus;
        private String refundTransactionNo;
        private long revision;
        private List<Event> history;
        private List<Check> checks;
    }
    @Data @AllArgsConstructor public static class Page {
        private List<Item> items;
        private int page;
        private int pageSize;
        private long totalElements;
        private long totalPages;
        private boolean enabled;
        private String policyVersion;
    }
    @Data @AllArgsConstructor public static class Order {
        private String id;
        private long amount;
        private String status;
        private Instant createdAt;
        private Instant paidAt;
    }
    @Data @AllArgsConstructor public static class Orders {
        private List<Order> items;
        private int page;
        private boolean hasNext;
    }
}
