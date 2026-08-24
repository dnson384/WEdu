package com.wedu.exam_creation.auth.usecase;

import com.wedu.exam_creation.auth.dto.mapper.AuthDTOMapper;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.token.RTPayload;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.user.usecase.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthUsecaseLogoutTest {
    private static final String EMAIL = "user@gmail.com";
    private final String VALID_AT = "valid-access-token";
    private final String INVALID_AT = "invalid-access-token";
    private final String VALID_RT = "valid-refresh-token";
    private final String INVALID_RT = "invalid-refresh-token";
    private final String VALID_JTI = "jti-123456";
    private final String VALID_USER_ID = "user-123";
    private RTPayload mockRTPayload;
    private ATPayload mockATPayload;

    @Mock
    private AuthDTOMapper mapperDTO;
    @Mock
    private SecurityService securityService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private UserService userService;

    private AuthUsecase authUsecase;

    @BeforeEach
    void setUp() {
        authUsecase = new AuthUsecase(userService, securityService, refreshTokenService, mapperDTO);

        mockRTPayload = new RTPayload(VALID_JTI, VALID_USER_ID, EMAIL, "ROLE_TEACHER");
        mockATPayload = new ATPayload(VALID_JTI, VALID_USER_ID, EMAIL, "ROLE_TEACHER");
    }

    @Test
    @DisplayName("Đăng xuất thành công - RT tồn tại và bị xóa thành công")
    void success() {
        // Given
        when(securityService.validateRefreshToken(VALID_RT))
                .thenReturn(true);
        when(securityService.validateAccessToken(VALID_AT))
                .thenReturn(true);
        when(securityService.getPayloadFromRefreshToken(VALID_RT))
                .thenReturn(mockRTPayload);
        when(securityService.getPayloadFromAccessToken(VALID_AT))
                .thenReturn(mockATPayload);
        when(refreshTokenService.exists(VALID_JTI, VALID_USER_ID))
                .thenReturn(true);
        when(refreshTokenService.delete(VALID_JTI))
                .thenReturn(true);

        // When
        boolean result = authUsecase.logout(VALID_AT, VALID_RT);

        // Then
        assertThat(result).isTrue();
        verify(securityService, times(1)).validateRefreshToken(VALID_RT);
        verify(securityService, times(1)).validateAccessToken(VALID_AT);
        verify(securityService, times(1)).getPayloadFromAccessToken(VALID_AT);
        verify(securityService, times(1)).getPayloadFromRefreshToken(VALID_RT);
        verify(refreshTokenService, times(1)).exists(VALID_JTI, VALID_USER_ID);
        verify(refreshTokenService, times(1)).delete(VALID_JTI);
    }

    @Test
    @DisplayName("AT không hợp lệ")
    void invalidAT() {
        // Given
        when(securityService.validateRefreshToken(VALID_RT)).thenReturn(true);
        when(securityService.validateAccessToken(INVALID_AT)).thenReturn(false);

        // When
        assertThatThrownBy(() -> authUsecase.logout(INVALID_AT, VALID_RT))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("AT không hợp lệ");

        // Then
        verify(securityService, never()).getPayloadFromAccessToken(anyString());
        verify(securityService, never()).getPayloadFromRefreshToken(anyString());
        verify(refreshTokenService, never()).exists(anyString(), anyString());
        verify(refreshTokenService, never()).delete(anyString());
    }

    @Test
    @DisplayName("AT null")
    void nullAT() {
        when(securityService.validateRefreshToken(VALID_RT)).thenReturn(true);

        // When
        assertThatThrownBy(() -> authUsecase.logout(null, VALID_RT))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("AT không hợp lệ");

        // Then
        verify(securityService, times(1)).validateRefreshToken(VALID_RT);
        verify(securityService, never()).validateAccessToken(anyString());
        verify(securityService, never()).getPayloadFromAccessToken(anyString());
        verify(securityService, never()).getPayloadFromRefreshToken(anyString());
        verify(refreshTokenService, never()).exists(anyString(), anyString());
        verify(refreshTokenService, never()).delete(anyString());
    }

    @Test
    @DisplayName("RT không hợp lệ")
    void invalidRT() {
        // Given
        when(securityService.validateRefreshToken(INVALID_RT)).thenReturn(false);

        // When
        assertThatThrownBy(() -> authUsecase.logout(VALID_AT, INVALID_RT))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("RT không hợp lệ");

        // Then
        verify(securityService, never()).validateAccessToken(anyString());
        verify(securityService, never()).getPayloadFromAccessToken(anyString());
        verify(securityService, never()).getPayloadFromRefreshToken(anyString());
        verify(refreshTokenService, never()).exists(anyString(), anyString());
        verify(refreshTokenService, never()).delete(anyString());
    }

    @Test
    @DisplayName("RT null")
    void nullRT() {
        // When
        assertThatThrownBy(() -> authUsecase.logout(VALID_AT, null))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("RT không hợp lệ");

        // Then
        verify(securityService, never()).validateAccessToken(anyString());
        verify(securityService, never()).validateRefreshToken(anyString());
        verify(securityService, never()).getPayloadFromAccessToken(anyString());
        verify(securityService, never()).getPayloadFromRefreshToken(anyString());
        verify(refreshTokenService, never()).exists(anyString(), anyString());
        verify(refreshTokenService, never()).delete(anyString());
    }

    @Test
    @DisplayName("userId không trùng khớp")
    void misMatchUserId() {
        ATPayload misMatchUserId = new ATPayload(
                VALID_JTI,
                "mismatch-user-id",
                "mismatch-email",
                "ROLE_TEACHER"
        );

        // Given
        when(securityService.validateRefreshToken(VALID_RT))
                .thenReturn(true);
        when(securityService.validateAccessToken(VALID_AT))
                .thenReturn(true);
        when(securityService.getPayloadFromRefreshToken(VALID_RT))
                .thenReturn(mockRTPayload);
        when(securityService.getPayloadFromAccessToken(VALID_AT))
                .thenReturn(misMatchUserId);

        // When
        assertThatThrownBy(() -> authUsecase.logout(VALID_AT, VALID_RT))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("userId không trùng khớp");

        // Then
        verify(securityService, times(1)).validateAccessToken(VALID_AT);
        verify(securityService, times(1)).validateRefreshToken(VALID_RT);
        verify(securityService, times(1)).getPayloadFromAccessToken(VALID_AT);
        verify(securityService, times(1)).getPayloadFromRefreshToken(VALID_RT);
        verify(refreshTokenService, never()).exists(anyString(), anyString());
        verify(refreshTokenService, never()).delete(anyString());
    }

    @Test
    @DisplayName("Phiên không trùng khớp")
    void misMatchSession() {
        ATPayload misMatchSession = new ATPayload(
                "mismatch-jti",
                VALID_USER_ID,
                EMAIL,
                "ROLE_TEACHER"
        );

        // Given
        when(securityService.validateRefreshToken(VALID_RT))
                .thenReturn(true);
        when(securityService.validateAccessToken(VALID_AT))
                .thenReturn(true);
        when(securityService.getPayloadFromRefreshToken(VALID_RT))
                .thenReturn(mockRTPayload);
        when(securityService.getPayloadFromAccessToken(VALID_AT))
                .thenReturn(misMatchSession);

        // When
        assertThatThrownBy(() -> authUsecase.logout(VALID_AT, VALID_RT))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("Phiên không trùng khớp");

        // Then
        verify(securityService, times(1)).validateAccessToken(VALID_AT);
        verify(securityService, times(1)).validateRefreshToken(VALID_RT);
        verify(securityService, times(1)).getPayloadFromAccessToken(VALID_AT);
        verify(securityService, times(1)).getPayloadFromRefreshToken(VALID_RT);
        verify(refreshTokenService, never()).exists(anyString(), anyString());
        verify(refreshTokenService, never()).delete(anyString());
    }

    @Test
    @DisplayName("RT không tồn tại")
    void nonExitedRT() {
        // Given
        when(securityService.validateRefreshToken(VALID_RT))
                .thenReturn(true);
        when(securityService.validateAccessToken(VALID_AT))
                .thenReturn(true);
        when(securityService.getPayloadFromRefreshToken(VALID_RT))
                .thenReturn(mockRTPayload);
        when(securityService.getPayloadFromAccessToken(VALID_AT))
                .thenReturn(mockATPayload);
        when(refreshTokenService.exists(mockRTPayload.getJti(), mockRTPayload.getUserId()))
                .thenReturn(false);

        // When
        assertThatThrownBy(() -> authUsecase.logout(VALID_AT, VALID_RT))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("RT không tồn tại");

        // Then
        verify(securityService, times(1)).validateAccessToken(VALID_AT);
        verify(securityService, times(1)).validateRefreshToken(VALID_RT);
        verify(securityService, times(1)).getPayloadFromAccessToken(VALID_AT);
        verify(securityService, times(1)).getPayloadFromRefreshToken(VALID_RT);
        verify(refreshTokenService, times(1)).exists(VALID_JTI, VALID_USER_ID);
        verify(refreshTokenService, never()).delete(anyString());
    }

    @Test
    @DisplayName("Xóa RT thất bại")
    void deleteRTFailure() {
        // Given
        when(securityService.validateRefreshToken(VALID_RT)).thenReturn(true);
        when(securityService.validateAccessToken(VALID_AT)).thenReturn(true);
        when(securityService.getPayloadFromRefreshToken(VALID_RT)).thenReturn(mockRTPayload);
        when(securityService.getPayloadFromAccessToken(VALID_AT)).thenReturn(mockATPayload);
        when(refreshTokenService.exists(VALID_JTI, VALID_USER_ID)).thenReturn(true);

        doThrow(new InternalServerException("Dữ liệu không nhất quán, xóa dư!"))
                .when(refreshTokenService).delete(VALID_JTI);

        // When & Then
        assertThatThrownBy(() -> authUsecase.logout(VALID_AT, VALID_RT))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Dữ liệu không nhất quán, xóa dư!");
    }
}
