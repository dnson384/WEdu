package com.wedu.exam_creation.auth.usecase;

import com.wedu.exam_creation.auth.dto.mapper.AuthDTOMapper;
import com.wedu.exam_creation.auth.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.common.dto.refreshToken.request.NewRTRequestDTO;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.token.RTPayload;
import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.user.usecase.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthUsecaseRegisterTest {
    @Mock
    private AuthDTOMapper mapperDTO;
    @Mock
    private SecurityService securityService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private UserService userService;

    private AuthUsecase authUsecase;

    private NewUserRequestDTO newUserRequest;
    private CommonUserResponseAllDTO savedEntity;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        authUsecase = new AuthUsecase(userService, securityService, refreshTokenService, mapperDTO);

        newUserRequest = new NewUserRequestDTO(
                "anv@gmail.com",
                "Password123@",
                "Password123@",
                "Nguyen Van A",
                "LOCAL"
        );

        savedEntity = new CommonUserResponseAllDTO(
                "user-123",
                "anv@gmail.com",
                "hashed-password",
                "Nguyen Van A",
                "ROLE_TEACHER",
                "LOCAL",
                "avatars/default-avatar-user.png",
                true,
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
    @DisplayName("Success cases")
    class SuccessCases {
        @Test
        @DisplayName("Đăng ký thành công - tạo user, hash password, sinh AT/RT và lưu RT")
        void registerSuccess() {
            when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.empty());
            when(securityService.hashPassword("Password123@")).thenReturn("hashed-password");
            when(userService.createNewUser(newUserRequest, "hashed-password")).thenReturn(savedEntity);
            when(mapperDTO.toUserResponseDTO(savedEntity)).thenReturn(userResponseDTO);
            when(securityService.generateAccessToken(any(ATPayload.class))).thenReturn("access-token");
            when(securityService.generateRefreshToken(any(RTPayload.class))).thenReturn("refresh-token");
            when(securityService.parseNewRefreshToken("refresh-token"))
                    .thenReturn(mock(NewRTRequestDTO.class));
            when(refreshTokenService.save(any(NewRTRequestDTO.class))).thenReturn(true);

            AuthorizedResponseDTO result = authUsecase.register(newUserRequest);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.getUser()).isEqualTo(userResponseDTO);

            verify(securityService, times(1)).hashPassword("Password123@");


            verify(userService, times(1)).findByEmail("anv@gmail.com");
            verify(securityService, times(1)).hashPassword("Password123@");

            ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<NewUserRequestDTO> requestCaptor = ArgumentCaptor.forClass(NewUserRequestDTO.class);
            verify(userService, times(1)).createNewUser(requestCaptor.capture(), passwordCaptor.capture());

            assertThat(passwordCaptor.getValue()).isEqualTo("hashed-password");
            assertThat(requestCaptor.getValue().getEmail()).isEqualTo("anv@gmail.com");

            verify(refreshTokenService, times(1)).save(any(NewRTRequestDTO.class));
        }

        @Test
        @DisplayName("Password gốc (plain) không được lưu trực tiếp vào UserEntity")
        void registerShouldNotStorePlainPassword() {
            when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.empty());
            when(securityService.hashPassword("Password123@")).thenReturn("hashed-password");
            when(userService.createNewUser(newUserRequest, "hashed-password")).thenReturn(savedEntity);
            when(mapperDTO.toUserResponseDTO(savedEntity)).thenReturn(userResponseDTO);
            when(securityService.generateAccessToken(any(ATPayload.class))).thenReturn("access-token");
            when(securityService.generateRefreshToken(any(RTPayload.class))).thenReturn("refresh-token");
            when(securityService.parseNewRefreshToken(anyString()))
                    .thenReturn(mock(NewRTRequestDTO.class));
            when(refreshTokenService.save(any(NewRTRequestDTO.class))).thenReturn(true);

            authUsecase.register(newUserRequest);

            ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
            verify(userService).createNewUser(any(NewUserRequestDTO.class), passwordCaptor.capture());

            assertThat(passwordCaptor.getValue())
                    .isNotEqualTo("Password123@")
                    .isEqualTo("hashed-password");
        }
    }

    @Nested
    @DisplayName("confirmPassword mismatch")
    class ConfirmPasswordMismatchCases {
        @Test
        @DisplayName("Ném BadRequestException khi confirmPassword không khớp plainPassword")
        void register_ConfirmPasswordMismatch_ThrowsBadRequestException() {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    "Password123@",
                    "Password999@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            assertThatThrownBy(() -> authUsecase.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Mật khẩu xác nhận không trùng khớp");

            // Verify dừng lại NGAY, không có side-effect nào xảy ra
            verify(securityService, never()).hashPassword(anyString());
            verify(userService, never()).createNewUser(any(NewUserRequestDTO.class), anyString());
            verify(refreshTokenService, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Email đã tồn tại")
    class EmailAlreadyExistsCases {
        @Test
        @DisplayName("Ném BadRequestException khi email đã tồn tại trong hệ thống")
        void register_EmailAlreadyExists_ThrowsBadRequestException() {
            when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(savedEntity));

            assertThatThrownBy(() -> authUsecase.register(newUserRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Tài khoản đã tồn tại");

            // Không được tạo user, hash password, hay sinh token
            verify(securityService, never()).hashPassword(anyString());
            verify(userService, never()).createNewUser(any(NewUserRequestDTO.class), anyString());
            verify(refreshTokenService, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Mapper trả về null")
    class MapperNullCases {
        @Test
        @DisplayName("Ném InternalServerException khi mapperDTO trả về null")
        void register_MapperReturnsNull_ThrowsInternalServerException() {
            when(securityService.hashPassword(anyString())).thenReturn("hashed-password");
            when(userService.createNewUser(any(NewUserRequestDTO.class), anyString())).thenReturn(savedEntity);
            when(mapperDTO.toUserResponseDTO(savedEntity)).thenReturn(null);

            assertThatThrownBy(() -> authUsecase.register(newUserRequest))
                    .isInstanceOf(InternalServerException.class)
                    .hasMessage("Lỗi trong quá trình chuyển đổi entity -> dto");

            verify(securityService, never()).generateAccessToken(any());
            verify(securityService, never()).generateRefreshToken(any());
            verify(refreshTokenService, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Lưu Refresh Token thất bại")
    class SaveRefreshTokenFailureCases {

        @Test
        @DisplayName("Ném InternalServerException khi lưu RT thất bại")
        void register_SaveRTFails_ThrowsInternalServerException() {
            when(securityService.hashPassword(anyString())).thenReturn("hashed-password");
            when(userService.createNewUser(any(NewUserRequestDTO.class), anyString())).thenReturn(savedEntity);
            when(mapperDTO.toUserResponseDTO(savedEntity)).thenReturn(userResponseDTO);
            when(securityService.generateAccessToken(any(ATPayload.class))).thenReturn("access-token");
            when(securityService.generateRefreshToken(any(RTPayload.class))).thenReturn("refresh-token");
            when(securityService.parseNewRefreshToken("refresh-token"))
                    .thenReturn(mock(NewRTRequestDTO.class));
            when(refreshTokenService.save(any(NewRTRequestDTO.class))).thenReturn(false);

            assertThatThrownBy(() -> authUsecase.register(newUserRequest))
                    .isInstanceOf(InternalServerException.class)
                    .hasMessage("Lỗi trong quá trình lưu RT!");

            verify(userService, times(1)).createNewUser(any(NewUserRequestDTO.class), anyString());
            verify(refreshTokenService, times(1)).save(any(NewRTRequestDTO.class));
        }
    }
}
