package com.wedu.exam_creation.security.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtAuthenticationFilterTest {
    private final JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    private final CustomUserDetailsService users = mock(CustomUserDetailsService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokens, users);
    private final FilterChain chain = mock(FilterChain.class);

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
}
