package com.wedu.exam_creation.user.usecase;

import com.wedu.exam_creation.common.dto.user.mapper.UserCommonDTOMapper;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.notification.service.TelegramNotificationService;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserUsecaseMeTest {
    private static final String USER_ID = "user-123";
    private static final String EMAIL = "user@gmail.com";
    private static final String RAW_AVATAR = "avatars/default-avatar-user.png";
    private static final String PRESIGNED_AVATAR = "https://s3.amazonaws.com/my-pic-presigned.png";

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
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        mockUserRes = new CommonUserResponseDTO(
                USER_ID,
                EMAIL,
                "Nguyen Van A",
                "ROLE_TEACHER",
                RAW_AVATAR,
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Case 1: 200 - Chuyển đổi thành công thông tin tài khoản")
    void success() throws Exception {
        // Given
        when(mapperCommon.commonAllToCommonDTO(mockUser)).thenReturn(mockUserRes);
        when(s3Service.generatePresignedUrl(RAW_AVATAR)).thenReturn(PRESIGNED_AVATAR);

        // When
        CommonUserResponseDTO result = userUsecase.getMe(any(CommonUserResponseAllDTO.class));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getAvatarUrl()).isEqualTo(PRESIGNED_AVATAR);

        verify(mapperCommon, times(1)).commonAllToCommonDTO(any(CommonUserResponseAllDTO.class));
        verify(s3Service, times(1)).generatePresignedUrl(anyString());
    }
}
