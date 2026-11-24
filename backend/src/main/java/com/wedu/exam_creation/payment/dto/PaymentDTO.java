package com.wedu.exam_creation.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

public final class PaymentDTO {
    private PaymentDTO() {
    }

    @Data
    @NoArgsConstructor
    public static class ReturnConfirmation {
        // Pairs preserve duplicate URL keys so the use case can reject ambiguity.
        private List<List<String>> parameters;
    }

    @Data
    @AllArgsConstructor
    public static class Plan {
        private long amount;
        private String currency;
        private int durationDays;
        private boolean available;
    }

    @Data
    @AllArgsConstructor
    public static class Order {
        private String orderId;
        private long amount;
        private String status;
        private Instant expiresAt;
        private Instant plusExpiresAt;
        private String paymentUrl;
        private boolean manualReview;
    }
}
