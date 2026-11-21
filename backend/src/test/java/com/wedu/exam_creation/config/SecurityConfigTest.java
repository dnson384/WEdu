package com.wedu.exam_creation.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.auth.controller.VerificationController;
import com.wedu.exam_creation.auth.usecase.VerificationUsecase;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

@WebMvcTest(
        value = VerificationController.class,
        properties = "app.frontend.url=http://localhost:3000")
@Import({SecurityConfig.class, SecurityConfigTest.ClientConfig.class})
class SecurityConfigTest {
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
    @DisplayName("trả về redirect có state nonce và binding trong session khi bắt đầu Google")
    void test_googleSecurityFilterChain_001() throws Exception {
        MvcResult result =
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
        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute("googleBinding")).isEqualTo("a".repeat(64));
        assertThat(session.getMaxInactiveInterval()).isEqualTo(600);
        verifyNoInteractions(verification, success, oidc);
    }

    @Test
    @DisplayName("trả về redirect lỗi không cấp JWT khi callback có state không hợp lệ")
    void test_googleSecurityFilterChain_002() throws Exception {
        mvc.perform(
                        get("/auth/login/oauth2/code/google")
                                .param("code", "not-a-provider-code")
                                .param("state", "wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(
                        redirectedUrl(
                                "http://localhost:3000/api/auth/google/callback?error=google_failed"));
        verifyNoInteractions(verification, success, oidc);
    }

    @Test
    @DisplayName("trả về session tạm bị hủy khi người dùng hủy Google")
    void test_googleSecurityFilterChain_003() throws Exception {
        MvcResult start =
                mvc.perform(get("/oauth2/authorization/google").param("binding", "b".repeat(64)))
                        .andReturn();
        MockHttpSession session = (MockHttpSession) start.getRequest().getSession(false);
        String state =
                UriComponentsBuilder.fromUriString(start.getResponse().getRedirectedUrl())
                        .build()
                        .getQueryParams()
                        .getFirst("state");
        mvc.perform(
                        get("/auth/login/oauth2/code/google")
                                .session(session)
                                .param("error", "access_denied")
                                .param("state", UriUtils.decode(state, StandardCharsets.UTF_8)))
                .andExpect(
                        redirectedUrl(
                                "http://localhost:3000/api/auth/google/callback?error=google_failed"));
        assertThat(session.isInvalid()).isTrue();
        verifyNoInteractions(verification, success, oidc);
    }

    @Test
    @DisplayName("trả về 401 khi truy cập endpoint Google không public")
    void test_securityFilterChain_004() throws Exception {
        MvcResult result =
                mvc.perform(get("/auth/google/not-public"))
                        .andExpect(status().isUnauthorized())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.message").value("Chưa xác thực"))
                        .andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("trả về 401 khi truy cập callback cũ hoặc path liền kề callback")
    void test_securityFilterChain_005() throws Exception {
        mvc.perform(get("/login/oauth2/code/google")).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/login/oauth2/code/google/extra"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {"abc", "gggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg"})
    @DisplayName("trả về lỗi và không tạo session OAuth khi binding thiếu hoặc sai định dạng")
    void test_googleSecurityFilterChain_006(String binding) throws Exception {
        MockHttpServletRequestBuilder request = get("/oauth2/authorization/google");
        if (binding != null) {
            request.param("binding", binding);
        }

        MvcResult result =
                mvc.perform(request)
                        .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                        .andReturn();

        assertThat(result.getResponse().getStatus()).isBetween(400, 599);
        assertThat(result.getRequest().getSession(false)).isNull();
        verifyNoInteractions(verification, success, oidc);
    }

    @Test
    @DisplayName("trả về HTTP 400 từ MVC khi gửi JSON lỗi tới endpoint Google exchange công khai")
    void test_securityFilterChain_007() throws Exception {
        mvc.perform(
                        post("/auth/google/exchange")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dữ liệu gửi lên không hợp lệ"));
        verifyNoInteractions(verification);
    }
}
