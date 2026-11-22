package com.wedu.exam_creation.security.infrastructure.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.auth.usecase.VerificationUsecase;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class OAuth2AuthenticationSuccessHandlerTest {
    private static final String CALLBACK = "https://wedu.example.test/api/auth/google/callback";
    private static final String BINDING = "test-binding";
    private final VerificationUsecase verification = mock(VerificationUsecase.class);
    private final OAuth2AuthenticationSuccessHandler handler =
            new OAuth2AuthenticationSuccessHandler(verification, "https://wedu.example.test/");
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @ParameterizedTest
    @CsvSource({
        "test@GMAIL.COM,,true",
        "test@school.example,school.example,true",
        "test@example.test,,false"
    })
    @DisplayName(
            "trả về redirect kèm mã handoff và hủy session khi OIDC identity được xử lý thành công")
    void test_onAuthenticationSuccess_001(String email, String hostedDomain, boolean authoritative)
            throws Exception {
        MockHttpSession session = freshSession();
        Authentication auth = authentication(email, hostedDomain);
        when(verification.googleHandoff(email, "Teacher", "subject", true, authoritative, BINDING))
                .thenReturn("test-handoff");

        handler.onAuthenticationSuccess(request, response, auth);

        assertRedirect("?code=test-handoff");
        verify(verification)
                .googleHandoff(email, "Teacher", "subject", true, authoritative, BINDING);
        verify(session).invalidate();
    }

    @Test
    @DisplayName("trả về redirect lỗi khi callback không có session tạm")
    void test_onAuthenticationSuccess_002() throws Exception {
        handler.onAuthenticationSuccess(request, response, mock(Authentication.class));

        assertRedirect("?error=google_failed");
        verifyNoInteractions(verification);
    }

    @Test
    @DisplayName("trả về redirect lỗi và hủy session khi phiên OAuth đã hết hạn")
    void test_onAuthenticationSuccess_003() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("googleStarted", Instant.parse("2000-01-01T00:00:00Z").toEpochMilli());
        request.setSession(session);

        handler.onAuthenticationSuccess(request, response, mock(Authentication.class));

        assertRedirect("?error=google_failed");
        assertThat(session.isInvalid()).isTrue();
        verifyNoInteractions(verification);
    }

    @Test
    @DisplayName("trả về redirect lỗi chung và hủy session khi tạo handoff thất bại")
    void test_onAuthenticationSuccess_004() throws Exception {
        MockHttpSession session = freshSession();
        Authentication auth = authentication("test@gmail.com", null);
        when(verification.googleHandoff(
                        "test@gmail.com", "Teacher", "subject", true, true, BINDING))
                .thenThrow(new IllegalStateException("test handoff failure"));

        handler.onAuthenticationSuccess(request, response, auth);

        assertRedirect("?error=google_failed");
        verify(session).invalidate();
    }

    private MockHttpSession freshSession() {
        MockHttpSession session = mock(MockHttpSession.class);
        when(session.getAttribute("googleBinding")).thenReturn(BINDING);
        when(session.getAttribute("googleStarted")).thenAnswer(call -> System.currentTimeMillis());
        request.setSession(session);
        return session;
    }

    private Authentication authentication(String email, String hostedDomain) {
        Authentication auth = mock(Authentication.class);
        OidcUser principal = mock(OidcUser.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(principal.getEmail()).thenReturn(email);
        when(principal.getFullName()).thenReturn("Teacher");
        when(principal.getSubject()).thenReturn("subject");
        when(principal.getEmailVerified()).thenReturn(true);
        when(principal.getClaimAsString("hd")).thenReturn(hostedDomain);
        return auth;
    }

    private void assertRedirect(String query) {
        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo(CALLBACK + query);
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(response.getCookies()).isEmpty();
    }
}
