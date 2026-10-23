package com.wedu.exam_creation.user.usecase;

import com.wedu.exam_creation.common.dto.user.mapper.UserCommonDTOMapper;
import com.wedu.exam_creation.common.dto.user.request.UserUpdateFields;
import org.mockito.ArgumentCaptor;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.notification.telegram.TelegramNotificationService;
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

import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserUsecaseUpdateUsernameTest {
    private static final String USER_ID = "user-123";
    private static final String EMAIL = "u123@gmail.com";
    private static final String AVATAR_URL = "avatars/s3key.png";
    private static final String OLD_USERNAME = "old-username";
    private static final String NEW_USERNAME = "new-username-123";

    @Mock
    private UserRepositoryImpl repo;
    @Mock
    private UserDTOMapper mapper;
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
        userUsecase = new UserUsecase(repo, mapper, mapperCommon, refreshTokenService, s3Service, telegramNotificationService);

        mockOldUserEntity = new UserEntity(
                USER_ID,
                EMAIL,
                "hashed-password",
                OLD_USERNAME,
                "ROLE_TEACHER",
                "LOCAL",
                AVATAR_URL,
                true,
                "FREE",
                Instant.now(),
                Instant.now()
        );

        mockNewUserEntity = new UserEntity(
                USER_ID,
                EMAIL,
                "hashed-password",
                NEW_USERNAME,
                "ROLE_TEACHER",
                "LOCAL",
                AVATAR_URL,
                true,
                "FREE",
                Instant.now(),
                Instant.now()
        );

    }

    // Update username
    @Test
    @DisplayName("200 - Cập nhật username")
    void should_updateUsernameSuccessfully_when_requestIsValid() {
        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.updateField(eq(USER_ID), any(UserUpdateFields.class)))
                .thenReturn(mockNewUserEntity);

        boolean result = userUsecase.updateUsername(USER_ID, NEW_USERNAME);

        assertThat(result).isTrue();

        verify(repo, times(1)).findById(anyString());
        ArgumentCaptor<UserUpdateFields> fieldsCaptor = ArgumentCaptor.forClass(UserUpdateFields.class);
        verify(repo).updateField(eq(USER_ID), fieldsCaptor.capture());
        assertThat(fieldsCaptor.getValue().getUsername()).isEqualTo(NEW_USERNAME);
        verifyNoInteractions(mapper, mapperCommon, s3Service);
    }

    @Test
    @DisplayName("400 - Cập nhật với username rỗng")
    void should_returnBadRequest_when_usernameIsEmpty() {
        assertThatThrownBy(() -> userUsecase.updateUsername(USER_ID, ""))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng rỗng");

        verify(repo, never()).findById(anyString());
        verify(repo, never()).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, never()).toCommonDTO(any(UserEntity.class));
    }

    @Test
    @DisplayName("400 - Cập nhật với username là null")
    void should_returnBadRequest_when_usernameIsNull() {
        assertThatThrownBy(() -> userUsecase.updateUsername(USER_ID, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng rỗng");

        verify(repo, never()).findById(anyString());
        verify(repo, never()).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, never()).toCommonDTO(any(UserEntity.class));
    }

    @Test
    @DisplayName("400 - Cập nhật với username là chỉ chứa khoảng trắng")
    void should_returnBadRequest_when_usernameContainsOnlyWhitespace() {
        assertThatThrownBy(() -> userUsecase.updateUsername(USER_ID, "    "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng rỗng");

        verify(repo, never()).findById(anyString());
        verify(repo, never()).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, never()).toCommonDTO(any(UserEntity.class));
    }

    @Test
    @DisplayName("400 - Cập nhật với username quá dài")
    void should_returnBadRequest_when_usernameExceedsMaxLength() {
        String longUsername = "a".repeat(255);

        assertThatThrownBy(() -> userUsecase.updateUsername(USER_ID, longUsername))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng quá dài");

        verify(repo, never()).findById(anyString());
        verify(repo, never()).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, never()).toCommonDTO(any(UserEntity.class));
    }

    @Test
    @DisplayName("200 - Cập nhật với username có số")
    void should_updateUsernameSuccessfully_when_usernameContainsDigits() {
        String hasNum = "abc123";

        UserEntity mockNum = new UserEntity(
                USER_ID,
                EMAIL,
                "hashed-password",
                hasNum,
                "ROLE_TEACHER",
                "LOCAL",
                AVATAR_URL,
                true,
                "FREE",
                Instant.now(),
                Instant.now()
        );


        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.updateField(eq(USER_ID), any(UserUpdateFields.class)))
                .thenReturn(mockNum);

        boolean result = userUsecase.updateUsername(USER_ID, hasNum);

        assertThat(result).isTrue();

        verify(repo, times(1)).findById(anyString());
        ArgumentCaptor<UserUpdateFields> fieldsCaptor = ArgumentCaptor.forClass(UserUpdateFields.class);
        verify(repo).updateField(eq(USER_ID), fieldsCaptor.capture());
        assertThat(fieldsCaptor.getValue().getUsername()).isEqualTo(hasNum);
        verifyNoInteractions(mapper, mapperCommon, s3Service);
    }

    @Test
    @DisplayName("200 - Cập nhật với username có emoji")
    void should_updateUsernameSuccessfully_when_usernameContainsEmojis() {
        String hasEmoji = "Nguyen Van A \uD83D\uDE00";

        UserEntity mockEmoji = new UserEntity(
                USER_ID,
                EMAIL,
                "hashed-password",
                hasEmoji,
                "ROLE_TEACHER",
                "LOCAL",
                AVATAR_URL,
                true,
                "FREE",
                Instant.now(),
                Instant.now()
        );


        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.updateField(eq(USER_ID), any(UserUpdateFields.class)))
                .thenReturn(mockEmoji);

        boolean result = userUsecase.updateUsername(USER_ID, hasEmoji);

        assertThat(result).isTrue();

        verify(repo, times(1)).findById(anyString());
        ArgumentCaptor<UserUpdateFields> fieldsCaptor = ArgumentCaptor.forClass(UserUpdateFields.class);
        verify(repo).updateField(eq(USER_ID), fieldsCaptor.capture());
        assertThat(fieldsCaptor.getValue().getUsername()).isEqualTo(hasEmoji);
        verifyNoInteractions(mapper, mapperCommon, s3Service);
    }

    @Test
    @DisplayName("200 - Cập nhật với username có unicode")
    void should_updateUsernameSuccessfully_when_usernameContainsUnicodeCharacters() {
        String hasUnicode = "Nguyễn Văn A";

        UserEntity mockUnicode = new UserEntity(
                USER_ID,
                EMAIL,
                "hashed-password",
                hasUnicode,
                "ROLE_TEACHER",
                "LOCAL",
                AVATAR_URL,
                true,
                "FREE",
                Instant.now(),
                Instant.now()
        );


        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.updateField(eq(USER_ID), any(UserUpdateFields.class)))
                .thenReturn(mockUnicode);

        boolean result = userUsecase.updateUsername(USER_ID, hasUnicode);

        assertThat(result).isTrue();

        verify(repo, times(1)).findById(anyString());
        ArgumentCaptor<UserUpdateFields> fieldsCaptor = ArgumentCaptor.forClass(UserUpdateFields.class);
        verify(repo).updateField(eq(USER_ID), fieldsCaptor.capture());
        assertThat(fieldsCaptor.getValue().getUsername()).isEqualTo(hasUnicode);
        verifyNoInteractions(mapper, mapperCommon, s3Service);
    }

    @Test
    @DisplayName("500 - Lỗi trong quá trình cập nhật thông tin tài khoản")
    void should_throwInternalServerError_when_updateAccountFails() {
        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.updateField(eq(USER_ID), any(UserUpdateFields.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> userUsecase.updateUsername(USER_ID, NEW_USERNAME))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Có lỗi trong quá trình cập nhật tên người dùng");

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, never()).toCommonDTO(any(UserEntity.class));
    }
}
