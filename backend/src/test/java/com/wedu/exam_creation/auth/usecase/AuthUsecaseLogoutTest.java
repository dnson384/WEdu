package com.wedu.exam_creation.auth.usecase;

import com.wedu.exam_creation.auth.dto.mapper.AuthDTOMapper;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.passwordResetToken.usecase.PasswordResetTokenService;
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.user.usecase.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUsecaseLogoutTest {
    private static final String ACCESS_TOKEN = "valid-access-token";
    private static final String AUTHORIZATION = "Bearer " + ACCESS_TOKEN;
    private static final String PARENT_JTI = "jti-123456";

    @Mock
    private AuthDTOMapper mapperDTO;
    @Mock
    private SecurityService securityService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordResetTokenService passwordResetTokenService;
    @Mock
    private UserService userService;

    private AuthUsecase authUsecase;
    private ATPayload accessTokenPayload;

    @BeforeEach
    void setUp() {
        authUsecase = new AuthUsecase(userService, securityService, refreshTokenService,
                passwordResetTokenService, mapperDTO);
        accessTokenPayload = new ATPayload(PARENT_JTI, "user-123", "user@gmail.com", "ROLE_TEACHER");
    }

    @Test
    @DisplayName("Trả về true khi xóa refresh token thành công")
    void should_returnTrue_when_refreshTokenIsDeleted() {
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN)).thenReturn(accessTokenPayload);
        when(refreshTokenService.delete(PARENT_JTI)).thenReturn(true);

        assertThat(authUsecase.logout(AUTHORIZATION)).isTrue();

        verifyTokenParsingAndDeletion();
    }

    @Test
    @DisplayName("Trả về false khi service xóa refresh token trả về false")
    void should_returnFalse_when_refreshTokenDeletionReturnsFalse() {
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN)).thenReturn(accessTokenPayload);
        when(refreshTokenService.delete(PARENT_JTI)).thenReturn(false);

        assertThat(authUsecase.logout(AUTHORIZATION)).isFalse();

        verifyTokenParsingAndDeletion();
    }

    @Test
    @DisplayName("Truyền lỗi không tìm thấy refresh token ra ngoài")
    void should_throwNotFound_when_refreshTokenDoesNotExist() {
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN)).thenReturn(accessTokenPayload);
        NotFoundException failure = new NotFoundException("Không tìm thấy RT");
        when(refreshTokenService.delete(PARENT_JTI)).thenThrow(failure);

        assertThatThrownBy(() -> authUsecase.logout(AUTHORIZATION))
                .isSameAs(failure)
                .hasMessage("Không tìm thấy RT");

        verifyTokenParsingAndDeletion();
    }

    @Test
    @DisplayName("Truyền lỗi dữ liệu không nhất quán khi xóa refresh token ra ngoài")
    void should_throwInternalServer_when_refreshTokenDeletionFails() {
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN)).thenReturn(accessTokenPayload);
        InternalServerException failure = new InternalServerException("Dữ liệu không nhất quán, xóa dư");
        when(refreshTokenService.delete(PARENT_JTI)).thenThrow(failure);

        assertThatThrownBy(() -> authUsecase.logout(AUTHORIZATION))
                .isSameAs(failure)
                .hasMessage("Dữ liệu không nhất quán, xóa dư");

        verifyTokenParsingAndDeletion();
    }

    @Test
    @DisplayName("Không xóa refresh token khi parse access token thất bại")
    void should_notDeleteRefreshToken_when_accessTokenParsingFails() {
        UnAuthorizedException failure = new UnAuthorizedException("Access Token không hợp lệ");
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN)).thenThrow(failure);

        assertThatThrownBy(() -> authUsecase.logout(AUTHORIZATION))
                .isSameAs(failure)
                .hasMessage("Access Token không hợp lệ");

        verify(securityService).getPayloadFromAccessToken(ACCESS_TOKEN);
        verifyNoMoreInteractions(securityService);
        verifyNoInteractions(refreshTokenService, userService, mapperDTO, passwordResetTokenService);
    }

    private void verifyTokenParsingAndDeletion() {
        verify(securityService).getPayloadFromAccessToken(ACCESS_TOKEN);
        verify(refreshTokenService).delete(PARENT_JTI);
        verifyNoMoreInteractions(securityService, refreshTokenService);
        verifyNoInteractions(userService, mapperDTO, passwordResetTokenService);
    }
}
