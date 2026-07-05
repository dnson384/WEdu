package com.fckedu.exam_creation.refreshToken.usecase;

import com.fckedu.exam_creation.common.dto.token.ATPayload;
import com.fckedu.exam_creation.common.dto.token.RTPayload;
import com.fckedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.common.exception.UnAuthorizedException;
import com.fckedu.exam_creation.refreshToken.domain.entity.RefreshTokenEntity;
import com.fckedu.exam_creation.refreshToken.infrastructure.repository.RefreshTokenRepositoryImpl;
import com.fckedu.exam_creation.security.service.SecurityService;
import com.fckedu.exam_creation.user.usecase.UserService;
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
        return repo.getRefreshTokenByJti(jti, userId);
    }

    public String generateAccessToken(String refreshToken) {
        if (!securityService.validateRefreshToken(refreshToken)) {
            throw new UnAuthorizedException("RT không hợp lệ");
        }

        RTPayload rtPayload = securityService.getPayloadFromRefreshToken(refreshToken);

        RefreshTokenEntity token = this.getRefreshToken(rtPayload.getJti(), rtPayload.getUserId());

        CommonUserResponseAllDTO user = userService.findById(token.getUserId());

        if (user == null) {
            throw new NotFoundException("Người dùng không tồn tại");
        }

        ATPayload atPayload = new ATPayload(
                user.getId(), user.getEmail(), user.getRole()
        );

        return securityService.generateAccessToken(atPayload);
    }
}
