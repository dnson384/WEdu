package com.wedu.exam_creation.security.infrastructure.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wedu.exam_creation.common.dto.refreshToken.request.NewRTRequestDTO;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.token.RTPayload;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {
    private static final long ACCESS_TTL = 300_000;
    private static final long REFRESH_TTL = 604_800_000;
    private static final String ACCESS_SECRET = testSecret((byte) 1);
    private static final String REFRESH_SECRET = testSecret((byte) 2);
    private static final Instant ISSUED_AT = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2099-01-01T00:00:00Z");
    private final JwtTokenProvider provider =
            new JwtTokenProvider(ACCESS_SECRET, REFRESH_SECRET, ACCESS_TTL, REFRESH_TTL);

    @Test
    @DisplayName("trả về access JWT có các claim và TTL đúng cấu hình khi phát token")
    void test_generateAccessToken_001() {
        String token =
                provider.generateAccessToken(
                        new ATPayload("jti", "user", "teacher@example.test", "ROLE_TEACHER"));
        Claims claims = inspect(token, ACCESS_SECRET);

        assertThat(claims.getSubject()).isEqualTo("user");
        assertThat(claims.get("parentJti", String.class)).isEqualTo("jti");
        assertThat(claims.get("email", String.class)).isEqualTo("teacher@example.test");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_TEACHER");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(ACCESS_TTL);
    }

    @Test
    @DisplayName("trả về refresh JWT có JTI và TTL đúng cấu hình khi phát token")
    void test_generateRefreshToken_002() {
        String token =
                provider.generateRefreshToken(
                        new RTPayload("jti", "user", "teacher@example.test", "ROLE_TEACHER"));
        Claims claims = inspect(token, REFRESH_SECRET);

        assertThat(claims.getId()).isEqualTo("jti");
        assertThat(claims.getSubject()).isEqualTo("user");
        assertThat(claims.get("email", String.class)).isEqualTo("teacher@example.test");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_TEACHER");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(REFRESH_TTL);
    }

    @Test
    @DisplayName("trả về access payload đúng trường khi đọc access JWT hợp lệ")
    void test_getPayloadFromAccessToken_003() {
        assertThat(provider.getPayloadFromAccessToken(signedFixture(ACCESS_SECRET, EXPIRES_AT)))
                .isEqualTo(new ATPayload("parent", "user", "teacher@example.test", "ROLE_TEACHER"));
    }

    @Test
    @DisplayName("trả về refresh payload đúng trường khi đọc refresh JWT hợp lệ")
    void test_getPayloadFromRefreshToken_004() {
        RTPayload payload =
                provider.getPayloadFromRefreshToken(signedFixture(REFRESH_SECRET, EXPIRES_AT));

        assertThat(payload.getJti()).isEqualTo("jti");
        assertThat(payload.getUserId()).isEqualTo("user");
        assertThat(payload.getEmail()).isEqualTo("teacher@example.test");
        assertThat(payload.getRole()).isEqualTo("ROLE_TEACHER");
    }

    @Test
    @DisplayName("trả về metadata có thời điểm phát và hết hạn khi đọc refresh JWT hợp lệ")
    void test_parseNewRefreshToken_005() {
        assertThat(provider.parseNewRefreshToken(signedFixture(REFRESH_SECRET, EXPIRES_AT)))
                .isEqualTo(new NewRTRequestDTO("jti", "user", EXPIRES_AT, ISSUED_AT));
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi xác thực access JWT hợp lệ")
    void test_validateAccessToken_006() {
        provider.validateAccessToken(signedFixture(ACCESS_SECRET, EXPIRES_AT));
    }

    @Test
    @DisplayName("trả về UnAuthorizedException khi access JWT sai cấu trúc")
    void test_validateAccessToken_007() {
        assertThatThrownBy(() -> provider.validateAccessToken("invalid"))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("Cấu trúc Access Token không hợp lệ");
    }

    @Test
    @DisplayName("trả về UnAuthorizedException khi access JWT đã hết hạn")
    void test_validateAccessToken_008() {
        String expired = signedFixture(ACCESS_SECRET, ISSUED_AT.plusSeconds(1));

        assertThatThrownBy(() -> provider.validateAccessToken(expired))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("Access Token đã hết hạn");
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi xác thực refresh JWT hợp lệ")
    void test_validateRefreshToken_009() {
        provider.validateRefreshToken(signedFixture(REFRESH_SECRET, EXPIRES_AT));
    }

    @Test
    @DisplayName("trả về UnAuthorizedException khi refresh JWT được ký bằng khóa khác")
    void test_validateRefreshToken_010() {
        String accessToken = signedFixture(ACCESS_SECRET, EXPIRES_AT);

        assertThatThrownBy(() -> provider.validateRefreshToken(accessToken))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("Chữ ký Refresh Token không chính xác");
    }

    private Claims inspect(String token, String secret) {
        return Jwts.parser()
                .verifyWith(key(secret))
                .clock(() -> Date.from(ISSUED_AT))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String signedFixture(String secret, Instant expiry) {
        return Jwts.builder()
                .id("jti")
                .subject("user")
                .claim("parentJti", "parent")
                .claim("email", "teacher@example.test")
                .claim("role", "ROLE_TEACHER")
                .issuedAt(Date.from(ISSUED_AT))
                .expiration(Date.from(expiry))
                .signWith(key(secret))
                .compact();
    }

    private static SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    private static String testSecret(byte value) {
        byte[] bytes = new byte[32];
        java.util.Arrays.fill(bytes, value);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
