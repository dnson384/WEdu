package com.wedu.exam_creation.refreshToken.usecase;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.token.RTPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.refreshToken.domain.entity.RefreshTokenEntity;
import com.wedu.exam_creation.refreshToken.infrastructure.repository.RefreshTokenRepositoryImpl;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.user.usecase.UserService;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenUsecase {
    private final RefreshTokenRepositoryImpl repo;
    private final UserService userService;
    private final SecurityService securityService;

    public RefreshTokenUsecase(RefreshTokenRepositoryImpl repo, UserService userService, SecurityService securityService) {
        this.repo = repo;
        this.userService = userService;
        this.securityService = securityService;
    }

    public RefreshTokenEntity getRefreshToken(String jti, String userId) {
        RefreshTokenEntity entity = repo.getRefreshTokenByJti(jti, userId);

        if (entity == null) {
            throw new NotFoundException("RT không tồn tại");
        }

        return entity;
    }

    public String generateAccessToken(String refreshToken) {
        if (!securityService.validateRefreshToken(refreshToken)) {
            throw new UnAuthorizedException("RT không hợp lệ");
        }

        try {
            RTPayload rtPayload = securityService.getPayloadFromRefreshToken(refreshToken);

            RefreshTokenEntity token = this.getRefreshToken(rtPayload.getJti(), rtPayload.getUserId());

            CommonUserResponseAllDTO user = userService.findById(token.getUserId());

            if (user == null) {
                throw new NotFoundException("Người dùng không tồn tại");
            }

            ATPayload atPayload = new ATPayload(
                    rtPayload.getJti(), user.getId(), user.getEmail(), user.getRole()
            );

            return securityService.generateAccessToken(atPayload);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnAuthorizedException("RT không hợp lệ");
        }
    }
}
