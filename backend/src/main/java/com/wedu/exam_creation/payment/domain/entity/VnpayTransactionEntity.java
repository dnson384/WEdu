package com.wedu.exam_creation.payment.domain.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VnpayTransactionEntity {
    private String transactionNo;
    private String responseCode;
    private String transactionStatus;
    private Instant lastVerifiedAt;
}
