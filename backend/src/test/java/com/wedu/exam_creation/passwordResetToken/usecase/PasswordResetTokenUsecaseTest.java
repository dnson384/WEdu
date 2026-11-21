package com.wedu.exam_creation.passwordResetToken.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.mail.service.MailService;
import com.wedu.exam_creation.passwordResetToken.domain.entity.PasswordResetTokenEntity;
import com.wedu.exam_creation.passwordResetToken.domain.repository.IPasswordResetTokenRepository;
import com.wedu.exam_creation.security.service.SecurityService;
import com.wedu.exam_creation.user.usecase.UserService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mail.MailSendException;

class PasswordResetTokenUsecaseTest {
    private static final String EMAIL = "teacher@example.test";
    private static final String USER_ID = "user-1";
    private static final String FRONTEND = "https://wedu.example.test";
    private static final String TOKEN = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String PASSWORD = "test-password";
    private static final Instant NOW = Instant.parse("2026-11-21T00:00:00Z");
    private static final Instant EXPIRY = Instant.parse("2026-11-21T00:15:00Z");

    private final UserService users = mock(UserService.class);
    private final IPasswordResetTokenRepository repository =
            mock(IPasswordResetTokenRepository.class);
    private final SecurityService security = mock(SecurityService.class);
    private final MailService mail = mock(MailService.class);
    private final PasswordResetTokenUsecase usecase =
            new PasswordResetTokenUsecase(users, repository, security, mail, FRONTEND);

