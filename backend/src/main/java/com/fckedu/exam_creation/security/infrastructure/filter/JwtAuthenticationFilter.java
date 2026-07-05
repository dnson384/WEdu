package com.fckedu.exam_creation.security.infrastructure.filter;

import com.fckedu.exam_creation.common.dto.token.ATPayload;
import com.fckedu.exam_creation.common.exception.UnAuthorizedException;
import com.fckedu.exam_creation.security.dto.CookieDataDTO;
import com.fckedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.fckedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;

@Slf4j
@NullMarked
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final List<String> EXCLUDED_PATHS = List.of(
            // User
            "/user/login",
            "/user/register",
            "/user/logout",
            // RT
            "/refresh-token/generate-access-token",
            // Static
            "/static/**",
            // Swagger
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml"
    );
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String currentPath = request.getServletPath();

        return EXCLUDED_PATHS.stream()
                .anyMatch(path -> pathMatcher.match(path, currentPath));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException, java.io.IOException {
        try {
            CookieDataDTO jwt = getJwtFromRequest(request);

            if (jwt.getRefreshToken() == null || jwt.getRefreshToken().trim().isEmpty()) {
                throw new UnAuthorizedException("Không có RT");
            } else {
                tokenProvider.validateRefreshToken(jwt.getRefreshToken());
            }

            // Validate token
            if (jwt.getAccessToken() != null && tokenProvider.validateAccessToken(jwt.getAccessToken())) {

                // Giải mã lấy ATPayload record bạn đã tạo
                ATPayload payload = tokenProvider.getPayloadFromAccessToken(jwt.getAccessToken());

                // Load thông tin chi tiết của User (Sử dụng email làm định danh đăng nhập)
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(payload.getEmail());

                if (!userDetails.isEnabled()) {
                    throw new UnavailableException("Tài khoản đã bị khóa!");
                }

                // Cấp quyền và nhét vào SecurityContext
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Không thể xác thực người dùng trong Security Context", ex);
        }

        filterChain.doFilter(request, response);
    }

    // Hàm bổ trợ quét Cookie tìm "access_token"
    private CookieDataDTO getJwtFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        CookieDataDTO res = new CookieDataDTO();
        for (Cookie cookie : cookies) {
            if ("accessToken".equals(cookie.getName())) {
                res.setAccessToken(cookie.getValue());
            } else if ("refreshToken".equals(cookie.getName())) {
                res.setRefreshToken(cookie.getValue());
            }
        }

        return res;
    }
}