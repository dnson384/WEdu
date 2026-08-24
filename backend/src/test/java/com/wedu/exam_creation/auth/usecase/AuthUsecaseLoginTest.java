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
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.user.dto.request.LoginUserRequestDTO;
import com.wedu.exam_creation.user.usecase.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private UserService userService;

    private AuthUsecase authUsecase;

    private LoginUserRequestDTO loginRequest;
    private CommonUserResponseAllDTO activeLocalUser;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        authUsecase = new AuthUsecase(userService, securityService, refreshTokenService, mapperDTO);

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
                "FREE"
        );

        userResponseDTO = new UserResponseDTO(
                "user-123",
                "anv@gmail.com",
                "Nguyen Van A",
                "ROLE_TEACHER",
                "avatars/default-avatar-user.png"
        );
    }

    @Nested
    @DisplayName("Success case")
    class SuccessCase {
        @Test
        @DisplayName("Đăng nhập thành công - trả về AT/RT hợp lệ")
        void login_Success() {
            when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(activeLocalUser));
            when(securityService.validatePassword("Password123@", "hashed-password")).thenReturn(true);
            when(mapperDTO.toUserResponseDTO(activeLocalUser)).thenReturn(userResponseDTO);
            when(securityService.generateAccessToken(any(ATPayload.class))).thenReturn("access-token");
            when(securityService.generateRefreshToken(any(RTPayload.class))).thenReturn("refresh-token");
            when(securityService.parseNewRefreshToken("refresh-token"))
                    .thenReturn(mock(NewRTRequestDTO.class));
            when(refreshTokenService.save(any(NewRTRequestDTO.class))).thenReturn(true);

            AuthorizedResponseDTO result = authUsecase.login(loginRequest);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.getUser()).isEqualTo(userResponseDTO);

            verify(userService, times(1)).findByEmail("anv@gmail.com");
            verify(securityService, times(1)).validatePassword("Password123@", "hashed-password");
            verify(refreshTokenService, times(1)).save(any(NewRTRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("Email không tồn tại")
    class EmailNotFoundCase {
        @Test
        @DisplayName("Ném NotFoundException khi email không tồn tại")
        void login_EmailNotFound_ThrowsNotFoundException() {
            when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authUsecase.login(loginRequest))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Tài khoản chưa tồn tại");

            verify(securityService, never()).validatePassword(anyString(), anyString());
            verify(refreshTokenService, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Sai phương thức đăng nhập (GOOGLE)")
    class WrongLoginMethodCase {
        @Test
        @DisplayName("Ném UnAuthorizedException khi tài khoản đăng ký bằng GOOGLE")
        void login_GoogleAccount_ThrowsUnAuthorizedException() {
            CommonUserResponseAllDTO googleUser = new CommonUserResponseAllDTO(
                    "user-456",
                    "anv@gmail.com",
                    null,
                    "Nguyen Van A",
                    "ROLE_TEACHER",
                    "GOOGLE",
                    "avatars/default-avatar-user.png",
                    true,
                    "FREE"
            );
            when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(googleUser));

            assertThatThrownBy(() -> authUsecase.login(loginRequest))
                    .isInstanceOf(UnAuthorizedException.class)
                    .hasMessage("Sai phương thức đăng nhập");

            // Không được check password hay tạo token khi loginMethod sai
            verify(securityService, never()).validatePassword(anyString(), anyString());
            verify(securityService, never()).generateAccessToken(any());
        }
    }

    @Nested
    @DisplayName("Tài khoản bị khóa")
    class LockedAccountCase {
        @Test
        @DisplayName("Ném UnAuthorizedException khi tài khoản đã bị khóa (isActive = false)")
        void login_LockedAccount_ThrowsUnAuthorizedException() {
            CommonUserResponseAllDTO lockedUser = new CommonUserResponseAllDTO(
                    "user-789",
                    "anv@gmail.com",
                    "hashed-password",
                    "Nguyen Van A",
                    "ROLE_TEACHER",
                    "LOCAL",
                    "avatars/default-avatar-user.png",
                    false, // isActive = false
                    "FREE"
            );
            when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(lockedUser));

            assertThatThrownBy(() -> authUsecase.login(loginRequest))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Tài khoản đã bị khóa! Vui lòng liên hệ xxx để được mở khóa");

            verify(securityService, never()).validatePassword(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Sai mật khẩu")
    class WrongPasswordCase {
        @Test
        @DisplayName("Ném UnAuthorizedException khi mật khẩu không đúng")
        void login_WrongPassword_ThrowsUnAuthorizedException() {
            when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(activeLocalUser));
            when(securityService.validatePassword("Password123@", "hashed-password")).thenReturn(false);

            assertThatThrownBy(() -> authUsecase.login(loginRequest))
                    .isInstanceOf(UnAuthorizedException.class)
                    .hasMessage("Mật khẩu không chính xác");

            verify(mapperDTO, never()).toUserResponseDTO(any());
            verify(securityService, never()).generateAccessToken(any());
            verify(refreshTokenService, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Lưu Refresh Token thất bại")
    class SaveRefreshTokenFailureCase {
        @Test
        @DisplayName("Ném InternalServerException khi lưu RT thất bại")
        void login_SaveRTFails_ThrowsInternalServerException() {
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
                    .hasMessage("Lỗi trong quá trình lưu RT!");
        }
    }
}