    @Test
    @DisplayName(
            "trả về không có ngoại lệ khi lưu hash token hạn 15 phút và gửi liên kết đặt lại mật khẩu")
    void test_requestReset_001() throws NoSuchAlgorithmException {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user(USER_ID)));

        requestWithFixedToken();

        ArgumentCaptor<PasswordResetTokenEntity> saved =
                ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        InOrder order = inOrder(repository, mail);
        order.verify(repository).deleteByUserId(USER_ID);
        order.verify(repository).save(saved.capture());
        order.verify(mail)
                .sendResetPasswordEmail(EMAIL, FRONTEND + "/auth/reset-password?token=" + TOKEN);
        assertThat(saved.getValue())
                .isEqualTo(
                        new PasswordResetTokenEntity(
                                null, tokenHash(), USER_ID, EXPIRY, false, NOW));
    }

    @Test
    @DisplayName("trả về NotFoundException khi yêu cầu đặt lại mật khẩu cho email chưa đăng ký")
    void test_requestReset_002() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usecase.requestReset(EMAIL)).isInstanceOf(NotFoundException.class);

        verifyNoInteractions(repository, mail);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("trả về BadRequestException khi tài khoản thiếu ID hợp lệ")
    void test_requestReset_003(String userId) {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user(userId)));

        assertThatThrownBy(() -> usecase.requestReset(EMAIL))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(repository, mail);
    }

    @Test
    @DisplayName("trả về lỗi lưu token và không gửi email khi kho lưu trữ thất bại")
    void test_requestReset_004() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user(USER_ID)));
        IllegalStateException failure = new IllegalStateException("storage unavailable");
        doThrow(failure).when(repository).save(any(PasswordResetTokenEntity.class));

        assertThatThrownBy(this::requestWithFixedToken).isSameAs(failure);

        verifyNoInteractions(mail);
    }

    @Test
    @DisplayName("trả về MailSendException khi gửi liên kết thất bại sau khi lưu token")
    void test_requestReset_005() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user(USER_ID)));
        MailSendException failure = new MailSendException("mail unavailable");
        doThrow(failure).when(mail).sendResetPasswordEmail(eq(EMAIL), anyString());

        assertThatThrownBy(this::requestWithFixedToken).isSameAs(failure);

        InOrder order = inOrder(repository, mail);
        order.verify(repository).save(any(PasswordResetTokenEntity.class));
        order.verify(mail).sendResetPasswordEmail(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName(
            "trả về không có ngoại lệ khi cập nhật mật khẩu đã hash rồi đánh dấu token đã dùng")
    void test_resetPassword_006() throws NoSuchAlgorithmException {
        PasswordResetTokenEntity token = token(EXPIRY);
        CommonUserResponseAllDTO account = user(USER_ID);
        when(repository.findByTokenHashAndUsedFalse(token.getTokenHash())).thenReturn(token);
        when(users.findById(USER_ID)).thenReturn(account);
        when(security.hashPassword(PASSWORD)).thenReturn("hashed-test-password");

        resetAtFixedTime();

        InOrder order = inOrder(security, users, repository);
        order.verify(security).hashPassword(PASSWORD);
        order.verify(users).updatePassword(account);
        order.verify(repository).save(token);
        assertThat(account.getHashedPassword()).isEqualTo("hashed-test-password");
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    @DisplayName("trả về BadRequestException khi mật khẩu xác nhận không khớp")
    void test_resetPassword_007() {
        assertThatThrownBy(() -> usecase.resetPassword(TOKEN, PASSWORD, "different"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Mật khẩu xác nhận không trùng khớp");

        verifyNoInteractions(repository, users, security);
    }

    @Test
    @DisplayName("trả về BadRequestException khi không tìm được token chưa sử dụng")
    void test_resetPassword_008() throws NoSuchAlgorithmException {
        when(repository.findByTokenHashAndUsedFalse(tokenHash())).thenReturn(null);

        assertThatThrownBy(this::resetAtFixedTime)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Token không hợp lệ hoặc đã được sử dụng");

        verifyNoInteractions(users, security);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("trả về BadRequestException khi token đã quá hạn")
    void test_resetPassword_009() throws NoSuchAlgorithmException {
        PasswordResetTokenEntity token = token(NOW.minusSeconds(1));
        when(repository.findByTokenHashAndUsedFalse(token.getTokenHash())).thenReturn(token);

        assertThatThrownBy(this::resetAtFixedTime)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Token đã hết hạn");

        verifyNoInteractions(users, security);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("trả về NotFoundException khi tài khoản gắn với token không còn tồn tại")
    void test_resetPassword_010() throws NoSuchAlgorithmException {
        PasswordResetTokenEntity token = token(EXPIRY);
        when(repository.findByTokenHashAndUsedFalse(token.getTokenHash())).thenReturn(token);
        when(users.findById(USER_ID)).thenReturn(null);

        assertThatThrownBy(this::resetAtFixedTime).isInstanceOf(NotFoundException.class);

        verifyNoInteractions(security);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("trả về lỗi cập nhật và giữ token chưa dùng khi lưu mật khẩu thất bại")
    void test_resetPassword_011() throws NoSuchAlgorithmException {
        PasswordResetTokenEntity token = token(EXPIRY);
        CommonUserResponseAllDTO account = user(USER_ID);
        when(repository.findByTokenHashAndUsedFalse(token.getTokenHash())).thenReturn(token);
        when(users.findById(USER_ID)).thenReturn(account);
        when(security.hashPassword(PASSWORD)).thenReturn("hashed-test-password");
        IllegalStateException failure = new IllegalStateException("user update failed");
        doThrow(failure).when(users).updatePassword(account);

        assertThatThrownBy(this::resetAtFixedTime).isSameAs(failure);

        assertThat(token.isUsed()).isFalse();
        verify(repository, never()).save(any());
    }

    private void requestWithFixedToken() {
        try (MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS);
                MockedConstruction<SecureRandom> random = mockConstruction(SecureRandom.class)) {
            clock.when(Instant::now).thenReturn(NOW);
            usecase.requestReset(EMAIL);
        }
    }

    private void resetAtFixedTime() {
        try (MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            clock.when(Instant::now).thenReturn(NOW);
            usecase.resetPassword(TOKEN, PASSWORD, PASSWORD);
        }
    }

    private CommonUserResponseAllDTO user(String id) {
        CommonUserResponseAllDTO user = new CommonUserResponseAllDTO();
        user.setId(id);
        user.setEmail(EMAIL);
        return user;
    }

    private PasswordResetTokenEntity token(Instant expiry) throws NoSuchAlgorithmException {
        return new PasswordResetTokenEntity("token-1", tokenHash(), USER_ID, expiry, false, NOW);
    }

    private String tokenHash() throws NoSuchAlgorithmException {
        return HexFormat.of()
                .formatHex(
                        MessageDigest.getInstance("SHA-256")
                                .digest(TOKEN.getBytes(StandardCharsets.UTF_8)));
    }
}
