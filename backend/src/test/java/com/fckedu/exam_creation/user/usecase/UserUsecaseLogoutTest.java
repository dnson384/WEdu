package com.fckedu.exam_creation.user.usecase;

import com.fckedu.exam_creation.common.dto.token.RTPayload;
import com.fckedu.exam_creation.common.exception.InternalServerException;
import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.common.exception.UnAuthorizedException;
import com.fckedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.fckedu.exam_creation.security.service.SecurityService;
import com.fckedu.exam_creation.storage.service.S3Service;
import com.fckedu.exam_creation.user.dto.mapper.UserDTOMapper;
import com.fckedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
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
public class UserUsecaseLogoutTest {
    private final String VALID_RT = "valid-refresh-token";
    private final String INVALID_RT = "invalid-refresh-token";
    private final String VALID_JTI = "jti-123456";
    private final String VALID_USER_ID = "user-123";
    private RTPayload mockPayload;

    @Mock
    private UserRepositoryImpl repo;
    @Mock
    private UserDTOMapper mapperDTO;
    @Mock
    private SecurityService securityService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private S3Service s3Service;

    private UserUsecase userUsecase;

    @BeforeEach
    void setUp() {
        userUsecase = new UserUsecase(repo, mapperDTO, securityService, refreshTokenService, s3Service);

        mockPayload = new RTPayload();
        mockPayload.setJti(VALID_JTI);
        mockPayload.setUserId(VALID_USER_ID);
    }

    @Test
    @DisplayName("Đăng xuất thành công - RT tồn tại và bị xóa thành công")
    void success() {
        // Given
        when(securityService.validateRefreshToken(VALID_RT))
                .thenReturn(true);
        when(securityService.getPayloadFromRefreshToken(VALID_RT)).thenReturn(mockPayload);
        when(refreshTokenService.exists(VALID_JTI, VALID_USER_ID)).thenReturn(true);
        when(refreshTokenService.delete(VALID_JTI)).thenReturn(true);

        // When
        boolean result = userUsecase.logout(VALID_RT);

        // Then
        assertThat(result).isTrue();
        verify(securityService, times(1)).validateRefreshToken(VALID_RT);
        verify(securityService, times(1)).getPayloadFromRefreshToken(VALID_RT); // Gọi 2 lần theo code thực tế của bạn
        verify(refreshTokenService, times(1)).exists(VALID_JTI, VALID_USER_ID);
        verify(refreshTokenService, times(1)).delete(VALID_JTI);
    }

    @Test
    @DisplayName("RT không hợp lệ")
    void invalidRT() {
        // Given
        when(securityService.validateRefreshToken(INVALID_RT)).thenReturn(false);

        // When
        assertThatThrownBy(() -> userUsecase.logout(INVALID_RT))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("RT không hợp lệ");

        // Then
        verify(refreshTokenService, never()).exists(anyString(), anyString());
    }

    @Test
    @DisplayName("RT null")
    void nullRT() {
        // When
        assertThatThrownBy(() -> userUsecase.logout(null))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("RT không hợp lệ");

        // Then
        verify(refreshTokenService, never()).exists(anyString(), anyString());
    }

    @Test
    @DisplayName("RT không tồn tại")
    void nonExitedRT() {
        // Given
        when(securityService.validateRefreshToken(VALID_RT))
                .thenReturn(true);
        when(securityService.getPayloadFromRefreshToken(VALID_RT))
                .thenReturn(mockPayload);
        when(refreshTokenService.exists(mockPayload.getJti(), mockPayload.getUserId()))
                .thenReturn(false);

        // When
        assertThatThrownBy(() -> userUsecase.logout(VALID_RT))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("RT không tồn tại");

        // Then
        verify(refreshTokenService, never()).delete(anyString());
    }

    @Test
    @DisplayName("Xóa RT thất bại")
    void deleteRTFailure() {
        // Given
        when(securityService.validateRefreshToken(VALID_RT)).thenReturn(true);
        when(securityService.getPayloadFromRefreshToken(VALID_RT)).thenReturn(mockPayload);
        when(refreshTokenService.exists(VALID_JTI, VALID_USER_ID)).thenReturn(true);

        // Giả lập hệ thống gặp lỗi kết nối đột ngột
        doThrow(new InternalServerException("Dữ liệu không nhất quán, xóa dư!"))
                .when(refreshTokenService).delete(VALID_JTI);

        // When & Then
        assertThatThrownBy(() -> userUsecase.logout(VALID_RT))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Dữ liệu không nhất quán, xóa dư!");
    }
}
