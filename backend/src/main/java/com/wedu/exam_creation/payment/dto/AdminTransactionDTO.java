package com.wedu.exam_creation.payment.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

public class AdminTransactionDTO {
    @Data
    @AllArgsConstructor
    public static class Item {
        private String orderId;
        private String email;
        private long amount;
        private String status;
        private Instant createdAt;
        private String transactionNo;
    }

    @Data
    @AllArgsConstructor
    public static class Page {
        private List<Item> items;
        private int page;
        private int pageSize;
        private long totalElements;
        private long totalPages;
    }
}
