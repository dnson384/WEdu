package com.wedu.exam_creation.auth.infrastructure.document;

import java.time.Instant;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("AuthOtpRateLimits")
public class OtpRateLimitDocument {
    @Id private String id;
    private Instant resendAt;
    private Instant expiresAt;
    private java.util.List<Instant> sentAt = new java.util.ArrayList<>();
    private long revision;
}
