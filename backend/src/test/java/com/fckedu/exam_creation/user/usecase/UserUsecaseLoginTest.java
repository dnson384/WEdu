package com.fckedu.exam_creation.user.usecase;

import com.fckedu.exam_creation.common.dto.refreshToken.request.NewRTRequestDTO;
import com.fckedu.exam_creation.common.dto.token.ATPayload;
import com.fckedu.exam_creation.common.dto.token.RTPayload;
import com.fckedu.exam_creation.common.exception.ForbiddenException;
import com.fckedu.exam_creation.common.exception.InternalServerException;
import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.common.exception.UnAuthorizedException;
import com.fckedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.fckedu.exam_creation.security.service.SecurityService;
import com.fckedu.exam_creation.storage.service.S3Service;
import com.fckedu.exam_creation.user.domain.entity.UserEntity;
import com.fckedu.exam_creation.user.dto.mapper.UserDTOMapper;
import com.fckedu.exam_creation.user.dto.request.LoginUserRequestDTO;
import com.fckedu.exam_creation.user.dto.response.AuthorizedResponseDTO;
import com.fckedu.exam_creation.user.dto.response.UserResponseDTO;
import com.fckedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
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
public class UserUsecaseLoginTest {
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

    private LoginUserRequestDTO loginRequest;
    private UserEntity activeLocalUser;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        userUsecase = new UserUsecase(repo, mapperDTO, securityService, refreshTokenService, s3Service);

        loginRequest = new LoginUserRequestDTO();
        loginRequest.setEmail("anv@gmail.com");
        loginRequest.setPlainPassword("Password123@");

        activeLocalUser = new UserEntity(
                "user-123",
                "anv@gmail.com",
                "hashed-password",
                "Nguyen Van A",
                "ROLE_TEACHER",
                "LOCAL",
                "avatars/default-avatar-user.png",
                true, // isActive
                "Free",
                null,
                null
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
            when(repo.findByEmail("anv@gmail.com")).thenReturn(Optional.of(activeLocalUser));
            when(securityService.validatePassword("Password123@", "hashed-password")).thenReturn(true);
            when(mapperDTO.toUserResponseDTO(activeLocalUser)).thenReturn(userResponseDTO);
            when(securityService.generateAccessToken(any(ATPayload.class))).thenReturn("access-token");
            when(securityService.generateRefreshToken(any(RTPayload.class))).thenReturn("refresh-token");
            when(securityService.parseNewRefreshToken("refresh-token"))
                    .thenReturn(mock(NewRTRequestDTO.class));
            when(refreshTokenService.save(any(NewRTRequestDTO.class))).thenReturn(true);

            AuthorizedResponseDTO result = userUsecase.login(loginRequest);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.getUser()).isEqualTo(userResponseDTO);

            verify(repo, times(1)).findByEmail("anv@gmail.com");
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
            when(repo.findByEmail("anv@gmail.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userUsecase.login(loginRequest))
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
            UserEntity googleUser = new UserEntity(
                    "user-456",
                    "anv@gmail.com",
                    null,
                    "Nguyen Van A",
                    "ROLE_TEACHER",
                    "GOOGLE",
                    "avatars/default-avatar-user.png",
                    true,
                    "Free",
                    null,
                    null
            );
            when(repo.findByEmail("anv@gmail.com")).thenReturn(Optional.of(googleUser));

            assertThatThrownBy(() -> userUsecase.login(loginRequest))
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
            UserEntity lockedUser = new UserEntity(
                    "user-789",
                    "anv@gmail.com",
                    "hashed-password",
                    "Nguyen Van A",
                    "ROLE_TEACHER",
                    "LOCAL",
                    "avatars/default-avatar-user.png",
                    false, // isActive = false
                    "Free",
                    null,
                    null
            );
            when(repo.findByEmail("anv@gmail.com")).thenReturn(Optional.of(lockedUser));

            assertThatThrownBy(() -> userUsecase.login(loginRequest))
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
            when(repo.findByEmail("anv@gmail.com")).thenReturn(Optional.of(activeLocalUser));
            when(securityService.validatePassword("Password123@", "hashed-password")).thenReturn(false);

            assertThatThrownBy(() -> userUsecase.login(loginRequest))
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
            when(repo.findByEmail("anv@gmail.com")).thenReturn(Optional.of(activeLocalUser));
            when(securityService.validatePassword("Password123@", "hashed-password")).thenReturn(true);
            when(mapperDTO.toUserResponseDTO(activeLocalUser)).thenReturn(userResponseDTO);
            when(securityService.generateAccessToken(any(ATPayload.class))).thenReturn("access-token");
            when(securityService.generateRefreshToken(any(RTPayload.class))).thenReturn("refresh-token");
            when(securityService.parseNewRefreshToken("refresh-token"))
                    .thenReturn(mock(NewRTRequestDTO.class));
            when(refreshTokenService.save(any(NewRTRequestDTO.class))).thenReturn(false);

            assertThatThrownBy(() -> userUsecase.login(loginRequest))
                    .isInstanceOf(InternalServerException.class)
                    .hasMessage("Lỗi trong quá trình lưu RT!");
        }
    }
}
