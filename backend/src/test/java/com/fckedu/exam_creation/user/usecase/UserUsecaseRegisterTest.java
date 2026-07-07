package com.fckedu.exam_creation.user.usecase;

import com.fckedu.exam_creation.common.dto.refreshToken.request.NewRTRequestDTO;
import com.fckedu.exam_creation.common.dto.token.ATPayload;
import com.fckedu.exam_creation.common.dto.token.RTPayload;
import com.fckedu.exam_creation.common.exception.BadRequestException;
import com.fckedu.exam_creation.common.exception.InternalServerException;
import com.fckedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.fckedu.exam_creation.security.service.SecurityService;
import com.fckedu.exam_creation.storage.service.S3Service;
import com.fckedu.exam_creation.user.domain.entity.UserEntity;
import com.fckedu.exam_creation.user.dto.mapper.UserDTOMapper;
import com.fckedu.exam_creation.user.dto.request.NewUserRequestDTO;
import com.fckedu.exam_creation.user.dto.response.AuthorizedResponseDTO;
import com.fckedu.exam_creation.user.dto.response.UserResponseDTO;
import com.fckedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserUsecaseRegisterTest {
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

    private NewUserRequestDTO newUserRequest;
    private UserEntity savedEntity;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        userUsecase = new UserUsecase(repo, mapperDTO, securityService, refreshTokenService, s3Service);

        newUserRequest = new NewUserRequestDTO(
                "anv@gmail.com",
                "Password123@",
                "Password123@",
                "Nguyen Van A",
                "LOCAL"
        );

        savedEntity = new UserEntity(
                "user-123",
                "anv@gmail.com",
                "hashed-password",
                "Nguyen Van A",
                "ROLE_TEACHER",
                "LOCAL",
                "avatars/default-avatar-user.png",
                true,
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
    @DisplayName("Success cases")
    class SuccessCases {
        @Test
        @DisplayName("Đăng ký thành công - tạo user, hash password, sinh AT/RT và lưu RT")
        void registerSuccess() {
            when(repo.findByEmail("anv@gmail.com")).thenReturn(Optional.empty());
            when(securityService.hashPassword("Password123@")).thenReturn("hashed-password");
            when(repo.save(any(UserEntity.class))).thenReturn(savedEntity);
            when(mapperDTO.toUserResponseDTO(savedEntity)).thenReturn(userResponseDTO);
            when(securityService.generateAccessToken(any(ATPayload.class))).thenReturn("access-token");
            when(securityService.generateRefreshToken(any(RTPayload.class))).thenReturn("refresh-token");
            when(securityService.parseNewRefreshToken("refresh-token"))
                    .thenReturn(mock(NewRTRequestDTO.class));
            when(refreshTokenService.save(any(NewRTRequestDTO.class))).thenReturn(true);

            AuthorizedResponseDTO result = userUsecase.register(newUserRequest);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.getUser()).isEqualTo(userResponseDTO);

            verify(securityService, times(1)).hashPassword("Password123@");

            ArgumentCaptor<UserEntity> entityCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(repo, times(1)).save(entityCaptor.capture());
            UserEntity capturedEntity = entityCaptor.getValue();

            assertThat(capturedEntity.getEmail()).isEqualTo("anv@gmail.com");
            assertThat(capturedEntity.getHashedPassword()).isEqualTo("hashed-password");
            assertThat(capturedEntity.getUsername()).isEqualTo("Nguyen Van A");
            assertThat(capturedEntity.getRole()).isEqualTo("ROLE_TEACHER");
            assertThat(capturedEntity.getLoginMethod()).isEqualTo("LOCAL");
            assertThat(capturedEntity.getAvatarUrl()).isEqualTo("avatars/default-avatar-user.png");
            assertThat(capturedEntity.getIsActive()).isTrue();
            assertThat(capturedEntity.getAccountType()).isEqualTo("Free");

            verify(refreshTokenService, times(1)).save(any(NewRTRequestDTO.class));
        }

        @Test
        @DisplayName("Password gốc (plain) không được lưu trực tiếp vào UserEntity")
        void registerShouldNotStorePlainPassword() {
            when(repo.findByEmail("anv@gmail.com")).thenReturn(Optional.empty());
            when(securityService.hashPassword("Password123@")).thenReturn("hashed-password");
            when(repo.save(any(UserEntity.class))).thenReturn(savedEntity);
            when(mapperDTO.toUserResponseDTO(savedEntity)).thenReturn(userResponseDTO);
            when(securityService.generateAccessToken(any(ATPayload.class))).thenReturn("access-token");
            when(securityService.generateRefreshToken(any(RTPayload.class))).thenReturn("refresh-token");
            when(securityService.parseNewRefreshToken(anyString()))
                    .thenReturn(mock(NewRTRequestDTO.class));
            when(refreshTokenService.save(any(NewRTRequestDTO.class))).thenReturn(true);

            userUsecase.register(newUserRequest);

            ArgumentCaptor<UserEntity> entityCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(repo).save(entityCaptor.capture());

            assertThat(entityCaptor.getValue().getHashedPassword())
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

            assertThatThrownBy(() -> userUsecase.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Mật khẩu xác nhận không trùng khớp");

            // Verify dừng lại NGAY, không có side-effect nào xảy ra
            verify(securityService, never()).hashPassword(anyString());
            verify(repo, never()).save(any(UserEntity.class));
            verify(refreshTokenService, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Email đã tồn tại")
    class EmailAlreadyExistsCases {
        @Test
        @DisplayName("Ném BadRequestException khi email đã tồn tại trong hệ thống")
        void register_EmailAlreadyExists_ThrowsBadRequestException() {
            when(repo.findByEmail("anv@gmail.com")).thenReturn(Optional.of(savedEntity));

            assertThatThrownBy(() -> userUsecase.register(newUserRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Tài khoản đã tồn tại");

            // Không được tạo user, hash password, hay sinh token
            verify(securityService, never()).hashPassword(anyString());
            verify(repo, never()).save(any(UserEntity.class));
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
            when(repo.save(any(UserEntity.class))).thenReturn(savedEntity);
            when(mapperDTO.toUserResponseDTO(savedEntity)).thenReturn(null);

            assertThatThrownBy(() -> userUsecase.register(newUserRequest))
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
            when(repo.save(any(UserEntity.class))).thenReturn(savedEntity);
            when(mapperDTO.toUserResponseDTO(savedEntity)).thenReturn(userResponseDTO);
            when(securityService.generateAccessToken(any(ATPayload.class))).thenReturn("access-token");
            when(securityService.generateRefreshToken(any(RTPayload.class))).thenReturn("refresh-token");
            when(securityService.parseNewRefreshToken("refresh-token"))
                    .thenReturn(mock(NewRTRequestDTO.class));
            when(refreshTokenService.save(any(NewRTRequestDTO.class))).thenReturn(false);

            assertThatThrownBy(() -> userUsecase.register(newUserRequest))
                    .isInstanceOf(InternalServerException.class)
                    .hasMessage("Lỗi trong quá trình lưu RT!");

            verify(repo, times(1)).save(any(UserEntity.class));
            verify(refreshTokenService, times(1)).save(any(NewRTRequestDTO.class));
        }
    }

}
