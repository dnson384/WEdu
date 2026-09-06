package com.wedu.exam_creation.user.usecase;

import com.wedu.exam_creation.common.dto.user.mapper.UserCommonDTOMapper;
import com.wedu.exam_creation.common.dto.user.request.UserUpdateFields;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.notification.service.TelegramNotificationService;
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.storage.service.S3Service;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.dto.mapper.UserDTOMapper;
import com.wedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserUsecaseUpdateAvatarTest {
    private static final String USER_ID = "user-123";
    private static final String EMAIL = "u123@gmail.com";
    private static final String USERNAME = "u123";
    private static final String OLD_S3KEY = "avatars/old-s3key.png";
    private static final String NEW_S3KEY = "avatars/new-s3key.png";

    @Mock
    private UserRepositoryImpl repo;
    @Mock
    private UserDTOMapper mapperDTO;
    @Mock
    private UserCommonDTOMapper mapperCommon;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private S3Service s3Service;
    @Mock
    private TelegramNotificationService telegramNotificationService;

    private UserUsecase userUsecase;

    private UserEntity mockOldUserEntity;
    private UserEntity mockNewUserEntity;

    @BeforeEach
    void setUp() {
        userUsecase = new UserUsecase(repo, mapperDTO, mapperCommon, refreshTokenService, s3Service, telegramNotificationService);

        mockOldUserEntity = new UserEntity(
                USER_ID,
                EMAIL,
                "hashed-password",
                USERNAME,
                "ROLE_TEACHER",
                "LOCAL",
                OLD_S3KEY,
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        mockNewUserEntity = new UserEntity(
                USER_ID,
                EMAIL,
                "hashed-password",
                USERNAME,
                "ROLE_TEACHER",
                "LOCAL",
                NEW_S3KEY,
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("200 - Update avatar thành công")
    void should_updateAvatarSuccessfully_when_requestIsValid() throws Exception {
        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.updateField(eq(USER_ID), any(UserUpdateFields.class)))
                .thenReturn(mockNewUserEntity);
        doNothing().when(s3Service).deleteFile(OLD_S3KEY);

        boolean result = userUsecase.updateAvatar(USER_ID, NEW_S3KEY);

        assertThat(result).isTrue();
        verify(repo, times(2)).findById(anyString());
        verify(repo, times(1)).updateField(anyString(), any(UserUpdateFields.class));
        verify(s3Service, times(1)).deleteFile(anyString());
    }

    @Test
    @DisplayName("200 - Update khi user đang dùng avatar mặc định thì không xóa ảnh mặc định")
    void should_notDeleteDefaultAvatar_when_userUpdatesFromDefaultAvatar() throws Exception {
        UserEntity mockDefault = new UserEntity(
                USER_ID,
                EMAIL,
                "hashed-password",
                USERNAME,
                "ROLE_TEACHER",
                "LOCAL",
                "avatars/default-avatar-user.png",
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(repo.findById(anyString()))
                .thenReturn(mockDefault);
        when(repo.updateField(eq(USER_ID), any(UserUpdateFields.class)))
                .thenReturn(mockNewUserEntity);

        boolean result = userUsecase.updateAvatar(USER_ID, NEW_S3KEY);

        assertThat(result).isTrue();
        verify(repo, times(2)).findById(anyString());
        verify(repo, times(1)).updateField(anyString(), any(UserUpdateFields.class));
        verify(s3Service, never()).deleteFile(anyString());
    }

    @Test
    @DisplayName("500 - Lưu thông tin người dùng thất bại")
    void should_returnInternalServerError_when_saveUserFails() throws Exception {
        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.updateField(eq(USER_ID), any(UserUpdateFields.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> userUsecase.updateAvatar(USER_ID, NEW_S3KEY))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Có lỗi trong quá trình cập nhật avatar");

        verify(repo, times(2)).findById(anyString());
        verify(repo, times(1)).updateField(anyString(), any(UserUpdateFields.class));
        verify(s3Service, times(1)).deleteFile(anyString());
    }

    @Test
    @DisplayName("200 - Xóa ảnh cũ khỏi S3 thất bại")
    void should_updateAvatarSuccessfully_when_deleteOldImageFromS3Fails() throws Exception {
        when(repo.findById(anyString()))
                .thenReturn(mockOldUserEntity);
        when(repo.updateField(eq(USER_ID), any(UserUpdateFields.class)))
                .thenReturn(mockNewUserEntity);
        doThrow(new RuntimeException("S3 down"))
                .when(s3Service).deleteFile(OLD_S3KEY);


        boolean result = userUsecase.updateAvatar(USER_ID, NEW_S3KEY);

        assertThat(result).isTrue();
        verify(repo, times(2)).findById(anyString());
        verify(repo, times(1)).updateField(anyString(), any(UserUpdateFields.class));
        verify(s3Service, times(1)).deleteFile(anyString());
    }

    @Test
    @DisplayName("400 - Update với S3Key rỗng")
    void should_returnBadRequest_when_s3KeyIsEmpty() throws Exception {
        assertThatThrownBy(() -> userUsecase.updateAvatar(USER_ID, ""))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("s3Key rỗng");

        verify(repo, times(0)).findById(anyString());
        verify(repo, times(0)).save(any(UserEntity.class));
        verify(s3Service, times(0)).deleteFile(anyString());
    }
}
