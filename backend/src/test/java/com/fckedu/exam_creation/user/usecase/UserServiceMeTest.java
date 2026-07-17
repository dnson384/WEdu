package com.fckedu.exam_creation.user.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fckedu.exam_creation.common.dto.token.ATPayload;
import com.fckedu.exam_creation.common.dto.token.RTPayload;
import com.fckedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.fckedu.exam_creation.common.exception.ForbiddenException;
import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.common.exception.UnAuthorizedException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceMeTest {
    private static final String VALID_AT = "mock-at";
    private static final String INVALID_AT = "invalid-at";
    private static final String VALID_RT = "mock-rt";
    private static final String INVALID_RT = "invalid-rt";

    private static final String USER_ID = "user-123";
    private static final String JTI = "jti-uuid";
    private static final String EMAIL = "user@gmail.com";
    private static final String RAW_AVATAR = "avatars/my-pic.png";
    private static final String PRESIGNED_AVATAR = "https://s3.amazonaws.com/my-pic-presigned.png";
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

    private CommonUserResponseDTO mockUserDto;
    private ATPayload mockAtPayload;
    private RTPayload mockRtPayload;
    private UserEntity mockUserEntity;

    @BeforeEach
    void setUp() {
        userService = new UserService(repo, mapperDTO, refreshTokenService, securityService, s3Service);

        mockAtPayload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");
        mockRtPayload = new RTPayload(JTI, USER_ID, EMAIL, "ROLE_TEACHER");

        mockUserEntity = new UserEntity();
        mockUserEntity.setId(USER_ID);
        mockUserEntity.setIsActive(true);
        mockUserEntity.setAvatarUrl(RAW_AVATAR);

        mockUserDto = new CommonUserResponseDTO(
                USER_ID,
                EMAIL,
                "Nguyen Van A",
                "ROLE_TEACHER",
                RAW_AVATAR,
                true,
                "FREE"
        );
    }

    @Test
    @DisplayName("Happy case")
    void success() throws Exception {
        // Given
        when(securityService.getPayloadFromAccessToken(VALID_AT)).thenReturn(mockAtPayload);
        when(securityService.getPayloadFromRefreshToken(VALID_RT)).thenReturn(mockRtPayload);
        when(refreshTokenService.exists(JTI, USER_ID)).thenReturn(true);
        when(repo.findById(USER_ID)).thenReturn(mockUserEntity);
        when(mapperDTO.toCommonDTO(mockUserEntity)).thenReturn(mockUserDto);
        when(s3Service.generatePresignedUrl(RAW_AVATAR)).thenReturn(PRESIGNED_AVATAR);

        // When
        CommonUserResponseDTO result = userService.getMe(VALID_AT, VALID_RT);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getAvatarUrl()).isEqualTo(PRESIGNED_AVATAR);

        verify(securityService, times(1)).getPayloadFromAccessToken(VALID_AT);
        verify(securityService, times(1)).getPayloadFromRefreshToken(VALID_RT);
        verify(refreshTokenService, times(1)).exists(JTI, USER_ID);
        verify(repo, times(1)).findById(USER_ID);
        verify(mapperDTO, times(1)).toCommonDTO(mockUserEntity);
        verify(s3Service, times(1)).generatePresignedUrl(RAW_AVATAR);
    }

    @Test
    @DisplayName("Thất bại khi userId trong AT và RT không trùng khớp")
    void misMatchUserId() throws Exception {
        RTPayload mismatchedRtPayload = new RTPayload(JTI, "different-user-id", EMAIL, "ROLE_TEACHER");
        when(securityService.getPayloadFromAccessToken(VALID_AT)).thenReturn(mockAtPayload);
        when(securityService.getPayloadFromRefreshToken(VALID_RT)).thenReturn(mismatchedRtPayload);

        assertThatThrownBy(() -> userService.getMe(VALID_AT, VALID_RT))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("userId không trùng khớp");

        verify(refreshTokenService, never()).exists(anyString(), anyString());
        verify(repo, never()).findById(anyString());
    }

    @Test
    @DisplayName("RT không tồn tại")
    void nonExistRT() {
        // Given
        when(securityService.getPayloadFromAccessToken(VALID_AT)).thenReturn(mockAtPayload);
        when(securityService.getPayloadFromRefreshToken(VALID_RT)).thenReturn(mockRtPayload);
        when(refreshTokenService.exists(JTI, USER_ID)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.getMe(VALID_AT, VALID_RT))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("RT không tồn tại");

        verify(repo, never()).findById(anyString());
    }

    @Test
    @DisplayName("Thất bại khi không tìm thấy thông tin tài khoản người dùng tương ứng trong DB")
    void userNotfound() {
        // Given
        when(securityService.getPayloadFromAccessToken(VALID_AT)).thenReturn(mockAtPayload);
        when(securityService.getPayloadFromRefreshToken(VALID_RT)).thenReturn(mockRtPayload);
        when(refreshTokenService.exists(JTI, USER_ID)).thenReturn(true);
        when(repo.findById(USER_ID)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> userService.getMe(VALID_AT, VALID_RT))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Không tìm thấy tài khoản");

        verify(mapperDTO, never()).toCommonDTO(any());
    }

    @Test
    @DisplayName("Thất bại khi tài khoản người dùng đã bị quản trị viên khóa (isActive = false)")
    void getMe_userLocked_throwsForbiddenException() {
        mockUserEntity.setIsActive(false);

        when(securityService.getPayloadFromAccessToken(VALID_AT)).thenReturn(mockAtPayload);
        when(securityService.getPayloadFromRefreshToken(VALID_RT)).thenReturn(mockRtPayload);
        when(refreshTokenService.exists(JTI, USER_ID)).thenReturn(true);
        when(repo.findById(USER_ID)).thenReturn(mockUserEntity);

        // When & Then
        assertThatThrownBy(() -> userService.getMe(VALID_AT, VALID_RT))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Tài khoản đã bị khoá");

        verify(mapperDTO, never()).toCommonDTO(any());
        verify(s3Service, never()).generatePresignedUrl(anyString());
    }
}
