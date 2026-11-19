package com.wedu.exam_creation.payment.infrastructure.document;

import java.time.Instant;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Data
@Document("PaymentOrders")
public class PaymentOrderDocument {
    @Id private String id;
    private String idempotencyKeyHash;

    @Field(targetType = FieldType.OBJECT_ID)
    private String userId;

    private String email;
    private long amount;
    private int durationDays;
    private String status;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant paidAt;
    private Instant plusExpiresAt;
    private VnpayTransactionDocument vnpayTransaction;
    private long revision;
}
