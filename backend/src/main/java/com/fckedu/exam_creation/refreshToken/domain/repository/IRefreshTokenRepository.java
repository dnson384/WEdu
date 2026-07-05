package com.fckedu.exam_creation.refreshToken.domain.repository;

import com.fckedu.exam_creation.refreshToken.domain.entity.RefreshTokenEntity;

import java.util.List;

public interface IRefreshTokenRepository {
    boolean save(RefreshTokenEntity newEntity);

    RefreshTokenEntity getRefreshTokenByJti(String jti, String userId);

    List<RefreshTokenEntity> getRefreshTokenByUserId(String userId);

    boolean delete(String jti);

    boolean deleteMany(List<String> jtis);
}
