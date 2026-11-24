package com.wedu.exam_creation.security.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tools.jackson.databind.ObjectMapper;

class JwtAuthenticationFilterTest {
    private final JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    private final CustomUserDetailsService users = mock(CustomUserDetailsService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokens, users);
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

    @Test
    @DisplayName("trả về cho qua callback Google khi không có JWT ứng dụng")
    void test_shouldNotFilter_001() throws Exception {
        MockHttpServletRequest request = request("/auth/login/oauth2/code/google");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(tokens, users);
    }

    @Test
    @DisplayName("trả về 400 và không chạy filter chain khi callback cũ không có JWT")
    void test_shouldNotFilter_002() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/login/oauth2/code/google"), response, chain);
        assertThat(response.getStatus()).isEqualTo(400);
        verifyNoInteractions(chain);
    }

    @Test
    @DisplayName("trả về 400 và không chạy filter chain khi path liền kề callback không có JWT")
    void test_shouldNotFilter_003() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/auth/login/oauth2/code/google/extra"), response, chain);
        assertThat(response.getStatus()).isEqualTo(400);
        verifyNoInteractions(chain);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ và gắn quyền từ tài khoản hiện tại khi JWT hợp lệ")
    void test_doFilterInternal_004() throws Exception {
        MockHttpServletRequest request = protectedRequest("Bearer test-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserDetails user =
                User.withUsername("teacher@example.test")
                        .password("unused")
                        .roles("TEACHER")
                        .build();
        givenPayload();
        when(users.loadUserByUsername("teacher@example.test")).thenReturn(user);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isSameAs(user);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_TEACHER");
        verify(tokens).validateAccessToken("test-token");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("trả về HTTP 400 khi header Authorization không đúng dạng Bearer")
    void test_doFilterInternal_005() throws Exception {
        assertRejected("Basic example", 400, "Header Authorization sai định dạng");
        verifyNoInteractions(tokens, users);
    }

    @Test
    @DisplayName("trả về HTTP 401 khi Bearer token để trống")
    void test_doFilterInternal_006() throws Exception {
        assertRejected("Bearer   ", 401, "Không có AT");
        verifyNoInteractions(tokens, users);
    }

    @Test
    @DisplayName("trả về HTTP 401 khi provider từ chối access token")
    void test_doFilterInternal_007() throws Exception {
        doThrow(new UnAuthorizedException("Token không hợp lệ"))
                .when(tokens)
                .validateAccessToken("test-token");

        assertRejected("Bearer test-token", 401, "Token không hợp lệ");
        verify(tokens, never()).getPayloadFromAccessToken(anyString());
        verifyNoInteractions(users);
    }

    @Test
    @DisplayName("trả về HTTP 403 khi tài khoản đã bị khóa")
    void test_doFilterInternal_008() throws Exception {
        givenPayload();
        when(users.loadUserByUsername("teacher@example.test"))
                .thenReturn(
                        User.withUsername("teacher")
                                .password("unused")
                                .roles("TEACHER")
                                .disabled(true)
                                .build());

        assertRejected("Bearer test-token", 403, "Tài khoản đã bị khóa");
    }

    @Test
    @DisplayName("trả về HTTP 404 khi không tìm thấy tài khoản của JWT")
    void test_doFilterInternal_009() throws Exception {
        givenPayload();
        when(users.loadUserByUsername("teacher@example.test"))
                .thenThrow(new UsernameNotFoundException("Tài khoản chưa tồn tại"));

        assertRejected("Bearer test-token", 404, "Tài khoản chưa tồn tại");
    }

    @Test
    @DisplayName("trả về HTTP 500 với thông báo chung khi đọc tài khoản gặp lỗi bất ngờ")
    void test_doFilterInternal_010() throws Exception {
        givenPayload();
        when(users.loadUserByUsername("teacher@example.test"))
                .thenThrow(new IllegalStateException("test storage failure"));

        assertRejected("Bearer test-token", 500, "Lỗi xác thực");
    }

    private MockHttpServletRequest protectedRequest(String authorization) {
        MockHttpServletRequest request = request("/user/me");
        request.addHeader("Authorization", authorization);
        return request;
    }

    private void givenPayload() {
        when(tokens.getPayloadFromAccessToken("test-token"))
                .thenReturn(new ATPayload("jti", "user", "teacher@example.test", "ROLE_ADMIN"));
    }

    private void assertRejected(String header, int status, String message) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(protectedRequest(header), response, chain);

        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(
                        new ObjectMapper()
                                .readTree(response.getContentAsString())
                                .get("message")
                                .asString())
                .isEqualTo(message);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(chain);
    }
}
