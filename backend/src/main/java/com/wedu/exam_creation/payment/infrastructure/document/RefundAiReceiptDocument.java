package com.wedu.exam_creation.payment.infrastructure.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document("RefundAiReceipts")
public class RefundAiReceiptDocument {
    @Id private String id;
    private String userId;
    private Instant createdAt;
}
