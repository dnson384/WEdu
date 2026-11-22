package com.wedu.exam_creation.security.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class CustomOAuth2UserServiceTest {
    private static final Instant ISSUED = Instant.parse("2026-11-22T00:00:00Z");
    private static final Instant EXPIRY = ISSUED.plusSeconds(3600);
    private final CustomOAuth2UserService service = new CustomOAuth2UserService();

    @Test
    @DisplayName("trả về tên định danh và email khi dữ liệu OIDC đã xác minh email")
    void test_loadUser_001() {
        OidcUser user = service.loadUser(request("teacher@example.test", true));

        assertThat(user.getEmail()).isEqualTo("teacher@example.test");
        assertThat(user.getSubject()).isEqualTo("google-subject");
        assertThat(user.getEmailVerified()).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    @DisplayName(
            "trả về OAuth2AuthenticationException khi email chưa xác minh hoặc thiếu cờ xác minh")
    void test_loadUser_002(Boolean verified) {
        assertThatThrownBy(() -> service.loadUser(request("teacher@example.test", verified)))
                .isInstanceOfSatisfying(
                        OAuth2AuthenticationException.class,
                        error ->
                                assertThat(error.getError().getErrorCode())
                                        .isEqualTo("unverified_email"));
    }

    @Test
    @DisplayName("trả về OAuth2AuthenticationException khi claim email không tồn tại")
    void test_loadUser_003() {
        assertThatThrownBy(() -> service.loadUser(request(null, true)))
                .isInstanceOfSatisfying(
                        OAuth2AuthenticationException.class,
                        error ->
                                assertThat(error.getError().getErrorCode())
                                        .isEqualTo("unverified_email"));
    }

    private OidcUserRequest request(String email, Boolean verified) {
        ClientRegistration registration =
                ClientRegistration.withRegistrationId("google")
                        .clientId("test-client")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("https://wedu.example.test/auth/login/oauth2/code/google")
                        .scope("openid", "email")
                        .authorizationUri("https://provider.invalid/authorize")
                        .tokenUri("https://provider.invalid/token")
                        .build();
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "google-subject");
        if (email != null) claims.put("email", email);
        if (verified != null) claims.put("email_verified", verified);
        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER, "test-access", ISSUED, EXPIRY);
        OidcIdToken idToken = new OidcIdToken("test-id-token", ISSUED, EXPIRY, claims);
        return new OidcUserRequest(registration, accessToken, idToken);
    }
}
