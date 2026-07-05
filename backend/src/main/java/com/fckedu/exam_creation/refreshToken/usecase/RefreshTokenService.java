package com.fckedu.exam_creation.refreshToken.usecase;

import com.fckedu.exam_creation.common.dto.refreshToken.request.NewRTRequestDTO;
import com.fckedu.exam_creation.common.dto.refreshToken.response.RTResponseDTO;
import com.fckedu.exam_creation.refreshToken.domain.entity.RefreshTokenEntity;
import com.fckedu.exam_creation.refreshToken.dto.mapper.RefreshTokenDTOMapper;
import com.fckedu.exam_creation.refreshToken.infrastructure.repository.RefreshTokenRepositoryImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepositoryImpl repo;
    private final RefreshTokenDTOMapper mapper;

    public RefreshTokenService(RefreshTokenRepositoryImpl repo, RefreshTokenDTOMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public List<RTResponseDTO> getUserRefreshToken(String userId) {
        List<RefreshTokenEntity> refreshTokenEntities = repo.getRefreshTokenByUserId(userId);

        return refreshTokenEntities.stream().map(mapper::toResponseDTO).toList();
    }

    public boolean save(NewRTRequestDTO newRT) {
        RefreshTokenEntity entity = new RefreshTokenEntity(
                null,
                newRT.getJti(),
                newRT.getUserId(),
                newRT.getExpiresAt(),
                newRT.getIssuedAt()
        );

        return repo.save(entity);
    }

    public boolean delete(String jti) {
        return repo.delete(jti);
    }

    public boolean deleteMany(List<String> jtis) {
        return repo.deleteMany(jtis);
    }
}
