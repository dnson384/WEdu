package com.wedu.exam_creation.auth.usecase;

import com.wedu.exam_creation.auth.dto.mapper.AuthDTOMapper;
import com.wedu.exam_creation.auth.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.common.dto.refreshToken.request.NewRTRequestDTO;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.token.RTPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.passwordResetToken.usecase.PasswordResetTokenService;
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.user.dto.request.LoginUserRequestDTO;
import com.wedu.exam_creation.user.usecase.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthUsecaseLoginTest {
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

    private LoginUserRequestDTO loginRequest;
    private CommonUserResponseAllDTO activeLocalUser;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        authUsecase = new AuthUsecase(userService, securityService, refreshTokenService, passwordResetTokenService, mapperDTO);

        loginRequest = new LoginUserRequestDTO();
        loginRequest.setEmail("anv@gmail.com");
        loginRequest.setPlainPassword("Password123@");

        activeLocalUser = new CommonUserResponseAllDTO(
                "user-123",
                "anv@gmail.com",
                "hashed-password",
                "Nguyen Van A",
                "ROLE_TEACHER",
                "LOCAL",
                "avatars/default-avatar-user.png",
                true, // isActive
                "FREE",
                Instant.now(),
                Instant.now()
        );

        userResponseDTO = new UserResponseDTO(
                "user-123",
                "anv@gmail.com",
                "Nguyen Van A",
                "ROLE_TEACHER",
                "avatars/default-avatar-user.png"
        );
    }

    @Test
    @DisplayName("200 - Đăng nhập thành công")
    void should_returnTokens_when_credentialsAreValid() {
        when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(activeLocalUser));
        when(securityService.validatePassword("Password123@", "hashed-password")).thenReturn(true);
        when(mapperDTO.toUserResponseDTO(activeLocalUser)).thenReturn(userResponseDTO);
        when(securityService.generateAccessToken(any(ATPayload.class))).thenReturn("access-token");
        when(securityService.generateRefreshToken(any(RTPayload.class))).thenReturn("refresh-token");
        NewRTRequestDTO parsedToken = mock(NewRTRequestDTO.class);
        when(securityService.parseNewRefreshToken("refresh-token")).thenReturn(parsedToken);
        when(refreshTokenService.save(parsedToken)).thenReturn(true);

        AuthorizedResponseDTO result = authUsecase.login(loginRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getUser()).isEqualTo(userResponseDTO);

        verify(userService, times(1)).findByEmail("anv@gmail.com");
        verify(securityService, times(1)).validatePassword("Password123@", "hashed-password");
        verify(securityService).parseNewRefreshToken("refresh-token");
        verify(refreshTokenService).save(same(parsedToken));

        ArgumentCaptor<ATPayload> atCaptor = ArgumentCaptor.forClass(ATPayload.class);
        ArgumentCaptor<RTPayload> rtCaptor = ArgumentCaptor.forClass(RTPayload.class);
        verify(securityService).generateAccessToken(atCaptor.capture());
        verify(securityService).generateRefreshToken(rtCaptor.capture());

        ATPayload at = atCaptor.getValue();
        RTPayload rt = rtCaptor.getValue();
        assertThat(at.getUserId()).isEqualTo(activeLocalUser.getId());
        assertThat(at.getEmail()).isEqualTo(activeLocalUser.getEmail());
        assertThat(at.getRole()).isEqualTo(activeLocalUser.getRole());
        assertThat(rt.getUserId()).isEqualTo(activeLocalUser.getId());
        assertThat(rt.getEmail()).isEqualTo(activeLocalUser.getEmail());
        assertThat(rt.getRole()).isEqualTo(activeLocalUser.getRole());
        assertThat(rt.getJti()).isNotBlank();
        assertThat(at.getParentJti()).isEqualTo(rt.getJti());
    }


    @Test
    @DisplayName("404 - Email không tồn tại")
    void should_throwNotFound_when_emailDoesNotExist() {
        when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authUsecase.login(loginRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Tài khoản chưa tồn tại");

        verifyNoInteractions(securityService, mapperDTO, refreshTokenService);
    }


    @Test
    @DisplayName("401 - Tài khoản đăng ký bằng GOOGLE cố gắng đăng nhập bằng LOCAL")
    void should_throwUnauthorized_when_loginMethodIsGoogle() {
        CommonUserResponseAllDTO googleUser = new CommonUserResponseAllDTO(
                "user-456",
                "anv@gmail.com",
                null,
                "Nguyen Van A",
                "ROLE_TEACHER",
                "GOOGLE",
                "avatars/default-avatar-user.png",
                true,
                "FREE",
                Instant.now(),
                Instant.now()
        );
        when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(googleUser));

        assertThatThrownBy(() -> authUsecase.login(loginRequest))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("Sai phương thức đăng nhập");

        verifyNoInteractions(securityService, mapperDTO, refreshTokenService);
    }


    @Test
    @DisplayName("403 - tài khoản đã bị khóa")
    void should_throwForbidden_when_accountIsLocked() {
        CommonUserResponseAllDTO lockedUser = new CommonUserResponseAllDTO(
                "user-789",
                "anv@gmail.com",
                "hashed-password",
                "Nguyen Van A",
                "ROLE_TEACHER",
                "LOCAL",
                "avatars/default-avatar-user.png",
                false,
                "FREE",
                Instant.now(),
                Instant.now()
        );
        when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(lockedUser));

        assertThatThrownBy(() -> authUsecase.login(loginRequest))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Tài khoản đã bị khóa! Vui lòng liên hệ xxx để được mở khóa");

        verifyNoInteractions(securityService, mapperDTO, refreshTokenService);
    }


    @Test
    @DisplayName("401 - mật khẩu không đúng")
    void should_throwUnauthorized_when_passwordIsIncorrect() {
        when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(activeLocalUser));
        when(securityService.validatePassword("Password123@", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authUsecase.login(loginRequest))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("Mật khẩu không chính xác");

        verify(securityService).validatePassword("Password123@", "hashed-password");
        verifyNoMoreInteractions(securityService);
        verifyNoInteractions(mapperDTO, refreshTokenService);
    }

    @Test
    @DisplayName("500 - lưu RT thất bại")
    void should_throwInternalServer_when_savingRefreshTokenFails() {
        when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(activeLocalUser));
        when(securityService.validatePassword("Password123@", "hashed-password")).thenReturn(true);
        when(mapperDTO.toUserResponseDTO(activeLocalUser)).thenReturn(userResponseDTO);
        when(securityService.generateAccessToken(any(ATPayload.class))).thenReturn("access-token");
        when(securityService.generateRefreshToken(any(RTPayload.class))).thenReturn("refresh-token");
        when(securityService.parseNewRefreshToken("refresh-token"))
                .thenReturn(mock(NewRTRequestDTO.class));
        when(refreshTokenService.save(any(NewRTRequestDTO.class))).thenReturn(false);

        assertThatThrownBy(() -> authUsecase.login(loginRequest))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Lỗi trong quá trình đăng nhập");
    }
}
