package com.wedu.exam_creation.passwordResetToken.domain.repository;

import com.wedu.exam_creation.passwordResetToken.domain.entity.PasswordResetTokenEntity;

public interface IPasswordResetTokenRepository {
    void deleteByUserId(String userId);

    void save(PasswordResetTokenEntity entity);

    PasswordResetTokenEntity findByTokenHashAndUsedFalse(String tokenHash);
}
