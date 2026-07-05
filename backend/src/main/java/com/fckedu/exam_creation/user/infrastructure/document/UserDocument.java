package com.fckedu.exam_creation.user.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Users")
public class UserDocument {
    @Id
    private String id;

    private String email;
    private String hashedPassword;
    private String username;
    private String role;
    private String loginMethod;
    private String avatarUrl;
    private Boolean isActive;
    private String accountType;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
