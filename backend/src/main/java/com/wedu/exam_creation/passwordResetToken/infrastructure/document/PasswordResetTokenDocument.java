package com.wedu.exam_creation.passwordResetToken.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "PasswordResetToken")
public class PasswordResetTokenDocument {
    @Id
    private String id;

    private String tokenHash;
    private String userId;

    @Indexed(expireAfter = "10m")
    private Instant expiryDate;

    private boolean used;
    private Instant createdAt;
}
