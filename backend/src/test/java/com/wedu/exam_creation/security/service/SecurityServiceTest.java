package com.wedu.exam_creation.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.refreshToken.request.NewRTRequestDTO;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.token.RTPayload;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityServiceTest {
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtTokenProvider provider = mock(JwtTokenProvider.class);
    private final SecurityService service = new SecurityService(encoder, provider);
    private final ATPayload accessPayload =
            new ATPayload("parent-jti", "user", "teacher@example.test", "ROLE_TEACHER");
    private final RTPayload refreshPayload =
            new RTPayload("jti", "user", "teacher@example.test", "ROLE_TEACHER");
    private final NewRTRequestDTO metadata = new NewRTRequestDTO();

    @Test
    @DisplayName("trả về mật khẩu đã hash khi encoder xử lý mật khẩu đầu vào")
    void test_hashPassword_001() {
        when(encoder.encode("plain")).thenReturn("hash");

        assertThat(service.hashPassword("plain")).isEqualTo("hash");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("trả về kết quả từ encoder khi kiểm tra mật khẩu với hash")
    void test_validatePassword_002(boolean matches) {
        when(encoder.matches("plain", "hash")).thenReturn(matches);

        assertThat(service.validatePassword("plain", "hash")).isEqualTo(matches);
    }

    @Test
    @DisplayName("trả về access JWT từ provider khi phát token")
    void test_generateAccessToken_003() {
        when(provider.generateAccessToken(accessPayload)).thenReturn("access-token");

        assertThat(service.generateAccessToken(accessPayload)).isEqualTo("access-token");
    }

    @Test
    @DisplayName("trả về refresh JWT từ provider khi phát token")
    void test_generateRefreshToken_004() {
        when(provider.generateRefreshToken(refreshPayload)).thenReturn("refresh-token");

        assertThat(service.generateRefreshToken(refreshPayload)).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("trả về access payload khi provider đọc token hợp lệ")
    void test_getPayloadFromAccessToken_005() {
        when(provider.getPayloadFromAccessToken("access-token")).thenReturn(accessPayload);

        assertThat(service.getPayloadFromAccessToken("access-token")).isEqualTo(accessPayload);
    }

    @Test
    @DisplayName("trả về refresh payload khi provider đọc token hợp lệ")
    void test_getPayloadFromRefreshToken_006() {
        when(provider.getPayloadFromRefreshToken("refresh-token")).thenReturn(refreshPayload);

        assertThat(service.getPayloadFromRefreshToken("refresh-token")).isEqualTo(refreshPayload);
    }

    @Test
    @DisplayName("trả về metadata mới khi provider đọc refresh token")
    void test_parseNewRefreshToken_007() {
        when(provider.parseNewRefreshToken("refresh-token")).thenReturn(metadata);

        assertThat(service.parseNewRefreshToken("refresh-token")).isEqualTo(metadata);
    }

    @Test
    @DisplayName(
            "trả về UnAuthorizedException với thông báo chung khi provider không đọc được access token")
    void test_getPayloadFromAccessToken_008() {
        when(provider.getPayloadFromAccessToken("invalid"))
                .thenThrow(new IllegalArgumentException("test failure"));

        assertThatThrownBy(() -> service.getPayloadFromAccessToken("invalid"))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("Token không hợp lệ");
    }

    @Test
    @DisplayName(
            "trả về UnAuthorizedException với thông báo chung khi provider không đọc được refresh token")
    void test_getPayloadFromRefreshToken_009() {
        when(provider.getPayloadFromRefreshToken("invalid"))
                .thenThrow(new IllegalArgumentException("test failure"));

        assertThatThrownBy(() -> service.getPayloadFromRefreshToken("invalid"))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("Token không hợp lệ");
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi provider chấp nhận access token")
    void test_validateAccessToken_010() {
        service.validateAccessToken("access-token");

        verify(provider).validateAccessToken("access-token");
    }

    @Test
    @DisplayName("trả về ngoại lệ gốc khi provider từ chối refresh token")
    void test_validateRefreshToken_011() {
        UnAuthorizedException failure = new UnAuthorizedException("Refresh Token đã hết hạn");
        doThrow(failure).when(provider).validateRefreshToken("expired");

        assertThatThrownBy(() -> service.validateRefreshToken("expired")).isSameAs(failure);
    }
}
