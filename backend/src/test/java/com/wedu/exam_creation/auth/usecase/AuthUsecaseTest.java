package com.wedu.exam_creation.auth.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.auth.dto.mapper.AuthDTOMapper;
import com.wedu.exam_creation.auth.dto.request.ChangePasswordRequestDTO;
import com.wedu.exam_creation.auth.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.common.dto.refreshToken.request.NewRTRequestDTO;
import com.wedu.exam_creation.common.dto.refreshToken.response.RTResponseDTO;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.token.RTPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.passwordResetToken.usecase.PasswordResetTokenService;
import com.wedu.exam_creation.refreshToken.usecase.RefreshTokenService;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.user.dto.request.LoginUserRequestDTO;
import com.wedu.exam_creation.user.usecase.UserService;
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@ExtendWith(MockitoExtension.class)
public class AuthUsecaseTest {
    @Mock private AuthDTOMapper mapperDTO;
    @Mock private SecurityService securityService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private PasswordResetTokenService passwordResetTokenService;
    @Mock private UserService userService;

    private AuthUsecase authUsecase;

    private LoginUserRequestDTO loginRequest;
    private CommonUserResponseAllDTO activeLocalUser;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        accessTokenPayload =
                new ATPayload(PARENT_JTI, "user-123", "user@gmail.com", "ROLE_TEACHER");
        authUsecase =
                new AuthUsecase(
                        userService,
                        securityService,
                        refreshTokenService,
                        passwordResetTokenService,
                        mapperDTO);

        loginRequest = new LoginUserRequestDTO();
        loginRequest.setEmail("anv@gmail.com");
        loginRequest.setPlainPassword("Password123@");

        activeLocalUser =
                new CommonUserResponseAllDTO(
                        "user-123",
                        "anv@gmail.com",
                        "hashed-password",
                        "Nguyen Van A",
                        "ROLE_TEACHER",
                        "LOCAL",
                        "avatars/default-avatar-user.png",
                        true, // isActive
                        "FREE",
                        java.time.Instant.parse("2026-11-13T00:00:00Z"),
                        java.time.Instant.parse("2026-11-13T00:00:00Z"),
                        null);

        userResponseDTO =
                new UserResponseDTO(
                        "user-123",
                        "anv@gmail.com",
                        "Nguyen Van A",
                        "ROLE_TEACHER",
                        "avatars/default-avatar-user.png");
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOCAL", "MIXED"})
    @DisplayName("trả về user và hai token khi credential LOCAL hoặc MIXED hợp lệ")
    void test_login_001(String loginMethod) {
        activeLocalUser.setLoginMethod(loginMethod);
        when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(activeLocalUser));
        when(securityService.validatePassword("Password123@", "hashed-password")).thenReturn(true);
        when(mapperDTO.toUserResponseDTO(activeLocalUser)).thenReturn(userResponseDTO);
        when(securityService.generateAccessToken(any(ATPayload.class))).thenReturn("access-token");
        when(securityService.generateRefreshToken(any(RTPayload.class)))
                .thenReturn("refresh-token");
        NewRTRequestDTO parsedToken = mock(NewRTRequestDTO.class);
        when(securityService.parseNewRefreshToken("refresh-token")).thenReturn(parsedToken);
        when(refreshTokenService.save(parsedToken)).thenReturn(true);

        AuthorizedResponseDTO result = authUsecase.login(loginRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getUser()).isEqualTo(userResponseDTO);

        verify(userService, times(1)).findByEmail("anv@gmail.com");
        verify(securityService, times(1)).validatePassword("Password123@", "hashed-password");
        verify(securityService).parseNewRefreshToken("refresh-token");
        verify(refreshTokenService).save(same(parsedToken));

        ArgumentCaptor<ATPayload> atCaptor = ArgumentCaptor.forClass(ATPayload.class);
        ArgumentCaptor<RTPayload> rtCaptor = ArgumentCaptor.forClass(RTPayload.class);
        verify(securityService).generateAccessToken(atCaptor.capture());
        verify(securityService).generateRefreshToken(rtCaptor.capture());

