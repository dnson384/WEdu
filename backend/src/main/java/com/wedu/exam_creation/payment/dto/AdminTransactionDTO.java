package com.wedu.exam_creation.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

public class AdminTransactionDTO {
    @Data @AllArgsConstructor
    public static class Item {
        private String orderId;
        private String email;
        private long amount;
        private String status;
        private Instant createdAt;
        private String transactionNo;
    }

    @Data @AllArgsConstructor
    public static class Page {
        private List<Item> items;
        private int page;
        private int pageSize;
        private long totalElements;
        private long totalPages;
    }

    @Data @AllArgsConstructor
    public static class Provider {
        private String transactionNo;
        private BigDecimal amount;
        private String bankCode;
        private Instant paidAt;
        private String responseCode;
        private String transactionStatus;
    }

    @Data @AllArgsConstructor
    public static class Lookup {
        private Item order;
        private Provider provider;
        private boolean amountMismatch;
        private Instant queriedAt;
    }
}
