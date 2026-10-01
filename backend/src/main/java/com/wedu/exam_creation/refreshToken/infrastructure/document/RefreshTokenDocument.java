package com.wedu.exam_creation.refreshToken.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "RefreshTokens")
public class RefreshTokenDocument {
    @Id
    private String id;

    private String jti;

    @Field(targetType = FieldType.OBJECT_ID)
    private String userId;

    private Instant expiresAt;
    private Instant issuedAt;
}
