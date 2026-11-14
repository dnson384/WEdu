package com.wedu.exam_creation.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtOAuthCallbackFilterTest {
    private final JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    private final CustomUserDetailsService users = mock(CustomUserDetailsService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokens, users);
    private final FilterChain chain = mock(FilterChain.class);

    private MockHttpServletRequest request(String path) {
        var request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

    @Test
    void googleCallbackPassesWithoutApplicationJwt() throws Exception {
        var request = request("/auth/login/oauth2/code/google");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(tokens, users);
    }

    @Test
    void formerCallbackDoesNotBypassJwt() throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(request("/login/oauth2/code/google"), response, chain);
        assertThat(response.getStatus()).isEqualTo(400);
        verifyNoInteractions(chain);
    }

    @Test
    void callbackAdjacentPathDoesNotBypassJwt() throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(request("/auth/login/oauth2/code/google/extra"), response, chain);
        assertThat(response.getStatus()).isEqualTo(400);
        verifyNoInteractions(chain);
    }
}
