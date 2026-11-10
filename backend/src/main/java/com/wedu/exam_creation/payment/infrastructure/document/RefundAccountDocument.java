package com.wedu.exam_creation.payment.infrastructure.document;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Data
@Document("RefundAccounts")
public class RefundAccountDocument {
    @MongoId(FieldType.STRING) private String id;
    private Instant trackedSince;
    private Instant lastAiUsedAt;
    private List<String> activeAi;
    private List<String> holds;
    private long revision;
}
