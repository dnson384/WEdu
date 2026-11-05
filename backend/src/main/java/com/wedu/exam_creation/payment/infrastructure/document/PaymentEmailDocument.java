package com.wedu.exam_creation.payment.infrastructure.document;
import lombok.Data;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Data
@Document("PaymentEmails")
public class PaymentEmailDocument {
    @Id
    private String id;
    private String email;
    private String orderId;
    private String status;
    private Instant plusExpiresAt;
    private boolean manualReview;
    private int attempts;
    private Instant nextAttemptAt;
    private Instant sentAt;
    private String leaseToken;
}
