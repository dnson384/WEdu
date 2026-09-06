package com.wedu.exam_creation.user.usecase;

import com.wedu.exam_creation.common.dto.user.mapper.UserCommonDTOMapper;
import com.wedu.exam_creation.common.dto.user.request.UserUpdateFields;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
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
    private CommonUserResponseDTO mockUserCommon;

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
                LocalDateTime.now(),
                LocalDateTime.now()
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
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        mockUserCommon = new CommonUserResponseDTO(
                USER_ID,
                EMAIL,
                NEW_USERNAME,
                "ROLE_TEACHER",
                AVATAR_URL,
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // Update username
    @Test
    @DisplayName("200 - Cập nhật username")
    void should_updateUsernameSuccessfully_when_requestIsValid() throws Exception {
        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.updateField(eq(USER_ID), any(UserUpdateFields.class)))
                .thenReturn(mockNewUserEntity);
        when(mapper.toCommonDTO(mockNewUserEntity))
                .thenReturn(mockUserCommon);

        CommonUserResponseDTO result = userUsecase.updateUsername(USER_ID, NEW_USERNAME);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getUsername()).isEqualTo(NEW_USERNAME);

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, times(1)).toCommonDTO(any(UserEntity.class));
    }

    @Test
    @DisplayName("400 - Cập nhật với username rỗng")
    void should_returnBadRequest_when_usernameIsEmpty() throws Exception {
        assertThatThrownBy(() -> userUsecase.updateUsername(USER_ID, ""))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng rỗng");

        verify(repo, never()).findById(anyString());
        verify(repo, never()).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, never()).toCommonDTO(any(UserEntity.class));
    }

    @Test
    @DisplayName("400 - Cập nhật với username là null")
    void should_returnBadRequest_when_usernameIsNull() throws Exception {
        assertThatThrownBy(() -> userUsecase.updateUsername(USER_ID, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng rỗng");

        verify(repo, never()).findById(anyString());
        verify(repo, never()).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, never()).toCommonDTO(any(UserEntity.class));
    }

    @Test
    @DisplayName("400 - Cập nhật với username là chỉ chứa khoảng trắng")
    void should_returnBadRequest_when_usernameContainsOnlyWhitespace() throws Exception {
        assertThatThrownBy(() -> userUsecase.updateUsername(USER_ID, "    "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng rỗng");

        verify(repo, never()).findById(anyString());
        verify(repo, never()).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, never()).toCommonDTO(any(UserEntity.class));
    }

    @Test
    @DisplayName("400 - Cập nhật với username quá dài")
    void should_returnBadRequest_when_usernameExceedsMaxLength() throws Exception {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder str = new StringBuilder();

        for (int i = 0; i < 260; i++) {
            int index = (int) (Math.random() * characters.length());
            str.append(characters.charAt(index));
        }

        String longUsername = str.toString();

        assertThatThrownBy(() -> userUsecase.updateUsername(USER_ID, longUsername))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng quá dài");

        verify(repo, never()).findById(anyString());
        verify(repo, never()).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, never()).toCommonDTO(any(UserEntity.class));
    }

    @Test
    @DisplayName("200 - Cập nhật với username có số")
    void should_updateUsernameSuccessfully_when_usernameContainsDigits() throws Exception {
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
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CommonUserResponseDTO mockNumCommon = new CommonUserResponseDTO(
                USER_ID,
                EMAIL,
                hasNum,
                "ROLE_TEACHER",
                AVATAR_URL,
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.updateField(eq(USER_ID), any(UserUpdateFields.class)))
                .thenReturn(mockNum);
        when(mapper.toCommonDTO(mockNum))
                .thenReturn(mockNumCommon);

        CommonUserResponseDTO result = userUsecase.updateUsername(USER_ID, hasNum);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(hasNum);

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, times(1)).toCommonDTO(any(UserEntity.class));
    }

    @Test
    @DisplayName("200 - Cập nhật với username có emoji")
    void should_updateUsernameSuccessfully_when_usernameContainsEmojis() throws Exception {
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
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CommonUserResponseDTO mockEmojiCommon = new CommonUserResponseDTO(
                USER_ID,
                EMAIL,
                hasEmoji,
                "ROLE_TEACHER",
                AVATAR_URL,
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.updateField(eq(USER_ID), any(UserUpdateFields.class)))
                .thenReturn(mockEmoji);
        when(mapper.toCommonDTO(mockEmoji))
                .thenReturn(mockEmojiCommon);

        CommonUserResponseDTO result = userUsecase.updateUsername(USER_ID, hasEmoji);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(hasEmoji);

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, times(1)).toCommonDTO(any(UserEntity.class));
    }

    @Test
    @DisplayName("200 - Cập nhật với username có unicode")
    void should_updateUsernameSuccessfully_when_usernameContainsUnicodeCharacters() throws Exception {
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
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CommonUserResponseDTO mockUnicodeCommon = new CommonUserResponseDTO(
                USER_ID,
                EMAIL,
                hasUnicode,
                "ROLE_TEACHER",
                AVATAR_URL,
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.updateField(eq(USER_ID), any(UserUpdateFields.class)))
                .thenReturn(mockUnicode);
        when(mapper.toCommonDTO(mockUnicode))
                .thenReturn(mockUnicodeCommon);

        CommonUserResponseDTO result = userUsecase.updateUsername(USER_ID, hasUnicode);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(hasUnicode);

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, times(1)).toCommonDTO(any(UserEntity.class));
    }

    @Test
    @DisplayName("500 - Lỗi trong quá trình cập nhật thông tin tài khoản")
    void should_throwInternalServerError_when_updateAccountFails() throws Exception {
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

    @Test
    @DisplayName("404 - Không tìm thấy tài khoản nguời dùng")
    void should_returnNotFound_when_userAccountDoesNotExist() throws Exception {
        when(repo.findById(USER_ID))
                .thenReturn(null);

        assertThatThrownBy(() -> userUsecase.updateUsername(USER_ID, NEW_USERNAME))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Không tìm thấy tài khoản");

        verify(repo, times(1)).findById(anyString());
        verify(repo, never()).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, never()).toCommonDTO(any(UserEntity.class));
    }

    @Test
    @DisplayName("403 - Tài khoản đã bị khóa")
    void should_returnForbidden_when_accountIsLocked() throws Exception {
        UserEntity lockedUser = new UserEntity(
                USER_ID,
                EMAIL,
                "hashed-password",
                OLD_USERNAME,
                "ROLE_TEACHER",
                "LOCAL",
                AVATAR_URL,
                false,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(repo.findById(USER_ID))
                .thenReturn(lockedUser);

        assertThatThrownBy(() -> userUsecase.updateUsername(USER_ID, NEW_USERNAME))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Tài khoản đã bị khóa");

        verify(repo, times(1)).findById(anyString());
        verify(repo, never()).updateField(anyString(), any(UserUpdateFields.class));
        verify(mapper, never()).toCommonDTO(any(UserEntity.class));
    }
}