        ATPayload at = atCaptor.getValue();
        RTPayload rt = rtCaptor.getValue();
        assertThat(at.getUserId()).isEqualTo(activeLocalUser.getId());
        assertThat(at.getEmail()).isEqualTo(activeLocalUser.getEmail());
        assertThat(at.getRole()).isEqualTo(activeLocalUser.getRole());
        assertThat(rt.getUserId()).isEqualTo(activeLocalUser.getId());
        assertThat(rt.getEmail()).isEqualTo(activeLocalUser.getEmail());
        assertThat(rt.getRole()).isEqualTo(activeLocalUser.getRole());
        assertThat(rt.getJti()).isNotBlank();
        assertThat(at.getParentJti()).isEqualTo(rt.getJti());
    }

    @Test
    @DisplayName("trả về UnAuthorizedException khi tài khoản MIXED không có mật khẩu LOCAL")
    void test_login_002() {
        activeLocalUser.setLoginMethod("MIXED");
        activeLocalUser.setHashedPassword("");
        when(userService.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(activeLocalUser));
        assertThatThrownBy(() -> authUsecase.login(loginRequest))
                .isInstanceOf(UnAuthorizedException.class);
        verifyNoInteractions(securityService, refreshTokenService);
    }

    @Test
    @DisplayName("trả về NotFoundException khi email không tồn tại")
    void test_login_003() {
        when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authUsecase.login(loginRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Tài khoản chưa tồn tại");

        verifyNoInteractions(securityService, mapperDTO, refreshTokenService);
    }

    @Test
    @DisplayName("trả về UnAuthorizedException khi tài khoản GOOGLE đăng nhập LOCAL")
    void test_login_004() {
        CommonUserResponseAllDTO googleUser =
                new CommonUserResponseAllDTO(
                        "user-456",
                        "anv@gmail.com",
                        null,
                        "Nguyen Van A",
                        "ROLE_TEACHER",
                        "GOOGLE",
                        "avatars/default-avatar-user.png",
                        true,
                        "FREE",
                        java.time.Instant.parse("2026-11-13T00:00:00Z"),
                        java.time.Instant.parse("2026-11-13T00:00:00Z"),
                        null);
        when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(googleUser));

        assertThatThrownBy(() -> authUsecase.login(loginRequest))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("Sai phương thức đăng nhập");

        verifyNoInteractions(securityService, mapperDTO, refreshTokenService);
    }

    @Test
    @DisplayName("trả về ForbiddenException khi tài khoản đã bị khóa")
    void test_login_005() {
        CommonUserResponseAllDTO lockedUser =
                new CommonUserResponseAllDTO(
                        "user-789",
                        "anv@gmail.com",
                        "hashed-password",
                        "Nguyen Van A",
                        "ROLE_TEACHER",
                        "LOCAL",
                        "avatars/default-avatar-user.png",
                        false,
                        "FREE",
                        java.time.Instant.parse("2026-11-13T00:00:00Z"),
                        java.time.Instant.parse("2026-11-13T00:00:00Z"),
                        null);
        when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(lockedUser));

        assertThatThrownBy(() -> authUsecase.login(loginRequest))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Tài khoản đã bị khóa! Vui lòng liên hệ xxx để được mở khóa");

        verifyNoInteractions(securityService, mapperDTO, refreshTokenService);
    }

    @Test
    @DisplayName("trả về UnAuthorizedException khi mật khẩu không đúng")
    void test_login_006() {
        when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(activeLocalUser));
        when(securityService.validatePassword("Password123@", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authUsecase.login(loginRequest))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("Mật khẩu không chính xác");

        verify(securityService).validatePassword("Password123@", "hashed-password");
        verifyNoMoreInteractions(securityService);
        verifyNoInteractions(mapperDTO, refreshTokenService);
    }

    @Test
    @DisplayName("trả về InternalServerException khi lưu refresh token thất bại")
    void test_login_007() {
        when(userService.findByEmail("anv@gmail.com")).thenReturn(Optional.of(activeLocalUser));
        when(securityService.validatePassword("Password123@", "hashed-password")).thenReturn(true);
        when(mapperDTO.toUserResponseDTO(activeLocalUser)).thenReturn(userResponseDTO);
        when(securityService.generateAccessToken(any(ATPayload.class))).thenReturn("access-token");
        when(securityService.generateRefreshToken(any(RTPayload.class)))
                .thenReturn("refresh-token");
        when(securityService.parseNewRefreshToken("refresh-token"))
                .thenReturn(mock(NewRTRequestDTO.class));
        when(refreshTokenService.save(any(NewRTRequestDTO.class))).thenReturn(false);

        assertThatThrownBy(() -> authUsecase.login(loginRequest))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Lỗi trong quá trình đăng nhập");
    }

    private static final String ACCESS_TOKEN = "valid-access-token";
    private static final String AUTHORIZATION = "Bearer " + ACCESS_TOKEN;
    private static final String PARENT_JTI = "jti-123456";

    private ATPayload accessTokenPayload;

    @Test
    @DisplayName("trả về true khi xóa refresh token thành công")
    void test_logout_001() {
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN))
                .thenReturn(accessTokenPayload);
        when(refreshTokenService.delete(PARENT_JTI)).thenReturn(true);

        assertThat(authUsecase.logout(AUTHORIZATION)).isTrue();

        verifyTokenParsingAndDeletion();
    }

    @Test
    @DisplayName("trả về false khi service xóa refresh token trả về false")
    void test_logout_002() {
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN))
                .thenReturn(accessTokenPayload);
        when(refreshTokenService.delete(PARENT_JTI)).thenReturn(false);

        assertThat(authUsecase.logout(AUTHORIZATION)).isFalse();

        verifyTokenParsingAndDeletion();
    }

    @Test
    @DisplayName("trả về NotFoundException khi refresh token không tồn tại")
    void test_logout_003() {
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN))
                .thenReturn(accessTokenPayload);
        NotFoundException failure = new NotFoundException("Không tìm thấy RT");
        when(refreshTokenService.delete(PARENT_JTI)).thenThrow(failure);

        assertThatThrownBy(() -> authUsecase.logout(AUTHORIZATION))
                .isSameAs(failure)
                .hasMessage("Không tìm thấy RT");

        verifyTokenParsingAndDeletion();
    }

    @Test
    @DisplayName("trả về InternalServerException khi xóa refresh token thất bại")
    void test_logout_004() {
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN))
                .thenReturn(accessTokenPayload);
        InternalServerException failure =
                new InternalServerException("Dữ liệu không nhất quán, xóa dư");
        when(refreshTokenService.delete(PARENT_JTI)).thenThrow(failure);

        assertThatThrownBy(() -> authUsecase.logout(AUTHORIZATION))
                .isSameAs(failure)
                .hasMessage("Dữ liệu không nhất quán, xóa dư");

        verifyTokenParsingAndDeletion();
    }

    @Test
    @DisplayName(
            "trả về UnAuthorizedException và không xóa refresh token khi parse access token thất bại")
    void test_logout_005() {
        UnAuthorizedException failure = new UnAuthorizedException("Access Token không hợp lệ");
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN)).thenThrow(failure);

        assertThatThrownBy(() -> authUsecase.logout(AUTHORIZATION))
                .isSameAs(failure)
                .hasMessage("Access Token không hợp lệ");

        verify(securityService).getPayloadFromAccessToken(ACCESS_TOKEN);
        verifyNoMoreInteractions(securityService);
        verifyNoInteractions(
                refreshTokenService, userService, mapperDTO, passwordResetTokenService);
    }

    private void verifyTokenParsingAndDeletion() {
        verify(securityService).getPayloadFromAccessToken(ACCESS_TOKEN);
        verify(refreshTokenService).delete(PARENT_JTI);
        verifyNoMoreInteractions(securityService, refreshTokenService);
        verifyNoInteractions(userService, mapperDTO, passwordResetTokenService);
    }

    @Test
    @DisplayName("trả về access token mới khi refresh token thuộc phiên còn tồn tại")
    void test_regenerateAccessToken_001() {
        RTPayload payload = new RTPayload(PARENT_JTI, "user-123", "anv@gmail.com", "ROLE_TEACHER");
        RTResponseDTO stored = mock(RTResponseDTO.class);
        when(stored.getUserId()).thenReturn("user-123");
        when(securityService.getPayloadFromRefreshToken("rt")).thenReturn(payload);
        when(refreshTokenService.getRefreshToken(PARENT_JTI, "user-123")).thenReturn(stored);
        when(userService.findById("user-123")).thenReturn(activeLocalUser);
        when(securityService.generateAccessToken(any())).thenReturn("new-at");

        assertThat(authUsecase.regenerateAccessToken("rt")).isEqualTo("new-at");
        verify(securityService).validateRefreshToken("rt");
        ArgumentCaptor<ATPayload> access = ArgumentCaptor.forClass(ATPayload.class);
        verify(securityService).generateAccessToken(access.capture());
        assertThat(access.getValue().getParentJti()).isEqualTo(PARENT_JTI);
        assertThat(access.getValue().getUserId()).isEqualTo("user-123");
        assertThat(access.getValue().getRole()).isEqualTo("ROLE_TEACHER");
    }

    @Test
    @DisplayName("trả về UnAuthorizedException khi refresh token đã bị thu hồi")
    void test_regenerateAccessToken_002() {
        when(securityService.getPayloadFromRefreshToken("rt"))
                .thenReturn(new RTPayload(PARENT_JTI, "user-123", "anv@gmail.com", "ROLE_TEACHER"));
        assertThatThrownBy(() -> authUsecase.regenerateAccessToken("rt"))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessageContaining("thu hồi");
        verifyNoInteractions(userService);
        verify(securityService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("trả về NotFoundException khi user của refresh token không tồn tại")
    void test_regenerateAccessToken_003() {
        RTResponseDTO stored = mock(RTResponseDTO.class);
        when(stored.getUserId()).thenReturn("missing");
        when(securityService.getPayloadFromRefreshToken("rt"))
                .thenReturn(new RTPayload(PARENT_JTI, "missing", "anv@gmail.com", "ROLE_TEACHER"));
        when(refreshTokenService.getRefreshToken(PARENT_JTI, "missing")).thenReturn(stored);
        assertThatThrownBy(() -> authUsecase.regenerateAccessToken("rt"))
                .isInstanceOf(NotFoundException.class);
        verify(securityService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("trả về UnAuthorizedException khi parse refresh token ném JwtException")
    void test_regenerateAccessToken_004() {
        when(securityService.getPayloadFromRefreshToken("rt"))
                .thenThrow(new JwtException("invalid"));
        assertThatThrownBy(() -> authUsecase.regenerateAccessToken("rt"))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("RT không hợp lệ");
        verifyNoInteractions(refreshTokenService, userService);
    }

    @Test
    @DisplayName("trả về lỗi xác thực trước lookup khi validate refresh token thất bại")
    void test_regenerateAccessToken_005() {
        doThrow(new UnAuthorizedException("expired"))
                .when(securityService)
                .validateRefreshToken("rt");
        assertThatThrownBy(() -> authUsecase.regenerateAccessToken("rt"))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("expired");
        verify(securityService, never()).getPayloadFromRefreshToken(any());
        verifyNoInteractions(refreshTokenService, userService);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi yêu cầu đặt lại mật khẩu bằng email hợp lệ")
    void test_forgotPassword_001() {
        authUsecase.forgotPassword("teacher@example.com");
        verify(passwordResetTokenService).requestReset("teacher@example.com");
    }

    @Test
    @DisplayName("trả về BadRequestException khi email đặt lại mật khẩu sai định dạng")
    void test_forgotPassword_002() {
        assertThatThrownBy(() -> authUsecase.forgotPassword("invalid"))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(passwordResetTokenService);
    }

    @Test
    @DisplayName("trả về lỗi dịch vụ khi gửi yêu cầu đặt lại mật khẩu thất bại")
    void test_forgotPassword_003() {
        doThrow(new InternalServerException("delivery failed"))
                .when(passwordResetTokenService)
                .requestReset("teacher@example.com");
        assertThatThrownBy(() -> authUsecase.forgotPassword("teacher@example.com"))
                .isInstanceOf(InternalServerException.class);
    }

    @Test
    @DisplayName(
            "trả về không có ngoại lệ khi dịch vụ đặt lại mật khẩu chấp nhận token và mật khẩu")
    void test_resetPassword_001() {
        authUsecase.resetPassword("reset-token", "password123", "password123");
        verify(passwordResetTokenService)
                .resetPassword("reset-token", "password123", "password123");
    }

    @Test
    @DisplayName("trả về lỗi xác thực khi dịch vụ từ chối token đặt lại mật khẩu")
    void test_resetPassword_002() {
        doThrow(new UnAuthorizedException("expired"))
                .when(passwordResetTokenService)
                .resetPassword("expired", "password123", "password123");
        assertThatThrownBy(() -> authUsecase.resetPassword("expired", "password123", "password123"))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("expired");
    }

    private ChangePasswordRequestDTO passwordChange() {
        return new ChangePasswordRequestDTO("old-password", "new-password", "new-password");
    }

    @Test
    @DisplayName("trả về true và giữ phiên hiện tại khi đổi mật khẩu hợp lệ")
    void test_changePassword_001() {
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN))
                .thenReturn(accessTokenPayload);
        when(userService.findById("user-123")).thenReturn(activeLocalUser);
        when(securityService.validatePassword("old-password", "hashed-password")).thenReturn(true);
        when(securityService.hashPassword("new-password")).thenReturn("new-hash");
        when(userService.updatePassword(activeLocalUser)).thenReturn(activeLocalUser);
        when(refreshTokenService.getUserRefreshToken("user-123")).thenReturn(List.of());

        assertThat(authUsecase.changePassword(ACCESS_TOKEN, "user-123", passwordChange())).isTrue();
        assertThat(activeLocalUser.getHashedPassword()).isEqualTo("new-hash");
        verify(refreshTokenService, never()).deleteMany(any());
    }

    @Test
    @DisplayName("trả về UnAuthorizedException trước lookup khi xác nhận mật khẩu mới không khớp")
    void test_changePassword_002() {
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN))
                .thenReturn(accessTokenPayload);
        ChangePasswordRequestDTO input = passwordChange();
        input.setConfirmNewPassword("different");
        assertThatThrownBy(() -> authUsecase.changePassword(ACCESS_TOKEN, "user-123", input))
                .isInstanceOf(UnAuthorizedException.class);
        verifyNoInteractions(userService, refreshTokenService);
    }

    @Test
    @DisplayName("trả về NotFoundException khi tài khoản đổi mật khẩu không tồn tại")
    void test_changePassword_003() {
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN))
                .thenReturn(accessTokenPayload);
        assertThatThrownBy(
                        () -> authUsecase.changePassword(ACCESS_TOKEN, "missing", passwordChange()))
                .isInstanceOf(NotFoundException.class);
        verify(userService, never()).updatePassword(any());
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("trả về UnAuthorizedException khi mật khẩu cũ không đúng")
    void test_changePassword_004() {
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN))
                .thenReturn(accessTokenPayload);
        when(userService.findById("user-123")).thenReturn(activeLocalUser);
        assertThatThrownBy(
                        () ->
                                authUsecase.changePassword(
                                        ACCESS_TOKEN, "user-123", passwordChange()))
                .isInstanceOf(UnAuthorizedException.class);
        verify(userService, never()).updatePassword(any());
        verify(securityService, never()).hashPassword(any());
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("trả về true và chỉ thu hồi phiên khác khi lưu mật khẩu và xóa token thành công")
    void test_saveUserAndRevokeRT_001() {
        RTResponseDTO current = mock(RTResponseDTO.class);
        RTResponseDTO other = mock(RTResponseDTO.class);
        when(current.getJti()).thenReturn(PARENT_JTI);
        when(other.getJti()).thenReturn("other-jti");
        when(userService.updatePassword(activeLocalUser)).thenReturn(activeLocalUser);
        when(refreshTokenService.getUserRefreshToken("user-123"))
                .thenReturn(List.of(current, other));
        when(refreshTokenService.deleteMany(List.of("other-jti"))).thenReturn(true);
        assertThat(authUsecase.saveUserAndRevokeRT(activeLocalUser, PARENT_JTI)).isTrue();
        verify(refreshTokenService).deleteMany(List.of("other-jti"));
    }

    @Test
    @DisplayName("trả về false và không thu hồi token khi cập nhật mật khẩu không trả user")
    void test_saveUserAndRevokeRT_002() {
        assertThat(authUsecase.saveUserAndRevokeRT(activeLocalUser, PARENT_JTI)).isFalse();
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("trả về false và đánh dấu rollback khi repository cập nhật mật khẩu ném lỗi")
    void test_saveUserAndRevokeRT_003() {
        when(userService.updatePassword(activeLocalUser))
                .thenThrow(new IllegalStateException("database unavailable"));
        TransactionStatus transaction = mock(TransactionStatus.class);
        try (MockedStatic<TransactionAspectSupport> transactions =
                mockStatic(TransactionAspectSupport.class)) {
            transactions
                    .when(TransactionAspectSupport::currentTransactionStatus)
                    .thenReturn(transaction);
            assertThat(authUsecase.saveUserAndRevokeRT(activeLocalUser, PARENT_JTI)).isFalse();
            verify(transaction).setRollbackOnly();
        }
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("trả về false khi dịch vụ thu hồi các phiên khác trả false")
    void test_saveUserAndRevokeRT_004() {
        RTResponseDTO other = mock(RTResponseDTO.class);
        when(other.getJti()).thenReturn("other-jti");
        when(userService.updatePassword(activeLocalUser)).thenReturn(activeLocalUser);
        when(refreshTokenService.getUserRefreshToken("user-123")).thenReturn(List.of(other));
        assertThat(authUsecase.saveUserAndRevokeRT(activeLocalUser, PARENT_JTI)).isFalse();
        verify(refreshTokenService).deleteMany(List.of("other-jti"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName(
            "trả về token và kết quả lưu refresh tương ứng khi persistence thành công hoặc từ chối")
    void test_generateToken_001(boolean saved) {
        NewRTRequestDTO metadata = mock(NewRTRequestDTO.class);
        when(securityService.generateAccessToken(any())).thenReturn("at");
        when(securityService.generateRefreshToken(any())).thenReturn("rt");
        when(securityService.parseNewRefreshToken("rt")).thenReturn(metadata);
        when(refreshTokenService.save(metadata)).thenReturn(saved);

        AuthUsecase.TokenResult result = authUsecase.generateToken(userResponseDTO);
        assertThat(result.saved()).isEqualTo(saved);
        assertThat(result.accessToken()).isEqualTo("at");
        assertThat(result.refreshToken()).isEqualTo("rt");
        ArgumentCaptor<ATPayload> access = ArgumentCaptor.forClass(ATPayload.class);
        ArgumentCaptor<RTPayload> refresh = ArgumentCaptor.forClass(RTPayload.class);
        verify(securityService).generateAccessToken(access.capture());
        verify(securityService).generateRefreshToken(refresh.capture());
        assertThat(access.getValue().getParentJti())
                .isEqualTo(refresh.getValue().getJti())
                .isNotBlank();
        assertThat(refresh.getValue().getUserId()).isEqualTo("user-123");
        assertThat(refresh.getValue().getEmail()).isEqualTo("anv@gmail.com");
        assertThat(refresh.getValue().getRole()).isEqualTo("ROLE_TEACHER");
    }

    @Test
    @DisplayName("trả về lỗi dịch vụ khi lưu refresh metadata ném ngoại lệ")
    void test_generateToken_002() {
        when(securityService.generateAccessToken(any())).thenReturn("at");
        when(securityService.generateRefreshToken(any())).thenReturn("rt");
        NewRTRequestDTO metadata = mock(NewRTRequestDTO.class);
        when(securityService.parseNewRefreshToken("rt")).thenReturn(metadata);
        when(refreshTokenService.save(metadata))
                .thenThrow(new InternalServerException("database unavailable"));
        assertThatThrownBy(() -> authUsecase.generateToken(userResponseDTO))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("database unavailable");
    }

    @Test
    @DisplayName(
            "trả về UnAuthorizedException trước lookup khi access token đổi mật khẩu không hợp lệ")
    void test_changePassword_005() {
        when(securityService.getPayloadFromAccessToken(ACCESS_TOKEN))
                .thenThrow(new UnAuthorizedException("invalid"));
        assertThatThrownBy(
                        () ->
                                authUsecase.changePassword(
                                        ACCESS_TOKEN, "user-123", passwordChange()))
                .isInstanceOf(UnAuthorizedException.class);
        verifyNoInteractions(userService, refreshTokenService);
    }

    @Test
    @DisplayName("trả về true không thu hồi phiên hiện tại khi không có phiên khác")
    void test_saveUserAndRevokeRT_005() {
        RTResponseDTO current = mock(RTResponseDTO.class);
        when(current.getJti()).thenReturn(PARENT_JTI);
        when(userService.updatePassword(activeLocalUser)).thenReturn(activeLocalUser);
        when(refreshTokenService.getUserRefreshToken("user-123")).thenReturn(List.of(current));
        assertThat(authUsecase.saveUserAndRevokeRT(activeLocalUser, PARENT_JTI)).isTrue();
        verify(refreshTokenService, never()).deleteMany(any());
    }
}
