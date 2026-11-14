package com.wedu.exam_creation.auth.infrastructure.document;

import java.time.Instant;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@ToString(onlyExplicitlyIncluded = true)
@Document("AuthVerifications")
public class VerificationDocument {
    @Id private String id;
    private String purpose;
    private String email;
    private String username;
    private String hashedPassword;
    private String googleSubject;
    private String userId;
    private String bindingHash;
    private boolean authoritativeEmail;
    private String otpHash;
    private int attempts;
    private boolean ready;
    private boolean consumed;
    private long revision;
    private Instant expiresAt;
    private Instant otpExpiresAt;
    private Instant resendAt;
}
