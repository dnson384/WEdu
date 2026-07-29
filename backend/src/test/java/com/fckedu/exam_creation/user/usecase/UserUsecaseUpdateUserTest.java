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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserUsecaseUpdateUserTest {
    private static final String VALID_AT = "mock-at";
    private static final String INVALID_AT = "invalid-at";
    private static final String VALID_RT = "mock-rt";
    private static final String INVALID_RT = "invalid-rt";

    private static final String USER_ID = "user-123";
    private static final String JTI = "jti-uuid";
    private static final String EMAIL = "user@gmail.com";
    private static final String AVATAR_URL = "avatars/s3key.png";
    private static final String OLD_USERNAME = "old-username";
    private static final String NEW_USERNAME = "new-username-123";
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
        mockOldUserEntity.setUsername(OLD_USERNAME);
        mockOldUserEntity.setIsActive(true);
        mockOldUserEntity.setAvatarUrl(AVATAR_URL);

        mockNewUserEntity = new UserEntity();
        mockNewUserEntity.setId(USER_ID);
        mockNewUserEntity.setUsername(NEW_USERNAME);
        mockNewUserEntity.setIsActive(true);
        mockNewUserEntity.setAvatarUrl(AVATAR_URL);
    }

    @Test
    @DisplayName("Cập nhật username thành công với dữ liệu hợp lệ")
    void happyCase() throws Exception {
        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.save(any(UserEntity.class)))
                .thenReturn(mockNewUserEntity);

        boolean result = userUsecase.updateUser(USER_ID, NEW_USERNAME);

        assertThat(result).isTrue();
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getUsername()).isEqualTo(NEW_USERNAME);
    }

    @Test
    @DisplayName("Cập nhật với username rỗng")
    void emptyUsername() throws Exception {
        assertThatThrownBy(() -> userUsecase.updateUser(USER_ID, ""))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng rỗng");

        verify(repo, times(0)).findById(anyString());
        verify(repo, times(0)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Cập nhật với username là null")
    void nullUsername() throws Exception {
        assertThatThrownBy(() -> userUsecase.updateUser(USER_ID, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng rỗng");

        verify(repo, times(0)).findById(anyString());
        verify(repo, times(0)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Cập nhật với username là chỉ chứa khoảng trắng")
    void onlySpaceUsername() throws Exception {
        assertThatThrownBy(() -> userUsecase.updateUser(USER_ID, "    "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng rỗng");

        verify(repo, times(0)).findById(anyString());
        verify(repo, times(0)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Cập nhật với username quá dài")
    void longUsername() throws Exception {
        String longUsername = "Gió chiều thổi nhẹ qua hàng cây, mang theo mùi cỏ mới cắt và tiếng chim vang xa. Trên con đường nhỏ, vài chiếc lá khô xoay tròn rồi lặng lẽ rơi xuống mặt đất. Không gian yên bình khiến mọi suy nghĩ trở nên chậm lại, như thể thời gian cũng đang tạm dừng để nghỉ ngơi.";
        assertThatThrownBy(() -> userUsecase.updateUser(USER_ID, longUsername))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên người dùng quá dài");

        verify(repo, times(0)).findById(anyString());
        verify(repo, times(0)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Cập nhật với username có số")
    void hasNumUsername() throws Exception {
        String hasNum = "abc123";

        UserEntity mockRes = new UserEntity();
        mockRes.setId(USER_ID);
        mockRes.setUsername(hasNum);
        mockRes.setIsActive(true);
        mockRes.setAvatarUrl(AVATAR_URL);

        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.save(any(UserEntity.class)))
                .thenReturn(mockRes);

        boolean result = userUsecase.updateUser(USER_ID, hasNum);

        assertThat(result).isTrue();
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getUsername()).isEqualTo(hasNum);
    }

    @Test
    @DisplayName("Cập nhật với username có emoji")
    void hasEmojiUsername() throws Exception {
        String hasEmoji = "Nguyen Van A \uD83D\uDE00";

        UserEntity mockRes = new UserEntity();
        mockRes.setId(USER_ID);
        mockRes.setUsername(hasEmoji);
        mockRes.setIsActive(true);
        mockRes.setAvatarUrl(AVATAR_URL);

        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.save(any(UserEntity.class)))
                .thenReturn(mockRes);

        boolean result = userUsecase.updateUser(USER_ID, hasEmoji);

        assertThat(result).isTrue();
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getUsername()).isEqualTo(hasEmoji);
    }

    @Test
    @DisplayName("Cập nhật với username có unicode")
    void hasUnicodeUsername() throws Exception {
        String hasUnicode = "Nguyễn Văn A";

        UserEntity mockRes = new UserEntity();
        mockRes.setId(USER_ID);
        mockRes.setUsername(hasUnicode);
        mockRes.setIsActive(true);
        mockRes.setAvatarUrl(AVATAR_URL);

        when(repo.findById(USER_ID))
                .thenReturn(mockOldUserEntity);
        when(repo.save(any(UserEntity.class)))
                .thenReturn(mockRes);

        boolean result = userUsecase.updateUser(USER_ID, hasUnicode);

        assertThat(result).isTrue();
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getUsername()).isEqualTo(hasUnicode);
    }

    @Test
    @DisplayName("Lỗi trong quá trình cập nhật thông tin tài khoản")
    void saveFailure() throws Exception {
        when(repo.findById(anyString()))
                .thenReturn(mockOldUserEntity);
        when(repo.save(any(UserEntity.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> userUsecase.updateUser(USER_ID, NEW_USERNAME))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Có lỗi trong quá trình cập nhật tài khoản");

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Update khi user đã bị khóa")
    void lockedUser() throws Exception {
        UserEntity lockedUser = new UserEntity();
        lockedUser.setId(USER_ID);
        lockedUser.setUsername(OLD_USERNAME);
        lockedUser.setAvatarUrl(AVATAR_URL);
        lockedUser.setIsActive(false);

        when(repo.findById(anyString()))
                .thenReturn(lockedUser);

        assertThatThrownBy(() -> userUsecase.updateUser(USER_ID, NEW_USERNAME))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Tài khoản đã bị khóa");

        verify(repo, times(1)).findById(anyString());
        verify(repo, times(0)).save(any(UserEntity.class));
    }
}
