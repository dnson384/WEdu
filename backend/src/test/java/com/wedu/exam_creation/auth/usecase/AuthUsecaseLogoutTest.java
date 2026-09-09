package com.wedu.exam_creation.auth.usecase;

import com.wedu.exam_creation.auth.dto.mapper.AuthDTOMapper;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
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
    private final String VALID_JTI = "jti-123456";
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

        mockATPayload = new ATPayload(VALID_JTI, "user-123", EMAIL, "ROLE_TEACHER");
    }

    @Test
    @DisplayName("200 - Đăng xuất thành công & RT bị xóa thành công")
    void should_logoutSuccessfully_when_refreshTokenIsValid() {
        String authorizationHeader = "Bearer " + VALID_AT;
        // Given
        when(securityService.getPayloadFromAccessToken(VALID_AT))
                .thenReturn(mockATPayload);
        when(refreshTokenService.delete(VALID_JTI))
                .thenReturn(true);

        // When
        boolean result = authUsecase.logout(authorizationHeader);

        // Then
        assertThat(result).isTrue();
        verify(securityService, times(1)).getPayloadFromAccessToken(VALID_AT);
        verify(refreshTokenService, times(1)).delete(VALID_JTI);
    }

    @Test
    @DisplayName("404 - Xoá RT thấy bại do không tìm thấy")
    void should_returnNotFound_when_refreshTokenDoesNotExist() {
        String authorizationHeader = "Bearer " + VALID_AT;
        String notFoundJti = mockATPayload.getParentJti();

        // Given
        when(securityService.getPayloadFromAccessToken(VALID_AT))
                .thenReturn(mockATPayload);
        doThrow(new NotFoundException("Không tìm thấy RT"))
                .when(refreshTokenService).delete(notFoundJti);

        // When
        assertThatThrownBy(() -> authUsecase.logout(authorizationHeader))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Không tìm thấy RT");

        // Then
        verify(securityService, times(1)).getPayloadFromAccessToken(anyString());
        verify(refreshTokenService, times(1)).delete(anyString());
    }

    @Test
    @DisplayName("500 - Xóa RT thất bại do xóa dư")
    void should_returnInternalServerError_when_deletedRefreshTokenCountExceedsExpected() {
        String authorizationHeader = "Bearer " + VALID_AT;
        String dupJti = mockATPayload.getParentJti();

        // Given
        when(securityService.getPayloadFromAccessToken(VALID_AT)).thenReturn(mockATPayload);
        doThrow(new InternalServerException("Dữ liệu không nhất quán, xóa dư"))
                .when(refreshTokenService).delete(dupJti);

        // When & Then
        assertThatThrownBy(() -> authUsecase.logout(authorizationHeader))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Dữ liệu không nhất quán, xóa dư");

        verify(securityService, times(1)).getPayloadFromAccessToken(anyString());
        verify(refreshTokenService, times(1)).delete(anyString());
    }
}
