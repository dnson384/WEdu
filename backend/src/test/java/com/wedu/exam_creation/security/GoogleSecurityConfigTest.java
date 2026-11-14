package com.wedu.exam_creation.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.auth.controller.VerificationController;
import com.wedu.exam_creation.auth.usecase.VerificationUsecase;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import jakarta.servlet.FilterChain;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = VerificationController.class,
        properties = "app.frontend.url=http://localhost:3000")
@Import({SecurityConfig.class, GoogleSecurityConfigTest.ClientConfig.class})
class GoogleSecurityConfigTest {
    @Autowired MockMvc mvc;
    @MockitoBean VerificationUsecase verification;
    @MockitoBean JwtAuthenticationFilter jwt;
    @MockitoBean CustomOAuth2UserService oidc;
    @MockitoBean OAuth2AuthenticationSuccessHandler success;

    @TestConfiguration
    static class ClientConfig {
        @Bean
        ClientRegistrationRepository clients(
                @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
                        String redirectUri) {
            return new InMemoryClientRegistrationRepository(
                    ClientRegistration.withRegistrationId("google")
                            .clientId("test-client")
                            .clientSecret("test-secret")
                            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                            .redirectUri(redirectUri)
                            .scope("openid", "profile", "email")
                            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                            .tokenUri("https://oauth2.googleapis.com/token")
                            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                            .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                            .userNameAttributeName("sub")
                            .clientName("Google")
                            .build());
        }
    }

    @BeforeEach
    void passGlobalMockFilter() throws Exception {
        doAnswer(
                        call -> {
                            ((FilterChain) call.getArgument(2))
                                    .doFilter(call.getArgument(0), call.getArgument(1));
                            return null;
                        })
                .when(jwt)
                .doFilter(any(), any(), any());
    }

    @Test
    void startCreatesStateNonceAndBrowserBindingWithoutJwt() throws Exception {
        var result =
                mvc.perform(get("/oauth2/authorization/google").param("binding", "a".repeat(64)))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();
        String location = result.getResponse().getRedirectedUrl();
        assertThat(location)
                .startsWith("https://accounts.google.com/")
                .contains("state=", "nonce=", "scope=openid");
        assertThat(URI.create(location).getQuery()).doesNotContain("binding=");
        assertThat(URI.create(location).getQuery())
                .contains("redirect_uri=http://localhost/auth/login/oauth2/code/google");
        var session = result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute("googleBinding")).isEqualTo("a".repeat(64));
        assertThat(session.getMaxInactiveInterval()).isEqualTo(600);
        verifyNoInteractions(verification, success, oidc);
    }

    @Test
    void callbackWithUnknownStateFailsWithoutIssuingJwt() throws Exception {
        mvc.perform(
                        get("/auth/login/oauth2/code/google")
                                .param("code", "not-a-provider-code")
                                .param("state", "wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "http://localhost:3000/api/auth/google/callback?error=google_failed"));
        verifyNoInteractions(verification, success, oidc);
    }

    @Test
    void providerCancellationInvalidatesTemporarySession() throws Exception {
        var start =
                mvc.perform(get("/oauth2/authorization/google").param("binding", "b".repeat(64)))
                        .andReturn();
        var session = (MockHttpSession) start.getRequest().getSession(false);
        mvc.perform(
                        get("/auth/login/oauth2/code/google")
                                .session(session)
                                .param("error", "access_denied")
                                .param("state", "invalid"))
                .andExpect(status().is3xxRedirection());
        assertThat(session.isInvalid()).isTrue();
        verifyNoInteractions(verification, success);
    }

    @Test
    void unrelatedEndpointRemainsProtected() throws Exception {
        mvc.perform(get("/auth/google/not-public")).andExpect(status().isUnauthorized());
    }

    @Test
    void formerCallbackAndAdjacentPathRemainProtected() throws Exception {
        mvc.perform(get("/login/oauth2/code/google")).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/login/oauth2/code/google/extra"))
                .andExpect(status().isUnauthorized());
    }
}
