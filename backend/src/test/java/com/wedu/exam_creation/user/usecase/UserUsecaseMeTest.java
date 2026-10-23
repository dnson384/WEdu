package com.wedu.exam_creation.user.usecase;

import com.wedu.exam_creation.common.dto.user.mapper.UserCommonDTOMapper;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.notification.telegram.TelegramNotificationService;
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.storage.service.S3Service;
import com.wedu.exam_creation.user.dto.mapper.UserDTOMapper;
import com.wedu.exam_creation.user.infrastructure.repository.UserRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserUsecaseMeTest {
    private static final String USER_ID = "user-123";
    private static final String EMAIL = "user@gmail.com";
    private static final String RAW_AVATAR = "avatars/default-avatar-user.png";
    private static final String PRESIGNED_AVATAR = "https://s3.amazonaws.com/my-pic-presigned.png";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");

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

    private CommonUserResponseAllDTO mockUser;
    private CommonUserResponseDTO mockUserRes;

    @BeforeEach
    void setUp() {
        userUsecase = new UserUsecase(repo, mapper, mapperCommon, refreshTokenService, s3Service, telegramNotificationService);

        mockUser = new CommonUserResponseAllDTO(
                USER_ID,
                EMAIL,
                "hashed-password",
                "Nguyen Van A",
                "ROLE_TEACHER",
                "LOCAL",
                RAW_AVATAR,
                true,
                "FREE",
                CREATED_AT,
                UPDATED_AT
        );

        mockUserRes = new CommonUserResponseDTO(
                USER_ID,
                EMAIL,
                "Nguyen Van A",
                "ROLE_TEACHER",
                RAW_AVATAR,
                true,
                "FREE",
                CREATED_AT,
                UPDATED_AT
        );
    }

    @Test
    @DisplayName("Trả về thông tin tài khoản với presigned avatar URL")
    void should_returnUserWithPresignedAvatar_when_userIsValid() {
        when(mapperCommon.commonAllToCommonDTO(mockUser)).thenReturn(mockUserRes);
        when(s3Service.generatePresignedUrl(RAW_AVATAR)).thenReturn(PRESIGNED_AVATAR);

        CommonUserResponseDTO result = userUsecase.getMe(mockUser);

        assertThat(result).isSameAs(mockUserRes);
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getUsername()).isEqualTo(mockUser.getUsername());
        assertThat(result.getRole()).isEqualTo(mockUser.getRole());
        assertThat(result.getAvatarUrl()).isEqualTo(PRESIGNED_AVATAR);
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getAccountType()).isEqualTo(mockUser.getAccountType());
        assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(result.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(mockUser.getAvatarUrl()).isEqualTo(RAW_AVATAR);

        verify(mapperCommon).commonAllToCommonDTO(same(mockUser));
        verify(s3Service).generatePresignedUrl(RAW_AVATAR);
        verifyNoMoreInteractions(mapperCommon, s3Service);
        verifyNoInteractions(repo, mapper, refreshTokenService, telegramNotificationService);
    }
}
