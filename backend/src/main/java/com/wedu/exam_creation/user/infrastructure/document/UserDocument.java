package com.wedu.exam_creation.user.infrastructure.document;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Users")
public class UserDocument {
    @Id private String id;

    private String email;
    private String hashedPassword;
    private String username;
    private String role;
    private String loginMethod;
    private String avatarUrl;
    private Boolean isActive;
    private String accountType;

    @CreatedDate private Instant createdAt;

    @LastModifiedDate private Instant updatedAt;
    private Instant plusExpiresAt;
    private String googleSubject;
}
