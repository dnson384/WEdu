package com.wedu.exam_creation.payment.infrastructure.document;

import java.time.Instant;
import lombok.Data;

@Data
public class VnpayTransactionDocument {
    private String transactionNo;
    private String responseCode;
    private String transactionStatus;
    private Instant lastVerifiedAt;
}
