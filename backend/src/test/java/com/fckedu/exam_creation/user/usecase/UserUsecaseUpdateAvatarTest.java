package com.fckedu.exam_creation.user.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fckedu.exam_creation.common.exception.BadRequestException;
import com.fckedu.exam_creation.common.exception.ForbiddenException;
import com.fckedu.exam_creation.common.exception.InternalServerException;
import com.fckedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.fckedu.exam_creation.security.service.SecurityService;
import com.fckedu.exam_creation.storage.service.S3Service;
import com.fckedu.exam_creation.user.domain.entity.UserEntity;
import com.fckedu.exam_creation.user.dto.mapper.UserDTOMapper;
import com.fckedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserUsecaseUpdateAvatarTest {
    private static final String VALID_AT = "mock-at";
    private static final String INVALID_AT = "invalid-at";
    private static final String VALID_RT = "mock-rt";
    private static final String INVALID_RT = "invalid-rt";

    private static final String USER_ID = "user-123";
    private static final String JTI = "jti-uuid";
    private static final String EMAIL = "user@gmail.com";
    private static final String OLD_S3KEY = "avatars/old-s3key.png";
    private static final String NEW_S3KEY = "avatars/new-s3key.png";
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    private UserService userService;

    private UserUsecase userUsecase;

    private UserEntity mockOldUserEntity;
    private UserEntity mockNewUserEntity;


    @BeforeEach
    void setUp() {
        userUsecase = new UserUsecase(repo, mapperDTO, securityService, refreshTokenService, s3Service);

        mockOldUserEntity = new UserEntity();
        mockOldUserEntity.setId(USER_ID);
        mockOldUserEntity.setIsActive(true);
        mockOldUserEntity.setAvatarUrl(OLD_S3KEY);

        mockNewUserEntity = new UserEntity();
        mockNewUserEntity.setId(USER_ID);
        mockNewUserEntity.setIsActive(true);
        mockNewUserEntity.setAvatarUrl(NEW_S3KEY);
    }

    @Test
    @DisplayName("Update avatar thành công với s3Key vừa upload")
    void happyCase() throws Exception {
        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.save(any(UserEntity.class)))
                .thenReturn(mockNewUserEntity);

        boolean result = userUsecase.updateAvatar(USER_ID, NEW_S3KEY);

        assertThat(result).isTrue();
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getAvatarUrl()).isEqualTo(NEW_S3KEY);

        verify(s3Service, times(1)).deleteFile(anyString());
    }

    @Test
    @DisplayName("Update khi user đang dùng avatar mặc định")
    void defaultAvatar() throws Exception {
        UserEntity mockDefault = new UserEntity();
        mockDefault.setId(USER_ID);
        mockDefault.setAvatarUrl("avatars/default-avatar-user.png");
        mockDefault.setIsActive(true);

        when(repo.findById(anyString()))
                .thenReturn(mockDefault);
        when(repo.save(any(UserEntity.class)))
                .thenReturn(mockNewUserEntity);

        boolean result = userUsecase.updateAvatar(USER_ID, NEW_S3KEY);

        assertThat(result).isTrue();
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getAvatarUrl()).isEqualTo(NEW_S3KEY);

        verify(s3Service, times(0)).deleteFile(anyString());
    }

    @Test
    @DisplayName("Lưu thông tin người dùng thất bại")
    void saveFailure() throws Exception {
        when(repo.findById(anyString()))
                .thenReturn(mockOldUserEntity);
        when(repo.save(any(UserEntity.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> userUsecase.updateAvatar(USER_ID, NEW_S3KEY))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Có lỗi trong quá trình cập nhật avatar");

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).save(any(UserEntity.class));
        verify(s3Service, times(1)).deleteFile(anyString());
    }

    @Test
    @DisplayName("Xóa ảnh cũ khỏi S3 thất bại")
    void deleteOldFailure() throws Exception {
        when(repo.findById(anyString()))
                .thenReturn(mockOldUserEntity);
        when(repo.save(any(UserEntity.class)))
                .thenReturn(mockNewUserEntity);

        boolean result = userUsecase.updateAvatar(USER_ID, NEW_S3KEY);

        assertThat(result).isTrue();
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getAvatarUrl()).isEqualTo(NEW_S3KEY);

        verify(s3Service, times(1)).deleteFile(anyString());
    }

    @Test
    @DisplayName("Update với S3Key rỗng")
    void emptyS3Key() throws Exception {
        assertThatThrownBy(() -> userUsecase.updateAvatar(USER_ID, ""))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("s3Key rỗng");

        verify(repo, times(0)).findById(anyString());
        verify(repo, times(0)).save(any(UserEntity.class));
        verify(s3Service, times(0)).deleteFile(anyString());
    }

    @Test
    @DisplayName("Update khi user đã bị khóa")
    void lockedUser() throws Exception {
        UserEntity lockedUser = new UserEntity();
        lockedUser.setId(USER_ID);
        lockedUser.setAvatarUrl(OLD_S3KEY);
        lockedUser.setIsActive(false);

        when(repo.findById(anyString()))
                .thenReturn(lockedUser);

        assertThatThrownBy(() -> userUsecase.updateAvatar(USER_ID, NEW_S3KEY))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Tài khoản đã bị khóa");

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(0)).save(any(UserEntity.class));
        verify(s3Service, times(0)).deleteFile(anyString());
    }
}
