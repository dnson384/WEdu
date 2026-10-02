package com.wedu.exam_creation.passwordResetToken.infrastructure.mapper;

import com.wedu.exam_creation.passwordResetToken.domain.entity.PasswordResetTokenEntity;
import com.wedu.exam_creation.passwordResetToken.infrastructure.document.PasswordResetTokenDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PasswordResetTokenMapper {
    PasswordResetTokenEntity toEntity(PasswordResetTokenDocument document);

    PasswordResetTokenDocument toDocument(PasswordResetTokenEntity entity);
}
